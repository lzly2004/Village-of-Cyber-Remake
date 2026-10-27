package com.vocr.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.*;
import com.vocr.core.MainLogic;
import com.vocr.domain.SelectionType;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;
import com.vocr.logging.DebugLogger;
import com.vocr.logging.WebDiagnostics;
import static com.vocr.logging.WebDiagnostics.Event.*;
import com.vocr.replay.ReplaySave;
import java.nio.charset.StandardCharsets;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本机游客 API 原型。每个会话独占规则、事件与内存战绩，只有应用快照可以被序列化。
 * 会话编号是临时访问凭据；此原型不提供公网认证或持久化恢复。
 */
public final class GuestGameServer implements AutoCloseable
{
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    private final Map<String, Entry> sessions = new HashMap<>();
    private final LongSupplier clock;
    private final long idleNanos;
    private final long lifetimeNanos;
    private final GuestRequestLimits requestLimits;
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "vocr-guest-reaper");
        thread.setDaemon(true);
        return thread;
    });
    private boolean started;

    private final class Entry
    {
        final GameSession session;
        final long created;
        long lastAccess;
        int users;
        boolean retired;
        final GuestRequestLimits.Bucket requests = new GuestRequestLimits.Bucket(clock,
                requestLimits.policy.sessionPerSecond(), requestLimits.policy.sessionBurst(), Duration.ofSeconds(1));
        final GuestRequestLimits.Bucket replays = new GuestRequestLimits.Bucket(clock,
                requestLimits.policy.replaysPerMinute(), requestLimits.policy.replayBurst(), Duration.ofMinutes(1));
        Entry(GameSession session, long now) { this.session = session; created = lastAccess = now; }
    }
    private final Function<String, GameSession> sessionFactory;
    private final int maxSessions;
    private final Javalin app;
    private int pendingCreations;
    private boolean closed;

    public GuestGameServer()
    {
        this(GuestGameServer::newGuestSession, 100);
    }

    // 测试可注入确定性对局；HTTP 请求不开放种子或核心状态。
    GuestGameServer(Function<String, GameSession> sessionFactory, int maxSessions)
    {
        this(sessionFactory, maxSessions, System::nanoTime, Duration.ofMinutes(30), Duration.ofHours(24));
    }

    GuestGameServer(Function<String, GameSession> sessionFactory, int maxSessions,
                    LongSupplier clock, Duration idle, Duration lifetime)
    {
        this(sessionFactory, maxSessions, clock, idle, lifetime, GuestRequestLimits.Policy.production());
    }

    GuestGameServer(Function<String, GameSession> sessionFactory, int maxSessions,
                    LongSupplier clock, Duration idle, Duration lifetime, GuestRequestLimits.Policy limits)
    {
        this.clock = Objects.requireNonNull(clock);
        idleNanos = Objects.requireNonNull(idle).toNanos();
        lifetimeNanos = Objects.requireNonNull(lifetime).toNanos();
        if (idleNanos <= 0 || lifetimeNanos <= 0) throw new IllegalArgumentException("会话时限必须为正数");
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
        if (maxSessions <= 0) throw new IllegalArgumentException("会话上限必须为正数");
        this.maxSessions = maxSessions;
        requestLimits = new GuestRequestLimits(Objects.requireNonNull(limits), clock);
        app = Javalin.create(config -> {
            config.jetty.host = "127.0.0.1";
            config.http.maxRequestSize = 16_384L;
            // Bind costly admission to registered endpoints, not the untrusted raw URI.
            // Router aliases (trailing slashes/decoding) cannot bypass creation or Replay limits.
            config.router.handlerWrapper(endpoint -> {
                boolean create = endpoint.method.name().equals("POST") && endpoint.path.equals("/api/v1/games");
                boolean replay = endpoint.method.name().equals("GET") && endpoint.path.equals("/api/v1/games/{id}/replay");
                if (!create && !replay) return endpoint.handler;
                return ctx -> {
                    GuestRequestLimits.Lease lease = Objects.requireNonNull(ctx.attribute("vocr.requestLease"));
                    lease.restrictRoute(create, replay);
                    endpoint.handler.handle(ctx);
                };
            });
            config.routes.before(ctx -> {
                ctx.header("Cache-Control", "no-store");
                ctx.header("Referrer-Policy", "no-referrer");
                // Before body parsing, session acquisition or game commands. after also runs on exceptions.
                ctx.attribute("vocr.requestLease", requestLimits.enter(false, false));
            });
            config.routes.after(ctx -> {
                GuestRequestLimits.Lease lease = ctx.attribute("vocr.requestLease");
                if (lease != null) lease.close();
            });
            config.routes.post("/api/v1/games", ctx -> {
                CreateGame request = read(ctx, CreateGame.class);
                if (request.configuration() == null || request.configuration() == peiyi.NONE)
                    throw new IllegalArgumentException("必须选择有效配役");
                GameSession session = create(request.configuration());
                ctx.status(201).header("Location", "/api/v1/games/" + session.sessionId());
                send(ctx, new CreatedGame(session.sessionId(), session.currentUpdate(0)));
            });
            config.routes.get("/api/v1/games/{id}", ctx -> {
                String cursor = ctx.queryParam("eventCursor");
                long value = cursor == null ? 0 : Long.parseLong(cursor);
                send(ctx, withSession(ctx.pathParam("id"), session -> session.currentUpdate(value)));
            });
            config.routes.get("/api/v1/games/{id}/replay", ctx -> {
                String cursor = ctx.queryParam("eventCursor");
                if (cursor == null || !cursor.matches("0|[1-9][0-9]{0,15}")
                        || ctx.queryParams("eventCursor").size() != 1)
                    throw new IllegalArgumentException("录像读取必须提供有效确认游标");
                ReplayRecording recording = withSession(ctx.pathParam("id"), true,
                        session -> session.completedReplay(Long.parseLong(cursor)));
                // Immutable application snapshot: parsing/serialization is outside the session monitor.
                if (recording.records().size() > 20_000
                        || recording.records().stream().anyMatch(line -> line.getBytes(StandardCharsets.UTF_8).length > 65_536)
                        || recording.records().stream().mapToLong(line -> line.getBytes(StandardCharsets.UTF_8).length).sum() > 8 * 1024 * 1024)
                    throw new ApiException(413, "REPLAY_LIMIT", "录像超出保存上限，未写入存储");
                ReplaySave replay = ReplaySave.fromRecords(0, recording.records());
                if (replay.endResult < 1 || replay.endResult > 3 || replay.totalDays < 1
                        || replay.totalDays > 50 || replay.daySnapshots.isEmpty() || replay.daySnapshots.size() > 50)
                    throw new ApiException(409, "REPLAY_UNAVAILABLE", "终局录像不完整，未写入存储");
                ReplaySave[] slots = new ReplaySave[9]; slots[0] = replay;
                byte[] document = json.writeValueAsBytes(slots);
                if (document.length > 8 * 1024 * 1024)
                    throw new ApiException(413, "REPLAY_LIMIT", "录像超出保存上限，未写入存储");
                ctx.contentType("application/json; charset=utf-8").result(document);
            });
            config.routes.put("/api/v1/games/{id}/vote-target", ctx -> {
                VoteTarget request = read(ctx, VoteTarget.class);
                send(ctx, withSession(ctx.pathParam("id"), session -> session.setTargetSelection(
                        new SetAllTargetSelectionCommand(request.expectedRevision(),
                                SelectionType.VOTE, request.target(), request.selected()))));
            });
            config.routes.post("/api/v1/games/{id}/votes", ctx -> {
                ExecuteVoteCommand request = read(ctx, ExecuteVoteCommand.class);
                send(ctx, withSession(ctx.pathParam("id"), session -> session.executeVote(request)));
            });
            config.routes.post("/api/v1/games/{id}/claims/by-role", ctx -> {
                AskClaimByRoleCommand request = read(ctx, AskClaimByRoleCommand.class);
                send(ctx, withSession(ctx.pathParam("id"), session -> session.requestClaims(request)));
            });
            config.routes.post("/api/v1/games/{id}/claims/by-players", ctx -> {
                AskClaimByPlayersCommand request = read(ctx, AskClaimByPlayersCommand.class);
                send(ctx, withSession(ctx.pathParam("id"), session -> session.requestClaims(request)));
            });
            config.routes.put("/api/v1/games/{id}/avoidance", ctx -> {
                SetAvoidanceCommand request = read(ctx, SetAvoidanceCommand.class);
                send(ctx, withSession(ctx.pathParam("id"), session -> session.setAvoidance(request)));
            });
            config.routes.put("/api/v1/games/{id}/target-selections", ctx -> {
                SetAllTargetSelectionCommand request = read(ctx, SetAllTargetSelectionCommand.class);
                if (request.type() != SelectionType.DIVINATION && request.type() != SelectionType.GUARD)
                    throw new IllegalArgumentException("此路径仅支持全体占卜或护卫目标预约");
                send(ctx, withSession(ctx.pathParam("id"), session -> session.setTargetSelection(request)));
            });
            config.routes.put("/api/v1/games/{id}/claimed-target-selections", ctx -> {
                SetClaimedRoleTargetSelectionCommand request = read(ctx, SetClaimedRoleTargetSelectionCommand.class);
                if (request.type() != SelectionType.DIVINATION && request.type() != SelectionType.GUARD)
                    throw new IllegalArgumentException("此路径仅支持公开占卜或护卫行动者预约");
                send(ctx, withSession(ctx.pathParam("id"), session -> session.setTargetSelection(request)));
            });
            config.routes.delete("/api/v1/games/{id}", ctx -> {
                remove(ctx.pathParam("id"));
                ctx.status(204);
            });
            config.routes.exception(StaleGameRevisionException.class, (error, ctx) -> {
                ctx.status(409);
                send(ctx, new ApiError("STALE_REVISION", "对局版本已更新，请重新同步",
                        error.currentRevision()));
            });
            config.routes.exception(IllegalArgumentException.class,
                    (error, ctx) -> error(ctx, 400, "INVALID_REQUEST", "请求参数无效，请核对后重试"));
            config.routes.exception(IllegalStateException.class,
                    (error, ctx) -> error(ctx, 409, "INVALID_STATE", "当前对局状态不允许该操作，请重新同步"));
            config.routes.exception(GuestRequestLimits.Rejected.class, (error, ctx) -> {
                ctx.header("Retry-After", Integer.toString(error.retrySeconds));
                error(ctx, 429, "REQUEST_LIMIT", "请求过于频繁或正在处理其他请求，本次操作未执行");
            });
            config.routes.exception(ApiException.class,
                    (error, ctx) -> error(ctx, error.status, error.code, error.getMessage()));
            config.routes.exception(HttpResponseException.class,
                    (error, ctx) -> error(ctx, error.getStatus(), "HTTP_ERROR", "HTTP 请求被拒绝"));
            config.routes.error(404, ctx -> {
                if (!Boolean.TRUE.equals(ctx.attribute("vocr.apiError")))
                    error(ctx, 404, "HTTP_ERROR", "请求路径不存在");
            });
            config.routes.exception(Exception.class, (error, ctx) -> {
                WebDiagnostics.write(REQUEST_FAILED);
                error(ctx, 500, "INTERNAL_ERROR", "请求处理失败");
            });
        });
    }

    static GameSession newGuestSession(String id)
    {
        GameEventBuffer events = new GameEventBuffer();
        GameRecordManager records = GameRecordManager.inMemory();
        return new GameSession(id, new MainLogic(events::publish, records), events, records);
    }

    public GuestGameServer start(int port)
    {
        // A standalone Web process must not emit the desktop core's private game diagnostics.
        // The desktop default and Replay recorder are unchanged. Web operational logging is separate.
        DebugLogger.getInstance().setEnabled(false);
        synchronized (this) {
            if (closed) throw new IllegalStateException("游客服务已经关闭");
            if (started) throw new IllegalStateException("游客服务已经启动");
            app.start(port);
            started = true;
            reaper.scheduleWithFixedDelay(() -> {
                try { reapExpired(); }
                catch (RuntimeException error) { WebDiagnostics.write(REAP_FAILED); }
            }, 1, 1, TimeUnit.MINUTES);
        }
        return this;
    }

    public int port() { return app.port(); }

    private GameSession create(peiyi configuration)
    {
        reapExpired();
        synchronized (this) {
            if (closed) throw new IllegalStateException("游客服务已经关闭");
            if (sessions.size() + pendingCreations >= maxSessions)
                throw new ApiException(503, "SESSION_LIMIT", "游客会话数量已达上限，请释放旧会话");
            pendingCreations++;
        }
        String id;
        GameSession session = null;
        try {
            id = UUID.randomUUID().toString();
            session = sessionFactory.apply(id);
            session.startGame(new StartGameCommand(0, configuration));
        } catch (RuntimeException | Error error) {
            synchronized (this) { pendingCreations--; }
            if (session != null) session.close();
            throw error;
        }
        synchronized (this) {
            pendingCreations--;
            if (!closed) {
                sessions.put(id, new Entry(session, clock.getAsLong()));
                return session;
            }
        }
        session.close();
        throw new IllegalStateException("游客服务已经关闭");
    }

    private boolean expired(Entry entry, long now)
    {
        // Monotonic differences tolerate nanoTime wraparound; never use wall-clock timestamps.
        return now - entry.lastAccess >= idleNanos || now - entry.created >= lifetimeNanos;
    }

    private static ApiException unavailable()
    {
        // Same bounded response for unknown, malformed, expired, released or restarted credentials.
        return new ApiException(404, "GAME_NOT_FOUND", "游客对局已失效或不存在，请重新开局");
    }

    private static boolean credential(String id)
    {
        return id != null && id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    <T> T withSession(String id, Function<GameSession, T> action)
    {
        return withSession(id, false, action);
    }

    private <T> T withSession(String id, boolean replay, Function<GameSession, T> action)
    {
        reapExpired();
        Entry entry;
        synchronized (this) {
            entry = credential(id) ? sessions.get(id) : null;
            if (closed || entry == null || entry.retired || expired(entry, clock.getAsLong()))
                throw unavailable();
            if (entry.users >= requestLimits.policy.sessionUsers()) throw new GuestRequestLimits.Rejected(1);
            entry.requests.take();
            if (replay) entry.replays.take();
            entry.users++;
        }
        try {
            return action.apply(entry.session);
        } finally {
            boolean release;
            synchronized (this) {
                entry.users--;
                long now = clock.getAsLong();
                // Idle is measured since the last request completion; absolute life never extends.
                entry.lastAccess = now;
                release = entry.users == 0 && (entry.retired || now - entry.created >= lifetimeNanos);
                if (release) sessions.remove(id, entry);
            }
            if (release) entry.session.close();
        }
    }

    void reapExpired()
    {
        List<GameSession> toClose = new ArrayList<>();
        synchronized (this) {
            long now = clock.getAsLong();
            var iterator = sessions.values().iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                if (entry.retired || expired(entry, now)) {
                    if (entry.users != 0) continue; // Never close a command in flight.
                    iterator.remove();
                    toClose.add(entry.session);
                }
            }
        }
        // No core/session monitor is acquired while holding the registry monitor.
        toClose.forEach(GameSession::close);
    }

    private void remove(String id)
    {
        reapExpired();
        GameSession toClose = null;
        synchronized (this) {
            Entry entry = credential(id) ? sessions.get(id) : null;
            if (closed || entry == null || entry.retired || expired(entry, clock.getAsLong()))
                throw unavailable();
            entry.retired = true; // Stop new leases; accepted requests may finish, never replayed.
            if (entry.users == 0) {
                sessions.remove(id);
                toClose = entry.session;
            }
        }
        if (toClose != null) toClose.close();
    }

    private <T> T read(Context ctx, Class<T> type)
    {
        if (ctx.contentType() == null
                || !ctx.contentType().split(";")[0].trim().equalsIgnoreCase("application/json"))
            throw new ApiException(415, "JSON_REQUIRED", "请求必须使用 application/json");
        try {
            T value = json.readValue(ctx.body(), type);
            if (value == null) throw new IllegalArgumentException("请求体不能为空");
            return value;
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("JSON 请求无效或字段缺失");
        }
    }

    private void send(Context ctx, Object value)
    {
        try {
            ctx.contentType("application/json; charset=utf-8").result(json.writeValueAsString(value));
        } catch (JsonProcessingException error) {
            WebDiagnostics.write(SERIALIZATION_FAILED);
            ctx.status(500).contentType("application/json")
                    .result("{\"code\":\"INTERNAL_ERROR\",\"message\":\"响应序列化失败\"}");
        }
    }

    private void error(Context ctx, int status, String code, String message)
    {
        ctx.attribute("vocr.apiError", true);
        ctx.status(status);
        send(ctx, new ApiError(code, message, null));
    }

    @Override
    public void close()
    {
        List<GameSession> toClose;
        synchronized (this) {
            if (closed) return;
            closed = true;
            toClose = sessions.values().stream().map(entry -> entry.session).toList();
            sessions.clear();
        }
        reaper.shutdownNow();
        try {
            toClose.forEach(GameSession::close);
        } finally {
            app.stop();
        }
    }

    public static void main(String[] args)
    {
        int status = run(args);
        if (status != 0) System.exit(status);
    }

    static int parsePort(String[] args)
    {
        if (args.length == 0) return 7070;
        if (args.length != 1 || !args[0].matches("[0-9]+"))
            throw new IllegalArgumentException("只接受一个十进制端口参数");
        try {
            int port = Integer.parseInt(args[0]);
            if (port >= 1 && port <= 65535) return port;
        } catch (NumberFormatException ignored) {
            // Fall through to the same bounded, user-facing diagnostic.
        }
        throw new IllegalArgumentException("端口必须在 1—65535 之间");
    }

    static int run(String[] args)
    {
        int port;
        try {
            port = parsePort(args);
        } catch (IllegalArgumentException error) {
            WebDiagnostics.write(INVALID_PORT);
            return 2;
        }
        GuestGameServer server = new GuestGameServer();
        try {
            server.start(port);
        } catch (RuntimeException error) {
            try { server.close(); }
            catch (RuntimeException ignored) { WebDiagnostics.write(START_CLEANUP_FAILED); }
            WebDiagnostics.write(START_FAILED);
            return 1;
        }
        WebDiagnostics.listening(server.port());
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        return 0;
    }

    public record CreateGame(peiyi configuration) { }
    public record VoteTarget(long expectedRevision, int target, boolean selected) { }
    public record CreatedGame(String sessionId, GameUpdate update) { }
    public record ApiError(String code, String message, Long currentRevision) { }

    private static final class ApiException extends RuntimeException
    {
        private final int status;
        private final String code;

        ApiException(int status, String code, String message)
        {
            super(message);
            this.status = status;
            this.code = code;
        }
    }
}
