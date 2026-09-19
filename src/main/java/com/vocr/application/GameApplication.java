package com.vocr.application;

import com.vocr.core.MainLogicInterface;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;

import java.util.Objects;

/**
 * 平台无关的对局应用入口。
 *
 * 开局、CO 询问、目标预约与投票/处刑已经按垂直切片迁入；后续业务操作继续逐步加入，
 * Swing 与 Web 均不得绕过这里复制一份规则流程。
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
        eventBuffer.reset();
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

    public VoteExecutionResult executeVote(ExecuteVoteCommand command)
    {
        Objects.requireNonNull(command, "投票命令不能为空");
        GameContextView context = requireStartedContext();
        if (command.candidates().stream().anyMatch(player -> player > context.getPlayerSum()))
            throw new IllegalArgumentException(
                    "投票候补编号超出当前对局范围: " + command.candidates());
        if (command.candidates().stream().anyMatch(player -> !context.isAlive(player)))
            throw new IllegalArgumentException(
                    "投票候补必须全部存活: " + command.candidates());

        boolean voteCompleted = gameLogic.shokei(
                command.method().legacyCode(), command.candidates(), command.avoidanceEnabled());
        return new VoteExecutionResult(voteCompleted, currentUpdate());
    }

    public GameUpdate setTargetSelection(SetAllTargetSelectionCommand command)
    {
        Objects.requireNonNull(command, "全体目标命令不能为空");
        GameContextView context = requireStartedContext();
        requireAlivePlayer(context, command.target(), "目标玩家");
        int day = context.getGameDay();

        if (command.type() == SelectionType.VOTE)
        {
            context.setSelectedVoteTarget(command.target(), day, command.selected());
            return currentUpdate();
        }

        Role scheduledRole = scheduledRole(command.type());
        if (command.type() == SelectionType.DIVINATION)
            context.setHiddenSeerScheduled(command.target(), day, command.selected());
        else
            context.setHiddenHunterScheduled(command.target(), day, command.selected());

        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            if (context.isAlive(player)
                    && (context.getActualRole(player) == scheduledRole.ordinal()
                    || context.getClaimedRole(player) == scheduledRole.ordinal()))
            {
                context.setClaimedRoleScheduled(
                        player, command.target(), day, command.selected());
            }
        }
        return currentUpdate();
    }

    public GameUpdate setTargetSelection(SetClaimedRoleTargetSelectionCommand command)
    {
        Objects.requireNonNull(command, "CO 目标命令不能为空");
        GameContextView context = requireStartedContext();
        requireAlivePlayer(context, command.claimedPlayer(), "CO 玩家");
        requireAlivePlayer(context, command.target(), "目标玩家");
        Role scheduledRole = scheduledRole(command.type());
        if (context.getClaimedRole(command.claimedPlayer()) != scheduledRole.ordinal())
            throw new IllegalArgumentException(
                    "玩家未声称该技能职业: " + command.claimedPlayer());

        context.setClaimedRoleScheduled(
                command.claimedPlayer(), command.target(), context.getGameDay(), command.selected());
        return currentUpdate();
    }

    private static Role scheduledRole(SelectionType type)
    {
        return switch (type)
        {
            case DIVINATION -> Role.zhan;
            case GUARD -> Role.lie;
            case VOTE -> throw new IllegalArgumentException("指定投票不包含技能职业");
        };
    }

    private static void requireAlivePlayer(GameContextView context, int player, String description)
    {
        if (player > context.getPlayerSum())
            throw new IllegalArgumentException(description + "编号超出当前对局范围: " + player);
        if (!context.isAlive(player))
            throw new IllegalArgumentException(description + "必须存活: " + player);
    }

    public GameUpdate currentUpdate()
    {
        return toUpdate(eventBuffer.pendingSnapshot());
    }

    /**
     * 返回指定游标之后发布的全部事件。读取不会影响桌面端的待展示队列；返回的下一游标
     * 可用于后续轮询，重复使用同一游标会得到相同结果。
     */
    public GameUpdate currentUpdate(long eventCursor)
    {
        return toUpdate(eventBuffer.snapshotAfter(eventCursor));
    }

    private static GameUpdate toUpdate(EventBatch batch)
    {
        return new GameUpdate(batch.nextCursor(), batch.events());
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
