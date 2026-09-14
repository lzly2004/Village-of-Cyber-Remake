package com.vocr.application;

import java.util.Objects;

/** 投票是否已完成，以及执行后尚未消费的领域事件快照。 */
public record VoteExecutionResult(boolean voteCompleted, GameUpdate update)
{
    public VoteExecutionResult
    {
        Objects.requireNonNull(update, "投票结果更新不能为空");
    }
}
