package com.vocr.application;

import com.vocr.core.MainLogic;
import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecord;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class GameSessionTest
{
    @TempDir
    Path tempDir;

    @Test
    void lifecycle_shouldStartOnceAndRejectReadsOrCommandsAfterClose()
    {
        GameEventBuffer events = new GameEventBuffer();
        GameSession session = new GameSession("lifecycle", logic("lifecycle", events), events);

        assertEquals(GameSession.State.NEW, session.state());
        assertThrows(IllegalStateException.class, session::currentUpdate);

        StartedGame started = session.startGame(new StartGameCommand(0, peiyi.jianyi));

        assertEquals(GameSession.State.RUNNING, session.state());
        assertEquals(1, started.revision());
        assertNotNull(session.contextView());
        assertTrue(session.currentEventCursor() > 0);

        assertThrows(IllegalStateException.class, () -> session.startGame(
                new StartGameCommand(session.currentRevision(), peiyi.tongchang)));

        session.close();

        assertEquals(GameSession.State.CLOSED, session.state());
        assertThrows(IllegalStateException.class, session::currentUpdate);
        assertThrows(IllegalStateException.class, () -> session.setAvoidance(
                new SetAvoidanceCommand(session.currentRevision(), false)));
    }

    @Test
    void eventCursor_shouldExposeRepeatableHistoryWithoutDrainingDesktopQueue()
    {
        GameEventBuffer events = new GameEventBuffer();
        GameSession session = new GameSession("cursor", logic("cursor", events), events);
        session.startGame(new StartGameCommand(0, peiyi.jianyi));

        GameUpdate history = session.currentUpdate(0);
        long cursor = history.nextEventCursor();
        assertEquals(cursor, session.currentEventCursor());
        assertEquals(history.events(), session.currentUpdate(0).events(),
                "同一游标续读必须得到相同历史");
        assertTrue(session.currentUpdate(cursor).events().isEmpty());

        int pendingBefore = session.currentUpdate().events().size();
        assertTrue(pendingBefore > 0);
        session.pollNextPresentedEvent();
        assertEquals(pendingBefore - 1, session.currentUpdate().events().size(),
                "桌面端消费只应缩短待展示后缀");
        assertEquals(history.events(), session.currentUpdate(0).events(),
                "桌面端消费不得破坏可续读历史");
    }

    @Test
    void commandsBeforeStart_shouldBeRejected()
    {
        GameEventBuffer events = new GameEventBuffer();
        GameSession session = new GameSession("not-started", logic("not-started", events), events);

        assertThrows(IllegalStateException.class, () -> session.setAvoidance(
                new SetAvoidanceCommand(0, false)));
        assertThrows(IllegalStateException.class, () -> session.executeVote(
                new ExecuteVoteCommand(0, VotingMethod.FREE, List.of(1))));
        assertThrows(IllegalStateException.class, () -> session.requestClaims(
                new AskClaimByRoleCommand(0, Role.zhan)));
    }

    @Test
    void twoSessions_shouldKeepRevisionsEventsAndRecordsIndependent() throws Exception
    {
        GameSession first = newSession("first", peiyi.jianyi);
        GameSession second = newSession("second", peiyi.tongchang);

        assertEquals(2, first.currentRevision());
        assertEquals(2, second.currentRevision());
        assertNotEquals(first.contextView(), second.contextView());
        assertTrue(first.currentEventCursor() > 0);
        assertTrue(second.currentEventCursor() > 0);

        advanceOneDay(first);

        assertTrue(first.currentRevision() > 2);
        assertEquals(2, second.currentRevision(), "另一局写命令不得推进本局版本");
        assertEquals(2, second.currentUpdate().revision());

        playToEnd(first);
        playToEnd(second);

        assertEquals(GameSession.State.FINISHED, first.state());
        assertEquals(GameSession.State.FINISHED, second.state());
        assertEquals(BusinessPhase.FINISHED, first.currentUpdate().phase());
        assertEquals(BusinessPhase.FINISHED, second.currentUpdate().phase());
        assertEquals(first.currentUpdate().view().players().size(),
                first.currentUpdate().revealedRoles().size());
        assertEquals(second.currentUpdate().view().players().size(),
                second.currentUpdate().revealedRoles().size());

        Path firstRecord = tempDir.resolve("first-record.json");
        Path secondRecord = tempDir.resolve("second-record.json");
        assertTrue(Files.exists(firstRecord) && Files.size(firstRecord) > 0);
        assertTrue(Files.exists(secondRecord) && Files.size(secondRecord) > 0);
        assertNotEquals(Files.readString(firstRecord), Files.readString(secondRecord),
                "不同配役的战绩文件必须各自独立");

        GameRecord firstStats = new GameRecordManager(firstRecord.toString()).getRecord();
        GameRecord secondStats = new GameRecordManager(secondRecord.toString()).getRecord();
        assertEquals(1, firstStats.totalPlayCnt);
        assertEquals(1, firstStats.playcnt[peiyi.jianyi.ordinal()]);
        assertEquals(0, firstStats.playcnt[peiyi.tongchang.ordinal()]);
        assertEquals(1, secondStats.totalPlayCnt);
        assertEquals(0, secondStats.playcnt[peiyi.jianyi.ordinal()]);
        assertEquals(1, secondStats.playcnt[peiyi.tongchang.ordinal()]);

        first.currentUpdate();
        first.currentUpdate(0);
        assertEquals(1, new GameRecordManager(firstRecord.toString()).getRecord().totalPlayCnt,
                "重复读取终局不得再次累计战绩");
    }

    private GameSession newSession(String label, peiyi configuration)
    {
        GameEventBuffer events = new GameEventBuffer();
        GameRecordManager records = new GameRecordManager(
                tempDir.resolve(label + "-record.json").toString());
        GameSession session = new GameSession(
                label, new MainLogic(events::publish, records), events, records);
        session.startGame(new StartGameCommand(0, configuration));
        session.setAvoidance(new SetAvoidanceCommand(session.currentRevision(), false));
        return session;
    }

    private MainLogic logic(String label, GameEventBuffer events)
    {
        GameRecordManager records = new GameRecordManager(
                tempDir.resolve(label + "-record.json").toString());
        return new MainLogic(events::publish, records);
    }

    private static void playToEnd(GameSession session)
    {
        int guard = 0;
        while (session.state() != GameSession.State.FINISHED)
        {
            if (++guard > 40)
                fail("会话未能在最大轮次内结束对局");
            advanceOneDay(session);
        }
    }

    private static void advanceOneDay(GameSession session)
    {
        GameContextView context = session.contextView();
        int target = selectExecutionTarget(context);
        session.setTargetSelection(new SetAllTargetSelectionCommand(
                session.currentRevision(), SelectionType.VOTE, target, true));
        VoteExecutionResult result = session.executeVote(new ExecuteVoteCommand(
                session.currentRevision(), VotingMethod.DESIGNATED, List.of(target)));
        assertTrue(result.voteCompleted(), "关闭回避后指定投票不应被中断");
    }

    private static int selectExecutionTarget(GameContextView context)
    {
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            if (context.isAlive(player) && context.getActualRole(player) != Role.gong.ordinal())
                return player;
        }
        for (int player = 1; player <= context.getPlayerSum(); player++)
            if (context.isAlive(player))
                return player;
        throw new IllegalStateException("没有可处刑的存活玩家");
    }
}
