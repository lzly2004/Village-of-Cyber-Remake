package com.vocr.replay;

import com.vocr.application.ReplayRecording;
import com.vocr.application.ReplaySaveSink;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Desktop adapter: keeps the existing ReplaySave v3 / nine-slot representation. */
public final class ReplaySlotSink implements ReplaySaveSink
{
    private final ReplayManager manager;
    private final int slot;

    public ReplaySlotSink(ReplayManager manager, int slot)
    {
        this.manager = Objects.requireNonNull(manager, "Replay存储不能为空");
        if (slot < 0 || slot >= ReplayManager.MAX_SLOTS)
            throw new IllegalArgumentException("Replay槽位越界");
        this.slot = slot;
    }

    @Override public void save(ReplayRecording recording)
    {
        ReplaySave save = ReplaySave.fromRecords(slot, Objects.requireNonNull(recording).records());
        try { manager.saveToSlotChecked(slot, save); }
        catch (IOException error) { throw new UncheckedIOException("Replay保存失败", error); }
    }
}
