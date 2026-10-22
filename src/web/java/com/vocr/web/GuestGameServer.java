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
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

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
    private final Map<String, GameSession> sessions = new HashMap<>();
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
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
        if (maxSessions <= 0) throw new IllegalArgumentException("会话上限必须为正数");
        this.maxSessions = maxSessions;
        app = Javalin.create(config -> {
            config.jetty.host = "127.0.0.1";
            config.http.maxRequestSize = 16_384L;
            config.routes.before(ctx -> ctx.header("Cache-Control", "no-store"));
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
                send(ctx, session(ctx.pathParam("id")).currentUpdate(value));
            });
            config.routes.put("/api/v1/games/{id}/vote-target", ctx -> {
                VoteTarget request = read(ctx, VoteTarget.class);
                send(ctx, session(ctx.pathParam("id")).setTargetSelection(
                        new SetAllTargetSelectionCommand(request.expectedRevision(),
                                SelectionType.VOTE, request.target(), request.selected())));
            });
            config.routes.post("/api/v1/games/{id}/votes", ctx -> {
                ExecuteVoteCommand request = read(ctx, ExecuteVoteCommand.class);
                send(ctx, session(ctx.pathParam("id")).executeVote(request));
            });
            config.routes.post("/api/v1/games/{id}/claims/by-role", ctx -> {
                AskClaimByRoleCommand request = read(ctx, AskClaimByRoleCommand.class);
                send(ctx, session(ctx.pathParam("id")).requestClaims(request));
            });
            config.routes.post("/api/v1/games/{id}/claims/by-players", ctx -> {
                AskClaimByPlayersCommand request = read(ctx, AskClaimByPlayersCommand.class);
                send(ctx, session(ctx.pathParam("id")).requestClaims(request));
            });
            config.routes.put("/api/v1/games/{id}/avoidance", ctx -> {
                SetAvoidanceCommand request = read(ctx, SetAvoidanceCommand.class);
                send(ctx, session(ctx.pathParam("id")).setAvoidance(request));
            });
            config.routes.put("/api/v1/games/{id}/target-selections", ctx -> {
                SetAllTargetSelectionCommand request = read(ctx, SetAllTargetSelectionCommand.class);
                if (request.type() != SelectionType.DIVINATION)
                    throw new IllegalArgumentException("当前仅开放全体占卜目标预约");
                send(ctx, session(ctx.pathParam("id")).setTargetSelection(request));
            });
            config.routes.delete("/api/v1/games/{id}", ctx -> {
                remove(ctx.pathParam("id"));
                ctx.status(204);
            });
            config.routes.exception(StaleGameRevisionException.class, (error, ctx) -> {
                ctx.status(409);
                send(ctx, new ApiError("STALE_REVISION", error.getMessage(),
                        error.currentRevision()));
            });
            config.routes.exception(IllegalArgumentException.class,
                    (error, ctx) -> error(ctx, 400, "INVALID_REQUEST", error.getMessage()));
            config.routes.exception(IllegalStateException.class,
                    (error, ctx) -> error(ctx, 409, "INVALID_STATE", error.getMessage()));
            config.routes.exception(ApiException.class,
                    (error, ctx) -> error(ctx, error.status, error.code, error.getMessage()));
            config.routes.exception(HttpResponseException.class,
                    (error, ctx) -> error(ctx, error.getStatus(), "HTTP_ERROR", error.getMessage()));
            config.routes.exception(Exception.class, (error, ctx) -> {
                DebugLogger.error("[HTTP] 请求处理失败", error);
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
        synchronized (this) {
            if (closed) throw new IllegalStateException("游客服务已经关闭");
        }
        app.start(port);
        return this;
    }

    public int port() { return app.port(); }

    private GameSession create(peiyi configuration)
    {
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
                sessions.put(id, session);
                return session;
            }
        }
        session.close();
        throw new IllegalStateException("游客服务已经关闭");
    }

    private synchronized GameSession session(String id)
    {
        GameSession session = sessions.get(id);
        if (session == null) throw new ApiException(404, "GAME_NOT_FOUND", "游客对局不存在");
        return session;
    }

    private void remove(String id)
    {
        GameSession session;
        synchronized (this) {
            session = session(id);
            sessions.remove(id);
        }
        session.close();
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
            DebugLogger.error("[HTTP] 响应序列化失败", error);
            ctx.status(500).contentType("application/json")
                    .result("{\"code\":\"INTERNAL_ERROR\",\"message\":\"响应序列化失败\"}");
        }
    }

    private void error(Context ctx, int status, String code, String message)
    {
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
            toClose = List.copyOf(sessions.values());
            sessions.clear();
        }
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
            DebugLogger.error(error.getMessage() + "；用法：runWeb --args=\"7070\"（可省略端口）");
            return 2;
        }
        GuestGameServer server = new GuestGameServer();
        try {
            server.start(port);
        } catch (RuntimeException error) {
            try { server.close(); }
            catch (RuntimeException ignored) { DebugLogger.warn("启动失败后的服务清理未完成"); }
            DebugLogger.error("无法启动游客服务，请检查端口 " + port + " 是否被占用及监听权限");
            return 1;
        }
        DebugLogger.info("[HTTP] 游客 API: http://127.0.0.1:" + server.port() + "/api/v1/games");
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
