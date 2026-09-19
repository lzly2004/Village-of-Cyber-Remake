package com.vocr.application;

import com.vocr.domain.Event;
import com.vocr.domain.SelectionType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 当前应用更新的不可变快照。
 *
 * 包含业务阶段、白名单玩家视图、当前合法动作、事件输出及其续读游标；完整状态
 * revision 会在相应写操作全部迁入应用层后加入。
 */
public record GameUpdate(
        BusinessPhase phase,
        PlayerViewSnapshot view,
        List<LegalVoteAction> legalVoteActions,
        ClaimInquiryOptions claimInquiry,
        List<LegalTargetSelection> legalTargetSelections,
        long eventCursor,
        long nextEventCursor,
        List<Event> events)
{
    public GameUpdate
    {
        Objects.requireNonNull(phase, "业务阶段不能为空");
        Objects.requireNonNull(view, "玩家视图不能为空");
        Objects.requireNonNull(claimInquiry, "CO 询问选项不能为空");
        legalVoteActions = List.copyOf(legalVoteActions);
        legalTargetSelections = List.copyOf(legalTargetSelections);
        if (phase == BusinessPhase.DAY_DECISION
                && (legalVoteActions.isEmpty() || !claimInquiry.available()
                || legalTargetSelections.size() != SelectionType.values().length))
            throw new IllegalArgumentException("白天决策阶段必须包含完整合法动作");
        if (phase == BusinessPhase.FINISHED
                && (!legalVoteActions.isEmpty() || claimInquiry.available()
                || !legalTargetSelections.isEmpty()))
            throw new IllegalArgumentException("结束阶段不能包含合法动作");
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
