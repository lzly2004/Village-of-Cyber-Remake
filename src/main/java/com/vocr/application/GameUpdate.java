package com.vocr.application;

import com.vocr.domain.Event;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 当前应用更新的不可变快照。
 *
 * 现阶段包含业务阶段、白名单玩家视图、合法投票动作、事件输出及其续读游标；其他合法
 * 动作和完整状态 revision 会在相应写操作全部迁入应用层后加入。
 */
public record GameUpdate(
        BusinessPhase phase,
        PlayerViewSnapshot view,
        List<LegalVoteAction> legalVoteActions,
        long nextEventCursor,
        List<Event> events)
{
    public GameUpdate
    {
        Objects.requireNonNull(phase, "业务阶段不能为空");
        Objects.requireNonNull(view, "玩家视图不能为空");
        legalVoteActions = List.copyOf(legalVoteActions);
        if (phase == BusinessPhase.DAY_DECISION && legalVoteActions.isEmpty())
            throw new IllegalArgumentException("白天决策阶段必须包含合法投票动作");
        if (phase == BusinessPhase.FINISHED && !legalVoteActions.isEmpty())
            throw new IllegalArgumentException("结束阶段不能包含合法投票动作");
        if (legalVoteActions.stream().map(LegalVoteAction::method).distinct().count()
                != legalVoteActions.size())
            throw new IllegalArgumentException("同一投票方式不能重复");
        if (nextEventCursor < 0)
            throw new IllegalArgumentException("下一事件游标不能为负数");
        events = List.copyOf(events);
    }

    public Optional<LegalVoteAction> legalVote(VotingMethod method)
    {
        Objects.requireNonNull(method, "投票方式不能为空");
        return legalVoteActions.stream().filter(action -> action.method() == method).findFirst();
    }
}
