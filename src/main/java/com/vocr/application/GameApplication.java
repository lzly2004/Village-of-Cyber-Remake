package com.vocr.application;

import com.vocr.core.MainLogicInterface;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;

import java.util.List;
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
    private long revision;
    private boolean avoidanceEnabled = true;

    public GameApplication(MainLogicInterface gameLogic, GameEventBuffer eventBuffer)
    {
        this.gameLogic = Objects.requireNonNull(gameLogic, "主逻辑不能为空");
        this.eventBuffer = Objects.requireNonNull(eventBuffer, "事件缓冲区不能为空");
    }

    public synchronized StartedGame startGame(StartGameCommand command)
    {
        Objects.requireNonNull(command, "开局命令不能为空");
        requireCurrentRevision(command);
        eventBuffer.reset();
        gameLogic.start(command.configuration());
        GameContextView context = Objects.requireNonNull(
                gameLogic.getGameContext(), "主逻辑开局后未提供游戏状态");
        avoidanceEnabled = true;
        advanceRevision();
        return new StartedGame(
                revision, context.getPeiyi(), context.getPlayerSum(), context.getGameDay());
    }

    public synchronized GameUpdate requestClaims(AskClaimByRoleCommand command)
    {
        Objects.requireNonNull(command, "按职业询问命令不能为空");
        requireCurrentRevision(command);
        GameContextView context = requireStartedContext();
        requireDayDecision(context, "询问 CO");
        if (!LegalActionProjector.claimInquiry(context).roles().contains(command.role()))
            throw new IllegalArgumentException("当前不能询问该职业 CO: " + command.role());
        long eventCursor = eventBuffer.currentCursor();
        gameLogic.askCo(command.role());
        advanceRevision();
        return currentUpdate(eventCursor);
    }

    public synchronized GameUpdate requestClaims(AskClaimByPlayersCommand command)
    {
        Objects.requireNonNull(command, "按玩家询问命令不能为空");
        requireCurrentRevision(command);
        GameContextView context = requireStartedContext();
        requireDayDecision(context, "询问 CO");
        if (command.players().stream().anyMatch(player -> player > context.getPlayerSum()))
            throw new IllegalArgumentException(
                    "询问玩家编号超出当前对局范围: " + command.players());
        List<Integer> legalPlayers = LegalActionProjector.claimInquiry(context).players();
        if (!legalPlayers.containsAll(command.players()))
            throw new IllegalArgumentException(
                    "询问对象必须全部来自当前合法候选: " + command.players());
        long eventCursor = eventBuffer.currentCursor();
        gameLogic.askCo(command.players());
        advanceRevision();
        return currentUpdate(eventCursor);
    }

    private GameContextView requireStartedContext()
    {
        GameContextView context = gameLogic.getGameContext();
        if (context == null)
            throw new IllegalStateException("尚未开始对局");
        return context;
    }

    public synchronized VoteExecutionResult executeVote(ExecuteVoteCommand command)
    {
        Objects.requireNonNull(command, "投票命令不能为空");
        requireCurrentRevision(command);
        GameContextView context = requireStartedContext();
        if (context.getEndResult() != GameResult.NONE)
            throw new IllegalStateException("对局已经结束，不能继续投票");
        if (command.candidates().stream().anyMatch(player -> player > context.getPlayerSum()))
            throw new IllegalArgumentException(
                    "投票候补编号超出当前对局范围: " + command.candidates());
        if (command.candidates().stream().anyMatch(player -> !context.isAlive(player)))
            throw new IllegalArgumentException(
                    "投票候补必须全部存活: " + command.candidates());
        LegalVoteAction legalAction = VoteCandidateProjector.project(context).stream()
                .filter(action -> action.method() == command.method())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "当前没有该投票动作: " + command.method()));
        if (!legalAction.candidates().equals(command.candidates()))
            throw new IllegalArgumentException(
                    "投票候补与当前合法动作不一致: " + command.candidates());

        long eventCursor = eventBuffer.currentCursor();
        boolean voteCompleted = gameLogic.shokei(
                command.method().legacyCode(), command.candidates(), avoidanceEnabled);
        advanceRevision();
        return new VoteExecutionResult(voteCompleted, currentUpdate(eventCursor));
    }

    public synchronized GameUpdate setTargetSelection(SetAllTargetSelectionCommand command)
    {
        Objects.requireNonNull(command, "全体目标命令不能为空");
        requireCurrentRevision(command);
        GameContextView context = requireStartedContext();
        requireDayDecision(context, "预约目标");
        requireAlivePlayer(context, command.target(), "目标玩家");
        LegalTargetSelection legalAction = legalTargetSelection(context, command.type());
        if (!legalAction.targets().contains(command.target()))
            throw new IllegalArgumentException("目标不在当前合法候选中: " + command.target());
        int day = context.getGameDay();
        long eventCursor = eventBuffer.currentCursor();

        if (command.type() == SelectionType.VOTE)
        {
            context.setSelectedVoteTarget(command.target(), day, command.selected());
            advanceRevision();
            return currentUpdate(eventCursor);
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
        advanceRevision();
        return currentUpdate(eventCursor);
    }

    public synchronized GameUpdate setTargetSelection(SetClaimedRoleTargetSelectionCommand command)
    {
        Objects.requireNonNull(command, "CO 目标命令不能为空");
        requireCurrentRevision(command);
        GameContextView context = requireStartedContext();
        requireDayDecision(context, "预约目标");
        requireAlivePlayer(context, command.claimedPlayer(), "CO 玩家");
        requireAlivePlayer(context, command.target(), "目标玩家");
        LegalTargetSelection legalAction = legalTargetSelection(context, command.type());
        if (!legalAction.targets().contains(command.target()))
            throw new IllegalArgumentException("目标不在当前合法候选中: " + command.target());
        if (legalAction.claimedActors().stream()
                .noneMatch(actor -> actor.playerNumber() == command.claimedPlayer()))
            throw new IllegalArgumentException(
                    "玩家不是当前合法的 CO 技能行动者: " + command.claimedPlayer());
        Role scheduledRole = scheduledRole(command.type());
        if (context.getClaimedRole(command.claimedPlayer()) != scheduledRole.ordinal())
            throw new IllegalArgumentException(
                    "玩家未声称该技能职业: " + command.claimedPlayer());

        long eventCursor = eventBuffer.currentCursor();
        context.setClaimedRoleScheduled(
                command.claimedPlayer(), command.target(), context.getGameDay(), command.selected());
        advanceRevision();
        return currentUpdate(eventCursor);
    }

    public synchronized GameUpdate setAvoidance(SetAvoidanceCommand command)
    {
        Objects.requireNonNull(command, "回避设置命令不能为空");
        requireCurrentRevision(command);
        GameContextView context = requireStartedContext();
        requireDayDecision(context, "设置投票回避");
        long eventCursor = eventBuffer.currentCursor();
        avoidanceEnabled = command.enabled();
        advanceRevision();
        return currentUpdate(eventCursor);
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

    private static void requireDayDecision(GameContextView context, String action)
    {
        if (context.getEndResult() != GameResult.NONE)
            throw new IllegalStateException("对局已经结束，不能继续" + action);
    }

    private static LegalTargetSelection legalTargetSelection(
            GameContextView context, SelectionType type)
    {
        return LegalActionProjector.targetSelections(context).stream()
                .filter(action -> action.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "当前没有该目标预约动作: " + type));
    }

    /**
     * 返回桌面端尚未确认展示的事件后缀。该窗口可能晚于本局事件起点；需要可靠续读的
     * 调用方必须保存并传入自己的事件游标。
     */
    public synchronized GameUpdate currentUpdate()
    {
        return toUpdate(eventBuffer.pendingSnapshot());
    }

    /**
     * 返回指定游标之后发布的全部事件。读取不会影响桌面端的待展示队列；更新中的
     * {@code eventCursor} 与 {@code nextEventCursor} 明确标识事件窗口，重复使用同一游标
     * 会得到相同结果。
     */
    public synchronized GameUpdate currentUpdate(long eventCursor)
    {
        return toUpdate(eventBuffer.snapshotAfter(eventCursor));
    }

    private GameUpdate toUpdate(EventBatch batch)
    {
        GameContextView context = requireStartedContext();
        BusinessPhase phase = context.getEndResult() == GameResult.NONE
                ? BusinessPhase.DAY_DECISION
                : BusinessPhase.FINISHED;
        boolean decisionPhase = phase == BusinessPhase.DAY_DECISION;
        return new GameUpdate(
                revision,
                phase,
                PlayerViewProjector.project(context),
                SelectionStateProjector.project(context, avoidanceEnabled),
                decisionPhase
                        ? VoteCandidateProjector.project(context)
                        : List.of(),
                decisionPhase
                        ? LegalActionProjector.claimInquiry(context)
                        : ClaimInquiryOptions.unavailable(),
                decisionPhase
                        ? LegalActionProjector.targetSelections(context)
                        : List.of(),
                decisionPhase
                        ? List.of()
                        : SettlementRevealProjector.project(context),
                batch.cursor(),
                batch.nextCursor(),
                batch.events());
    }

    /** 当前应用状态版本；开局前为 0，成功写命令后严格递增。 */
    public synchronized long currentRevision()
    {
        return revision;
    }

    private void requireCurrentRevision(RevisionedCommand command)
    {
        if (command.expectedRevision() != revision)
            throw new StaleGameRevisionException(command.expectedRevision(), revision);
    }

    private void advanceRevision()
    {
        revision = Math.incrementExact(revision);
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
