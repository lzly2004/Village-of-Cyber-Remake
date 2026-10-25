package com.vocr.application;

/** 携带调用方所见应用状态版本的写命令。 */
public interface RevisionedCommand
{
    long expectedRevision();

    static void requireValid(long expectedRevision)
    {
        if (expectedRevision < 0)
            throw new IllegalArgumentException("预期状态版本不能为负数: " + expectedRevision);
    }
}
