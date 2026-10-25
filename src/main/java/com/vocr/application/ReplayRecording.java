package com.vocr.application;

import java.util.List;

/** A detached recording, not a mutable recorder, context, file or session credential. */
public record ReplayRecording(List<String> records)
{
    public ReplayRecording
    {
        records = List.copyOf(records);
        if (records.isEmpty()) throw new IllegalArgumentException("录像记录不能为空");
    }
}
