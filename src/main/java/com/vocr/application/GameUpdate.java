package com.vocr.application;

import com.vocr.domain.Event;
import com.vocr.domain.SelectionType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 当前应用更新的不可变快照。
 *
 * 包含状态 revision、业务阶段、白名单玩家视图、当前合法动作、事件输出及其续读游标。
 */
public record GameUpdate(
        long revision,
        BusinessPhase phase,
        PlayerViewSnapshot view,
        List<LegalVoteAction> legalVoteActions,
        ClaimInquiryOptions claimInquiry,
        List<LegalTargetSelection> legalTargetSelections,
        List<RevealedPlayerRole> revealedRoles,
        long eventCursor,
        long nextEventCursor,
        List<Event> events)
{
    public GameUpdate
    {
        if (revision < 0)
            throw new IllegalArgumentException("状态版本不能为负数");
        Objects.requireNonNull(phase, "业务阶段不能为空");
        Objects.requireNonNull(view, "玩家视图不能为空");
        Objects.requireNonNull(claimInquiry, "CO 询问选项不能为空");
        legalVoteActions = List.copyOf(legalVoteActions);
        legalTargetSelections = List.copyOf(legalTargetSelections);
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
        if (eventCursor < 0 || nextEventCursor < eventCursor)
            throw new IllegalArgumentException("事件游标窗口无效");
        events = List.copyOf(events);
        if (nextEventCursor - eventCursor != events.size())
            throw new IllegalArgumentException("事件数量与游标窗口不一致");
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
}
