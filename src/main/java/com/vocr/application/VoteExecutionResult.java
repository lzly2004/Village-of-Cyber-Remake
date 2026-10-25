package com.vocr.application;

import java.util.Objects;

/** 投票是否已完成，以及执行后尚未消费的领域事件快照。 */
public record VoteExecutionResult(boolean voteCompleted, GameUpdate update)
{
    public VoteExecutionResult
    {
        Objects.requireNonNull(update, "投票结果更新不能为空");
    }

    public VoteResultSnapshot completedVote()
    {
        if (!voteCompleted)
            throw new IllegalStateException("本次投票被 CO 回避中断，没有完成结果");
        return update.latestVoteResult().orElseThrow(
                () -> new IllegalStateException("核心报告投票完成，但应用更新缺少投票结果"));
    }
}
