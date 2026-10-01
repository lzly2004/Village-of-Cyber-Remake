package com.vocr.application;

/** 写命令依据的状态版本已经落后于当前对局。 */
public final class StaleGameRevisionException extends IllegalStateException
{
    private final long expectedRevision;
    private final long currentRevision;

    public StaleGameRevisionException(long expectedRevision, long currentRevision)
    {
        super("对局状态版本已过期: expected=" + expectedRevision
                + ", current=" + currentRevision);
        this.expectedRevision = expectedRevision;
        this.currentRevision = currentRevision;
    }

    public long expectedRevision()
    {
        return expectedRevision;
    }

    public long currentRevision()
    {
        return currentRevision;
    }
}
