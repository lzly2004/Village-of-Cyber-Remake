package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextVoteTest
{
    @TempDir
    Path tempDir;

    @Test
    void recordVoteRound_shouldStoreAllPlayersForEachRoundAndDay()
    {
        GameContext context = createContext();
        int[] firstRound = validTargets(context);
        firstRound[context.getPlayerSum()] = 0;

        context.recordVoteRound(1, 1, firstRound);
        firstRound[1] = 3;
        context.recordVoteRound(GameConstants.MAX_GAME_DAYS, GameConstants.MAX_VOTE_ROUNDS,
                validTargets(context));

        assertEquals(2, context.getVoteTarget(1, 1, 1));
        assertEquals(0, context.getVoteTarget(context.getPlayerSum(), 1, 1));
        assertEquals(2, context.getVoteTarget(1, GameConstants.MAX_GAME_DAYS,
                GameConstants.MAX_VOTE_ROUNDS));
    }

    @Test
    void completeVoting_shouldExposeOneImmutableHistorySnapshot()
    {
        GameContext context = createContext();
        int[] targets = validTargets(context);
        context.recordVoteRound(2, 1, targets);

        List<Integer> candidates = new ArrayList<>(List.of(2, 3, 4));
        context.completeVoting(2, 1, candidates, 1);
        candidates.clear();

        assertEquals(1, context.getVoteRoundCount(2));
        assertEquals(1, context.getVotingRule(2));
        assertIterableEquals(List.of(2, 3, 4), context.getVotingCandidates(2));
        assertThrows(UnsupportedOperationException.class,
                () -> context.getVotingCandidates(2).add(5));
        assertThrows(IllegalStateException.class,
                () -> context.recordVoteRound(2, 1, targets));
        assertThrows(IllegalStateException.class,
                () -> context.completeVoting(2, 1, List.of(2, 3), 1));
    }

    @Test
    void completeVoting_shouldValidateEverythingBeforePublishingHistory()
    {
        GameContext context = createContext();
        context.recordVoteRound(2, 1, validTargets(context));

        assertThrows(IllegalArgumentException.class,
                () -> context.completeVoting(2, -1, List.of(1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.completeVoting(2, 3, List.of(1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.completeVoting(2, 1, null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.completeVoting(2, 1, List.of(1, 1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.completeVoting(2, 1,
                        Arrays.asList(1, null), 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.completeVoting(2, 1,
                        List.of(context.getPlayerSum() + 1), 1));
        assertThrows(IllegalStateException.class,
                () -> context.completeVoting(2, 1, List.of(1), 2));

        assertEquals(0, context.getVoteRoundCount(2));
        assertEquals(-1, context.getVotingRule(2));
        assertTrue(context.getVotingCandidates(2).isEmpty());
    }

    @Test
    void recorder_shouldSerializeVoteStateWithoutUiHistoryCopies()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        GameContext context = new GameContext(status);
        int[] targets = validTargets(context);
        context.recordVoteRound(2, 1, targets);
        context.setSelectedVoteTarget(2, 2, true);
        context.setSelectedVoteTarget(3, 2, true);
        context.completeVoting(2, 2, List.of(2, 3), 1);

        GameRecordManager recordManager = new GameRecordManager(
                tempDir.resolve("game-record.json").toString());
        GameRecorder recorder = new GameRecorder("vote-state-test", recordManager);
        recorder.startGame(peiyi.jianyi, 42L, context.getPlayerSum(), status);
        recorder.recordVoteData(2, context);

        String expectedMethod = "VOTE_METHOD|day=2|rule=2|grey=2,3|design="
                + context.getCharacterNumber(2) + "," + context.getCharacterNumber(3);
        StringBuilder expectedDetail = new StringBuilder("VOTE_DETAIL|day=2|round=1|");
        for (int player = 1; player <= context.getPlayerSum(); player++)
            expectedDetail.append(player).append("->").append(targets[player]).append(",");
        expectedDetail.setLength(expectedDetail.length() - 1);

        List<String> voteRecords = recorder.getRecords().stream()
                .filter(line -> line.startsWith("VOTE_"))
                .toList();
        assertIterableEquals(List.of(expectedMethod, expectedDetail.toString()), voteRecords);
    }

    @Test
    void recordVoteRound_shouldRejectInvalidCoordinatesAndShape()
    {
        GameContext context = createContext();
        int[] targets = validTargets(context);

        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(0, 1, targets));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(GameConstants.MAX_GAME_DAYS + 1, 1, targets));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(1, 0, targets));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(1, GameConstants.MAX_VOTE_ROUNDS + 1, targets));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(1, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(1, 1, new int[context.getPlayerSum()]));
        assertThrows(IllegalArgumentException.class,
                () -> context.getVoteTarget(0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.getVoteTarget(1, 1, GameConstants.MAX_VOTE_ROUNDS + 1));
    }

    @Test
    void recordVoteRound_shouldValidateBeforeReplacingAnyExistingVote()
    {
        GameContext context = createContext();
        context.recordVoteRound(1, 1, validTargets(context));

        int[] invalidTargets = validTargets(context);
        invalidTargets[1] = 3;
        invalidTargets[2] = context.getPlayerSum() + 1;

        assertThrows(IllegalArgumentException.class,
                () -> context.recordVoteRound(1, 1, invalidTargets));
        assertEquals(2, context.getVoteTarget(1, 1, 1));
        assertEquals(1, context.getVoteTarget(2, 1, 1));
    }

    @Test
    void selectedVoteTargets_shouldBeIndependentByPlayerAndDayAndToggleable()
    {
        GameContext context = createContext();

        context.setSelectedVoteTarget(1, 1, true);
        context.setSelectedVoteTarget(2, 2, true);

        assertTrue(context.isSelectedVoteTarget(1, 1));
        assertFalse(context.isSelectedVoteTarget(1, 2));
        assertTrue(context.isSelectedVoteTarget(2, 2));

        context.setSelectedVoteTarget(1, 1, false);
        assertFalse(context.isSelectedVoteTarget(1, 1));
    }

    @Test
    void selectedVoteTargets_shouldRejectInvalidCoordinatesWithoutMutation()
    {
        GameContext context = createContext();
        context.setSelectedVoteTarget(1, 1, true);

        assertThrows(IllegalArgumentException.class,
                () -> context.setSelectedVoteTarget(0, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> context.setSelectedVoteTarget(context.getPlayerSum() + 1, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> context.setSelectedVoteTarget(1, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> context.setSelectedVoteTarget(1, GameConstants.MAX_GAME_DAYS + 1, false));

        assertTrue(context.isSelectedVoteTarget(1, 1));
    }

    private static int[] validTargets(GameContext context)
    {
        int[] targets = new int[context.getPlayerSum() + 1];
        for (int player = 1; player <= context.getPlayerSum(); player++)
            targets[player] = player == 1 ? 2 : 1;
        return targets;
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status);
    }
}
