package com.vocr.application;

/** 修改当前对局投票回避开关。 */
public record SetAvoidanceCommand(
        long expectedRevision,
        boolean enabled) implements RevisionedCommand
{
    public SetAvoidanceCommand
    {
        RevisionedCommand.requireValid(expectedRevision);
    }
}
