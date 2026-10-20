package com.vocr.application;

import com.vocr.core.GameRecorder;
import com.vocr.core.MainLogicInterface;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;

import java.util.Objects;
import java.util.UUID;

/**
 * 单局对局会话：拥有本局的应用门面、事件缓冲区、状态版本与生命周期。
 *
 * <p>同一会话的命令、事件读取和版本查询都通过本类串行进入；桌面端和未来 Web
 * 适配器共用这一入口。会话不选择持久化路径，组合根必须为并发会话注入相互隔离的
 * 战绩存储。</p>
 */
public final class GameSession
{
    /** 会话生命周期；一局只允许启动一次。 */
    public enum State
    {
        NEW,
        RUNNING,
        FINISHED,
        CLOSED
    }

    private final String sessionId;
    private final MainLogicInterface gameLogic;
    private final GameEventBuffer eventBuffer;
    private final GameApplication application;
    private State state = State.NEW;

    /**
     * @param gameLogic 必须已把领域事件投递到传入的 {@code eventBuffer}，否则更新无法携带事件
     * @param eventBuffer 本局独占的事件缓冲区
     */
    public GameSession(String sessionId, MainLogicInterface gameLogic, GameEventBuffer eventBuffer)
    {
        if (sessionId == null || sessionId.isBlank())
            throw new IllegalArgumentException("会话编号不能为空");
        this.sessionId = sessionId;
        this.gameLogic = Objects.requireNonNull(gameLogic, "主逻辑不能为空");
        this.eventBuffer = Objects.requireNonNull(eventBuffer, "事件缓冲区不能为空");
        this.application = new GameApplication(gameLogic, eventBuffer);
    }

    public GameSession(MainLogicInterface gameLogic, GameEventBuffer eventBuffer)
    {
        this(UUID.randomUUID().toString(), gameLogic, eventBuffer);
    }

    public String sessionId()
    {
        return sessionId;
    }

    public synchronized State state()
    {
        return state;
    }

    public synchronized boolean isFinished()
    {
        return state == State.FINISHED;
    }

    public synchronized StartedGame startGame(StartGameCommand command)
    {
        requireState(State.NEW, "开始对局");
        StartedGame started = application.startGame(command);
        state = State.RUNNING;
        return started;
    }

    /** 桌面端尚未确认展示的事件后缀。 */
    public synchronized GameUpdate currentUpdate()
    {
        requireOpen();
        return application.currentUpdate();
    }

    /** 返回指定事件游标之后发布的全部事件；读取不会推进桌面展示队列。 */
    public synchronized GameUpdate currentUpdate(long eventCursor)
    {
        requireOpen();
        return application.currentUpdate(eventCursor);
    }

    public synchronized long currentRevision()
    {
        return application.currentRevision();
    }

    /** 当前已发布事件总数，可作为下一次续读的游标。 */
    public synchronized long currentEventCursor()
    {
        return eventBuffer.currentCursor();
    }

    public synchronized GameUpdate requestClaims(AskClaimByRoleCommand command)
    {
        requireState(State.RUNNING, "询问 CO");
        return observe(application.requestClaims(command));
    }

    public synchronized GameUpdate requestClaims(AskClaimByPlayersCommand command)
    {
        requireState(State.RUNNING, "询问 CO");
        return observe(application.requestClaims(command));
    }

    public synchronized VoteExecutionResult executeVote(ExecuteVoteCommand command)
    {
        requireState(State.RUNNING, "投票");
        VoteExecutionResult result = application.executeVote(command);
        observe(result.update());
        return result;
    }

    public synchronized GameUpdate setTargetSelection(SetAllTargetSelectionCommand command)
    {
        requireState(State.RUNNING, "预约目标");
        return observe(application.setTargetSelection(command));
    }

    public synchronized GameUpdate setTargetSelection(SetClaimedRoleTargetSelectionCommand command)
    {
        requireState(State.RUNNING, "预约目标");
        return observe(application.setTargetSelection(command));
    }

    public synchronized GameUpdate setAvoidance(SetAvoidanceCommand command)
    {
        requireState(State.RUNNING, "设置投票回避");
        return observe(application.setAvoidance(command));
    }

    /**
     * 桌面迁移期的只读状态访问；Web 适配器不得把该视图直接序列化给浏览器。
     */
    public synchronized GameContextView contextView()
    {
        return gameLogic.getGameContext();
    }

    /** 桌面迁移期用于把本局录像写入 Replay 槽位；开局前为 {@code null}。 */
    public synchronized GameRecorder recorder()
    {
        return gameLogic.getRecorder();
    }

    public synchronized PresentedEvent pollNextPresentedEvent()
    {
        requireOpen();
        return application.pollNextPresentedEvent();
    }

    public synchronized PresentedEvent peekNextPresentedEvent()
    {
        requireOpen();
        return application.peekNextPresentedEvent();
    }

    public synchronized boolean hasPendingEvents()
    {
        return state != State.CLOSED && application.hasPendingEvents();
    }

    public synchronized void clearPendingEvents()
    {
        if (state != State.CLOSED)
            application.clearPendingEvents();
    }

    /** 仅保留桌面调试兜底；正常领域事件必须由核心写入缓冲区。 */
    public synchronized void publishDiagnosticEvent(Event event)
    {
        requireOpen();
        application.publishDiagnosticEvent(event);
    }

    /** 标记会话关闭；关闭后不再接受命令或读取。 */
    public synchronized void close()
    {
        state = State.CLOSED;
    }

    private GameUpdate observe(GameUpdate update)
    {
        if (update.phase() == BusinessPhase.FINISHED)
            state = State.FINISHED;
        return update;
    }

    private void requireOpen()
    {
        if (state == State.CLOSED)
            throw new IllegalStateException("对局会话已经关闭");
    }

    private void requireState(State expected, String action)
    {
        if (state == State.CLOSED)
            throw new IllegalStateException("对局会话已经关闭，不能" + action);
        if (state != expected)
            throw new IllegalStateException("当前会话状态为 " + state + "，不能" + action);
    }
}
