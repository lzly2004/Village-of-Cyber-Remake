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
                if (request.method() != VotingMethod.DESIGNATED)
                    throw new IllegalArgumentException("此切片只支持指定投票");
                send(ctx, session(ctx.pathParam("id")).executeVote(request));
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
        app.start(port);
        return this;
    }

    public int port() { return app.port(); }

    private synchronized GameSession create(peiyi configuration)
    {
        if (sessions.size() >= maxSessions)
            throw new ApiException(503, "SESSION_LIMIT", "游客会话数量已达上限，请释放旧会话");
        String id = UUID.randomUUID().toString();
        GameSession session = sessionFactory.apply(id);
        try {
            session.startGame(new StartGameCommand(0, configuration));
            sessions.put(id, session);
            return session;
        } catch (RuntimeException error) {
            session.close();
            throw error;
        }
    }

    private synchronized GameSession session(String id)
    {
        GameSession session = sessions.get(id);
        if (session == null) throw new ApiException(404, "GAME_NOT_FOUND", "游客对局不存在");
        return session;
    }

    private synchronized void remove(String id)
    {
        GameSession session = session(id);
        sessions.remove(id);
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
        app.stop();
        synchronized (this) {
            sessions.values().forEach(GameSession::close);
            sessions.clear();
        }
    }

    public static void main(String[] args)
    {
        GuestGameServer server = new GuestGameServer().start(
                args.length == 0 ? 7070 : Integer.parseInt(args[0]));
        DebugLogger.info("[HTTP] 游客 API: http://127.0.0.1:" + server.port() + "/api/v1/games");
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
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
