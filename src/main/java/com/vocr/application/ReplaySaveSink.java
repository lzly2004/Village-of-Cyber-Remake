package com.vocr.application;

/** The caller binds a destination; persistence failure must propagate, never imply success. */
@FunctionalInterface
public interface ReplaySaveSink
{
    void save(ReplayRecording recording);
}
