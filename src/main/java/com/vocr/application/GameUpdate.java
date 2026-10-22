package com.vocr.application;

import com.vocr.domain.Event;
import com.vocr.domain.SelectionType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 当前应用更新的不可变快照。
 *
 * 包含状态 revision、业务阶段、白名单玩家视图、当前选择、合法动作、公开投票历史、
 * 原始事件、跨端展示阶段投影及其续读游标。
 */
public record GameUpdate(
        long revision,
        BusinessPhase phase,
        PlayerViewSnapshot view,
        SelectionStateSnapshot selections,
        List<LegalVoteAction> legalVoteActions,
        ClaimInquiryOptions claimInquiry,
        List<LegalTargetSelection> legalTargetSelections,
        List<VoteResultSnapshot> voteHistory,
        List<RevealedPlayerRole> revealedRoles,
        PublicInformation publicInformation,
        long eventCursor,
        long nextEventCursor,
        List<Event> events,
        List<PresentedEvent> presentedEvents)
{
    public GameUpdate
    {
        if (revision < 0)
            throw new IllegalArgumentException("状态版本不能为负数");
        Objects.requireNonNull(phase, "业务阶段不能为空");
        Objects.requireNonNull(view, "玩家视图不能为空");
        Objects.requireNonNull(selections, "当前选择状态不能为空");
        Objects.requireNonNull(claimInquiry, "CO 询问选项不能为空");
        Objects.requireNonNull(publicInformation, "公开信息不能为空");
        if ((phase == BusinessPhase.FINISHED) != (view.result() != com.vocr.domain.GameResult.NONE))
            throw new IllegalArgumentException("阶段与胜负不一致");
        if (view.players().stream().anyMatch(player -> !player.deathReason().allowedIn(phase)))
            throw new IllegalArgumentException("死因值不属于当前阶段的公开范围");
        if ((phase == BusinessPhase.FINISHED || view.gameDay() <= 2) && publicInformation.suspicion().available())
            throw new IllegalArgumentException("此阶段不公开怀疑摘要");
        for (var report : publicInformation.skillReports())
        {
            var actor = view.players().stream().filter(player -> player.playerNumber() == report.playerNumber()).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未知公开报告行动者"));
            if (phase == BusinessPhase.FINISHED || actor.claimedRole() != report.claimedRole()
                    || actor.claimedRoleOrder() != report.claimedRoleOrder()
                    || report.results().stream().anyMatch(result -> result.skillDay() < (report.claimedRole() == com.vocr.domain.Role.zhan ? 1 : 2)
                    || result.skillDay() >= view.gameDay() || result.targetPlayerNumber() > view.players().size()
                    || (actor.deathDay() > 0 && result.skillDay() >= actor.deathDay())))
                throw new IllegalArgumentException("报告超出当前公开范围");
        }
        for (var row : publicInformation.suspicion().rows())
            if (view.players().stream().noneMatch(player -> player.playerNumber() == row.playerNumber() && player.alive())
                    || row.targets().stream().anyMatch(target -> target > view.players().size()))
                throw new IllegalArgumentException("怀疑摘要超出公开范围");
        legalVoteActions = List.copyOf(legalVoteActions);
        legalTargetSelections = List.copyOf(legalTargetSelections);
        voteHistory = List.copyOf(voteHistory);
        revealedRoles = List.copyOf(revealedRoles);
        if (phase == BusinessPhase.DAY_DECISION
                && (legalVoteActions.isEmpty() || !claimInquiry.available()
                || legalTargetSelections.size() != SelectionType.values().length
                || !revealedRoles.isEmpty()))
            throw new IllegalArgumentException("白天决策阶段必须包含完整合法动作");
        if (phase == BusinessPhase.FINISHED
                && (!legalVoteActions.isEmpty() || claimInquiry.available()
                || !legalTargetSelections.isEmpty()
                || revealedRoles.size() != view.players().size()))
            throw new IllegalArgumentException("结束阶段必须仅包含完整揭晓数据");
        if (!revealedRoles.isEmpty()
                && !revealedRoles.stream().map(RevealedPlayerRole::playerNumber).toList()
                .equals(view.players().stream().map(VisiblePlayer::playerNumber).toList()))
            throw new IllegalArgumentException("揭晓职业必须与公开玩家一一对应");
        if (legalVoteActions.stream().map(LegalVoteAction::method).distinct().count()
                != legalVoteActions.size())
            throw new IllegalArgumentException("同一投票方式不能重复");
        if (legalTargetSelections.stream().map(LegalTargetSelection::type).distinct().count()
                != legalTargetSelections.size())
            throw new IllegalArgumentException("同一目标预约类型不能重复");
        if (voteHistory.stream().map(VoteResultSnapshot::day).distinct().count()
                != voteHistory.size())
            throw new IllegalArgumentException("同一游戏日不能出现多份投票结果");
        for (int index = 1; index < voteHistory.size(); index++)
            if (voteHistory.get(index - 1).day() >= voteHistory.get(index).day())
                throw new IllegalArgumentException("投票历史必须按日期递增排列");
        if (eventCursor < 0 || nextEventCursor < eventCursor)
            throw new IllegalArgumentException("事件游标窗口无效");
        events = List.copyOf(events);
        presentedEvents = List.copyOf(presentedEvents);
        if (nextEventCursor - eventCursor != events.size())
            throw new IllegalArgumentException("事件数量与游标窗口不一致");
        if (presentedEvents.size() != events.size())
            throw new IllegalArgumentException("事件展示投影与原始事件数量不一致");
        for (int index = 0; index < events.size(); index++)
            if (!events.get(index).equals(presentedEvents.get(index).event()))
                throw new IllegalArgumentException("事件展示投影与原始事件顺序不一致");
    }

    public Optional<LegalVoteAction> legalVote(VotingMethod method)
    {
        Objects.requireNonNull(method, "投票方式不能为空");
        return legalVoteActions.stream().filter(action -> action.method() == method).findFirst();
    }

    public Optional<LegalTargetSelection> legalTargetSelection(SelectionType type)
    {
        Objects.requireNonNull(type, "目标预约类型不能为空");
        return legalTargetSelections.stream().filter(action -> action.type() == type).findFirst();
    }

    public Optional<VoteResultSnapshot> latestVoteResult()
    {
        return voteHistory.isEmpty()
                ? Optional.empty()
                : Optional.of(voteHistory.get(voteHistory.size() - 1));
    }
}
