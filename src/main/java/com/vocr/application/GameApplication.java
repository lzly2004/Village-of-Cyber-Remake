package com.vocr.application;

import com.vocr.core.MainLogicInterface;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;

import java.util.Objects;

/**
 * 平台无关的对局应用入口。
 *
 * 当前先迁移开局用例；后续业务操作按垂直切片逐步加入，Swing 与 Web 均不得绕过这里
 * 复制一份规则流程。
 */
public final class GameApplication
{
    private final MainLogicInterface gameLogic;
    private final GameEventBuffer eventBuffer;

    public GameApplication(MainLogicInterface gameLogic, GameEventBuffer eventBuffer)
    {
        this.gameLogic = Objects.requireNonNull(gameLogic, "主逻辑不能为空");
        this.eventBuffer = Objects.requireNonNull(eventBuffer, "事件缓冲区不能为空");
    }

    public StartedGame startGame(StartGameCommand command)
    {
        Objects.requireNonNull(command, "开局命令不能为空");
        gameLogic.start(command.configuration());
        GameContextView context = Objects.requireNonNull(
                gameLogic.getGameContext(), "主逻辑开局后未提供游戏状态");
        return new StartedGame(context.getPeiyi(), context.getPlayerSum(), context.getGameDay());
    }

    public GameUpdate requestClaims(AskClaimByRoleCommand command)
    {
        Objects.requireNonNull(command, "按职业询问命令不能为空");
        requireStartedContext();
        gameLogic.askCo(command.role());
        return currentUpdate();
    }

    public GameUpdate requestClaims(AskClaimByPlayersCommand command)
    {
        Objects.requireNonNull(command, "按玩家询问命令不能为空");
        GameContextView context = requireStartedContext();
        if (command.players().stream().anyMatch(player -> player > context.getPlayerSum()))
            throw new IllegalArgumentException(
                    "询问玩家编号超出当前对局范围: " + command.players());
        gameLogic.askCo(command.players());
        return currentUpdate();
    }

    private GameContextView requireStartedContext()
    {
        GameContextView context = gameLogic.getGameContext();
        if (context == null)
            throw new IllegalStateException("尚未开始对局");
        return context;
    }

    public GameUpdate currentUpdate()
    {
        return new GameUpdate(eventBuffer.snapshot());
    }

    /** 桌面迁移期用于确认已展示的队首事件；Web 适配器后续改用事件游标。 */
    public Event pollNextEvent()
    {
        return eventBuffer.poll();
    }

    public Event peekNextEvent()
    {
        return eventBuffer.peek();
    }

    public boolean hasPendingEvents()
    {
        return peekNextEvent() != null;
    }

    public void clearPendingEvents()
    {
        eventBuffer.clear();
    }

    /** 仅保留现有桌面调试兜底；正常领域事件必须由核心写入缓冲区。 */
    public void publishDiagnosticEvent(Event event)
    {
        eventBuffer.publish(event);
    }
}
