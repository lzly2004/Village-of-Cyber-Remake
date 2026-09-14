package com.vocr.application;

import com.vocr.core.MainLogic;
import com.vocr.core.GameRecorder;
import com.vocr.core.MainLogicInterface;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameApplicationTest
{
    @TempDir
    Path tempDir;

    @Test
    void startGame_shouldReturnSafeSummaryAndKeepEventsAvailableToCaller()
    {
        GameEventBuffer events = new GameEventBuffer();
        MainLogic logic = new MainLogic(events::publish, records("start-record.json"));
        GameApplication application = new GameApplication(logic, events);

        StartedGame result = application.startGame(new StartGameCommand(peiyi.jianyi));

        assertEquals(peiyi.jianyi, result.configuration());
        assertEquals(logic.getGameContext().getPlayerSum(), result.playerCount());
        assertEquals(logic.getGameContext().getGameDay(), result.gameDay());
        assertTrue(result.playerCount() > 0);
        assertTrue(result.gameDay() >= 2);
        GameUpdate update = application.currentUpdate();
        assertFalse(update.events().isEmpty(), "开局领域事件应进入应用层更新");
        Event first = update.events().getFirst();
        assertEquals(first, application.pollNextEvent());
        assertEquals(first, update.events().getFirst(), "已发布的更新快照不得随消费进度变化");
        assertThrows(UnsupportedOperationException.class, () -> update.events().add(first));
    }

    @Test
    void startCommand_shouldRejectMissingOrInvalidConfiguration()
    {
        assertThrows(NullPointerException.class, () -> new StartGameCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new StartGameCommand(peiyi.NONE));
    }

    @Test
    void claimCommands_shouldDelegateThroughApplicationAndReturnCurrentUpdate()
    {
        TrackingLogic logic = new TrackingLogic();
        GameEventBuffer events = new GameEventBuffer();
        GameApplication application = new GameApplication(logic, events);

        GameUpdate roleUpdate = application.requestClaims(new AskClaimByRoleCommand(Role.zhan));
        assertEquals(Role.zhan, logic.askedRole);
        assertEquals(application.currentUpdate(), roleUpdate);

        GameUpdate playersUpdate = application.requestClaims(
                new AskClaimByPlayersCommand(List.of(2, 4)));
        assertEquals(List.of(2, 4), logic.askedPlayers);
        assertEquals(application.currentUpdate(), playersUpdate);
    }

    @Test
    void claimCommands_shouldRejectInvalidInputsAndDefensivelyCopyPlayers()
    {
        assertThrows(NullPointerException.class, () -> new AskClaimByRoleCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new AskClaimByRoleCommand(Role.NONE));
        assertThrows(IllegalArgumentException.class, () -> new AskClaimByRoleCommand(Role.renlang));
        assertThrows(NullPointerException.class, () -> new AskClaimByPlayersCommand(null));
        assertThrows(IllegalArgumentException.class, () -> new AskClaimByPlayersCommand(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new AskClaimByPlayersCommand(List.of(0)));

        List<Integer> players = new ArrayList<>(List.of(1, 3));
        AskClaimByPlayersCommand command = new AskClaimByPlayersCommand(players);
        players.add(5);
        assertEquals(List.of(1, 3), command.players());
        assertThrows(UnsupportedOperationException.class, () -> command.players().add(5));

        GameApplication application = new GameApplication(new TrackingLogic(), new GameEventBuffer());
        assertThrows(IllegalArgumentException.class, () -> application.requestClaims(
                new AskClaimByPlayersCommand(List.of(11))));
    }

    @Test
    void executeVote_shouldTranslateMethodAndReturnCoreCompletionState()
    {
        TrackingLogic logic = new TrackingLogic();
        logic.voteCompleted = true;
        GameApplication application = new GameApplication(logic, new GameEventBuffer());

        List<Integer> candidates = new ArrayList<>(List.of(2, 4));
        ExecuteVoteCommand command = new ExecuteVoteCommand(
                VotingMethod.DESIGNATED, candidates, true);
        candidates.add(6);
        VoteExecutionResult result = application.executeVote(command);

        assertEquals(2, logic.votingMethod);
        assertEquals(List.of(2, 4), logic.voteCandidates);
        assertTrue(logic.avoidanceEnabled);
        assertTrue(result.voteCompleted());
        assertEquals(application.currentUpdate(), result.update());
        assertThrows(UnsupportedOperationException.class, () -> command.candidates().add(6));

        logic.voteCompleted = false;
        VoteExecutionResult interrupted = application.executeVote(
                new ExecuteVoteCommand(VotingMethod.FREE, List.of(1), true));
        assertFalse(interrupted.voteCompleted(), "回避 CO 时必须保留核心的未完成状态");
        assertEquals(0, logic.votingMethod);
    }

    @Test
    void executeVote_shouldRejectInvalidCandidatesBeforeCallingCore()
    {
        assertThrows(NullPointerException.class,
                () -> new ExecuteVoteCommand(null, List.of(1), false));
        assertThrows(NullPointerException.class,
                () -> new ExecuteVoteCommand(VotingMethod.FREE, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecuteVoteCommand(VotingMethod.FREE, List.of(), false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecuteVoteCommand(VotingMethod.FREE, List.of(0), false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecuteVoteCommand(VotingMethod.FREE, List.of(1, 1), false));

        TrackingLogic logic = new TrackingLogic();
        GameApplication application = new GameApplication(logic, new GameEventBuffer());
        assertThrows(IllegalArgumentException.class, () -> application.executeVote(
                new ExecuteVoteCommand(VotingMethod.FREE, List.of(11), false)));
        assertNull(logic.voteCandidates);

        logic.deadPlayers.add(4);
        assertThrows(IllegalArgumentException.class, () -> application.executeVote(
                new ExecuteVoteCommand(VotingMethod.DESIGNATED, List.of(4), false)));
        assertNull(logic.voteCandidates);
    }

    private GameRecordManager records(String fileName)
    {
        return new GameRecordManager(tempDir.resolve(fileName).toString());
    }

    private static final class TrackingLogic implements MainLogicInterface
    {
        private final Set<Integer> deadPlayers = new HashSet<>();
        private final GameContextView context = (GameContextView) Proxy.newProxyInstance(
                GameContextView.class.getClassLoader(),
                new Class<?>[]{GameContextView.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getPlayerSum")) return 10;
                    if (method.getName().equals("isAlive"))
                        return !deadPlayers.contains((Integer) arguments[0]);
                    throw new UnsupportedOperationException(method.getName());
                });
        private Role askedRole;
        private List<Integer> askedPlayers;
        private int votingMethod = -1;
        private List<Integer> voteCandidates;
        private boolean avoidanceEnabled;
        private boolean voteCompleted;

        @Override public GameContextView getGameContext() { return context; }
        @Override public GameRecorder getRecorder() { return null; }
        @Override public void start(peiyi configuration) { }
        @Override public boolean shokei(int method, List<Integer> players, boolean avoid) {
            votingMethod = method;
            voteCandidates = List.copyOf(players);
            avoidanceEnabled = avoid;
            return voteCompleted;
        }
        @Override public void askCo(Role role) { askedRole = role; }
        @Override public void askCo(List<Integer> players) { askedPlayers = List.copyOf(players); }
    }
}
