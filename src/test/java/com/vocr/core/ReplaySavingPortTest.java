package com.vocr.core;

import com.vocr.application.*;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ReplaySavingPortTest {
    private record Fixture(GameSession session, MainLogic logic, GameRecordManager stats) {}
    private Fixture fixture(String id, long seed) {
        var events = new GameEventBuffer();
        var stats = GameRecordManager.inMemory();
        var logic = new MainLogic(events::publish, stats, new GameRandom(seed));
        return new Fixture(new GameSession(id, logic, events, stats), logic, stats);
    }
    private void start(Fixture f, peiyi configuration) {
        f.session.startGame(new StartGameCommand(0, configuration));
        f.session.setAvoidance(new SetAvoidanceCommand(f.session.currentRevision(), false));
    }
    private void finish(Fixture f) {
        for (int guard=0; !f.session.isFinished(); guard++) {
            assertTrue(guard < 40, "fixed-seed game failed to terminate");
            var action = f.session.currentUpdate().legalVote(VotingMethod.FREE).orElseThrow();
            f.session.executeVote(new ExecuteVoteCommand(f.session.currentRevision(), action.method(), action.candidates()));
        }
    }

    @Test void recordingDefensivelyCopiesAndDoesNotExposeMutableCore() {
        var original = new ArrayList<>(List.of("JSON|{\n\"text\":\"ä/ü\"\n}"));
        var recording = new ReplayRecording(original);
        original.clear();
        assertEquals(1, recording.records().size());
        assertThrows(UnsupportedOperationException.class, () -> recording.records().clear());
        assertThrows(IllegalArgumentException.class, () -> new ReplayRecording(List.of()));
    }

    @Test void completedExportRequiresFinishedAndExactCursorButNeverDrainsOrWrites() {
        var f = fixture("PRIVATE_SESSION_DO_NOT_EXPORT",4202);
        assertFalse(f.session.hasReplayRecording());
        assertThrows(IllegalStateException.class, () -> f.session.completedReplay(0));
        start(f,peiyi.tongchang);
        assertThrows(IllegalStateException.class, () -> f.session.completedReplay(f.session.currentEventCursor()));
        finish(f);
        long cursor=f.session.currentEventCursor();
        assertThrows(IllegalArgumentException.class, () -> f.session.completedReplay(cursor-1));
        assertThrows(IllegalArgumentException.class, () -> f.session.completedReplay(cursor+1));
        assertThrows(IllegalArgumentException.class, () -> f.session.completedReplay(-1));
        var before=f.session.currentUpdate();
        var all=f.session.currentUpdate(0);
        var expected=f.logic.getRecorder().getRecords();
        var recording=f.session.completedReplay(cursor);
        assertEquals(expected,recording.records());
        assertFalse(String.join("",recording.records()).contains("PRIVATE_SESSION_DO_NOT_EXPORT"));
        assertEquals(before,f.session.currentUpdate());
        assertEquals(all,f.session.currentUpdate(0));
        assertSame(recording,f.session.completedReplay(cursor));
        assertEquals(1,f.stats.getRecord().totalPlayCnt);
        f.session.close();
        assertThrows(IllegalStateException.class, () -> f.session.completedReplay(cursor));
        assertFalse(f.session.hasReplayRecording());
    }

    @Test void desktopStillPersistsPreStopRecordsThenAppliesExistingStopWithoutChangingRevisionOrEvents() {
        var f=fixture("desktop",4201); start(f,peiyi.jianyi);
        var before=f.logic.getRecorder().getRecords();
        var update=f.session.currentUpdate();
        var sent=new AtomicReference<ReplayRecording>();
        f.session.saveDesktopReplay(sent::set);
        assertEquals(before,sent.get().records());
        var after=f.logic.getRecorder().getRecords();
        assertEquals(before,after.subList(0,before.size()));
        assertEquals(before.size()+1,after.size());
        assertEquals("END|result=UNFINISHED|day=0",after.getLast());
        assertFalse(f.logic.getRecorder().isActive());
        assertEquals(update,f.session.currentUpdate());
        assertEquals(0,f.stats.getRecord().totalPlayCnt);
    }

    @Test void failingSinkDoesNotStopOrAppendAndRetryCanPersistTheIdenticalSnapshot() {
        var f=fixture("retry",4201); start(f,peiyi.jianyi);
        var before=f.logic.getRecorder().getRecords();
        var update=f.session.currentUpdate();
        assertThrows(IllegalStateException.class, () -> f.session.saveDesktopReplay(recording -> { throw new IllegalStateException("storage offline"); }));
        assertEquals(before,f.logic.getRecorder().getRecords());
        assertTrue(f.logic.getRecorder().isActive());
        assertEquals(update,f.session.currentUpdate());
        f.session.saveDesktopReplay(recording -> assertEquals(before,recording.records()));
        assertFalse(f.logic.getRecorder().isActive());
    }

    @Test void completedSnapshotRemainsIndependentOfLaterDesktopStopAndOtherSession() {
        var first=fixture("first",4201); var second=fixture("second",4202);
        start(first,peiyi.jianyi); start(second,peiyi.tongchang);
        finish(first); finish(second);
        var a=first.session.completedReplay(first.session.currentEventCursor());
        var b=second.session.completedReplay(second.session.currentEventCursor());
        assertNotEquals(a,b);
        var expected=List.copyOf(a.records());
        first.session.saveDesktopReplay(recording -> assertEquals(expected,recording.records()));
        assertEquals(expected,first.session.completedReplay(first.session.currentEventCursor()).records());
        assertEquals(b,second.session.completedReplay(second.session.currentEventCursor()));
    }
}
