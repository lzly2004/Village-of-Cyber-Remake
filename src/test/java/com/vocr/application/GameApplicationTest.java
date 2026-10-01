package com.vocr.application;

import com.vocr.core.MainLogic;
import com.vocr.core.GameRecorder;
import com.vocr.core.MainLogicInterface;
import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        StartGameCommand command = new StartGameCommand(0, peiyi.jianyi);
        StartedGame result = application.startGame(command);

        assertEquals(1, result.revision());
        assertEquals(peiyi.jianyi, result.configuration());
        assertEquals(logic.getGameContext().getPlayerSum(), result.playerCount());
        assertEquals(logic.getGameContext().getGameDay(), result.gameDay());
        assertTrue(result.playerCount() > 0);
        assertTrue(result.gameDay() >= 2);
        GameUpdate update = application.currentUpdate();
        assertEquals(result.revision(), update.revision());
        assertFalse(update.events().isEmpty(), "开局领域事件应进入应用层更新");
        assertEquals(0, update.eventCursor());
        assertEquals(update.events().size(), update.nextEventCursor());
        assertEquals(peiyi.jianyi, update.view().configuration());
        assertEquals(result.playerCount(), update.view().players().size());
        Event first = update.events().getFirst();
        assertEquals(first, application.pollNextEvent());
        assertEquals(first, update.events().getFirst(), "已发布的更新快照不得随消费进度变化");
        assertThrows(UnsupportedOperationException.class, () -> update.events().add(first));

        StaleGameRevisionException stale = assertThrows(
                StaleGameRevisionException.class,
                () -> application.startGame(command));
        assertEquals(0, stale.expectedRevision());
        assertEquals(1, stale.currentRevision());

        GameUpdate completeHistory = application.currentUpdate(0);
        assertEquals(update.events(), completeHistory.events());
        assertEquals(update.nextEventCursor(), completeHistory.nextEventCursor());
        assertTrue(application.currentUpdate(completeHistory.nextEventCursor()).events().isEmpty());
    }

    @Test
    void startCommand_shouldRejectMissingOrInvalidConfiguration()
    {
        assertThrows(IllegalArgumentException.class, () -> new StartGameCommand(-1, peiyi.jianyi));
        assertThrows(NullPointerException.class, () -> new StartGameCommand(0, null));
        assertThrows(IllegalArgumentException.class, () -> new StartGameCommand(0, peiyi.NONE));
    }

    @Test
    void eventCursor_shouldReadRepeatablyWithoutAffectingDesktopQueue()
    {
        GameEventBuffer events = new GameEventBuffer();
        GameApplication application = new GameApplication(new TrackingLogic(), events);
        Event first = new Event(EventName.zco7, CharacterEnglishName.Abel);
        Event second = new Event(EventName.lnco18, CharacterEnglishName.Erich);

        application.publishDiagnosticEvent(first);
        long cursorAfterFirst = application.currentUpdate(0).nextEventCursor();
        application.publishDiagnosticEvent(second);

        GameUpdate unread = application.currentUpdate(cursorAfterFirst);
        assertEquals(List.of(second), unread.events());
        assertEquals(cursorAfterFirst, unread.eventCursor());
        assertEquals(1, unread.nextEventCursor() - unread.eventCursor());
        assertEquals(unread, application.currentUpdate(cursorAfterFirst),
                "同一游标必须可重复读取，便于网络重试");
        assertEquals(first, application.pollNextEvent(), "游标读取不得消费桌面待展示事件");
        GameUpdate pendingSuffix = application.currentUpdate();
        assertEquals(List.of(second), pendingSuffix.events());
        assertEquals(cursorAfterFirst, pendingSuffix.eventCursor(),
                "桌面待展示事件必须携带自身窗口起点，不能冒充完整历史");
        application.clearPendingEvents();
        assertFalse(application.hasPendingEvents());
        assertEquals(List.of(first, second), application.currentUpdate(0).events(),
                "桌面确认或清空事件不得破坏网页续读历史");

        assertThrows(IllegalArgumentException.class, () -> application.currentUpdate(-1));
        assertThrows(IllegalArgumentException.class,
                () -> application.currentUpdate(unread.nextEventCursor() + 1));
    }

    @Test
    void commandResult_shouldContainOnlyEventsPublishedByThatCommand()
    {
        GameEventBuffer events = new GameEventBuffer();
        TrackingLogic logic = new TrackingLogic();
        GameApplication application = new GameApplication(logic, events);
        Event earlier = new Event(EventName.zco7, CharacterEnglishName.Abel);
        Event commandEvent = new Event(EventName.lnco18, CharacterEnglishName.Erich);
        application.publishDiagnosticEvent(earlier);
        logic.onAskRole = () -> events.publish(commandEvent);

        GameUpdate result = application.requestClaims(
                new AskClaimByRoleCommand(application.currentRevision(), Role.zhan));

        assertEquals(List.of(commandEvent), result.events());
        assertEquals(1, result.eventCursor());
        assertEquals(2, result.nextEventCursor());
        assertEquals(List.of(earlier, commandEvent), application.currentUpdate().events(),
                "命令返回窗口不能消费桌面待展示队列");
    }

    @Test
    void revision_shouldAdvanceForAcceptedWritesAndRejectStaleOrRepeatedCommands()
    {
        TrackingLogic logic = new TrackingLogic();
        GameEventBuffer events = new GameEventBuffer();
        GameApplication application = new GameApplication(logic, events);
        AskClaimByRoleCommand firstCommand = new AskClaimByRoleCommand(0, Role.zhan);

        assertEquals(0, application.currentRevision());
        assertEquals(0, application.currentUpdate().revision());
        GameUpdate firstUpdate = application.requestClaims(firstCommand);

        assertEquals(1, firstUpdate.revision());
        assertEquals(1, application.currentRevision());
        assertEquals(1, logic.askRoleCalls);

        application.currentUpdate();
        application.currentUpdate(0);
        application.publishDiagnosticEvent(
                new Event(EventName.zco7, CharacterEnglishName.Abel));
        application.pollNextEvent();
        application.clearPendingEvents();
        assertEquals(1, application.currentRevision(), "只读和展示队列操作不得推进状态版本");

        StaleGameRevisionException stale = assertThrows(
                StaleGameRevisionException.class,
                () -> application.requestClaims(firstCommand));
        assertEquals(0, stale.expectedRevision());
        assertEquals(1, stale.currentRevision());
        assertEquals(1, logic.askRoleCalls, "过期命令不得进入核心逻辑");

        assertThrows(IllegalArgumentException.class, () -> application.requestClaims(
                new AskClaimByPlayersCommand(application.currentRevision(), List.of(11))));
        assertEquals(1, application.currentRevision(), "校验失败的命令不得推进状态版本");

        GameUpdate secondUpdate = application.requestClaims(
                new AskClaimByPlayersCommand(application.currentRevision(), List.of(2, 4)));
        assertEquals(2, secondUpdate.revision());
    }

    @Test
    void playerView_shouldExposeOnlyExplicitlyWhitelistedPublicState()
    {
        TrackingLogic logic = new TrackingLogic();
        logic.actualRoles[2] = Role.renlang.ordinal();
        logic.actualRoles[4] = Role.zhan.ordinal();
        logic.claimedRoles[2] = Role.zhan.ordinal();
        logic.deadPlayers.add(4);
        GameApplication application = new GameApplication(logic, new GameEventBuffer());

        PlayerViewSnapshot view = application.currentUpdate(0).view();

        assertEquals(peiyi.jianyi, view.configuration());
        assertEquals(4, view.gameDay());
        assertEquals(9, view.aliveCount());
        assertEquals(1, view.deathCount());
        assertEquals(10, view.players().size());
        assertEquals(Role.zhan, view.players().get(1).claimedRole());
        assertFalse(view.players().get(3).alive());

        Set<String> snapshotFields = Arrays.stream(PlayerViewSnapshot.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "configuration", "gameDay", "aliveCount", "deathCount", "result", "players"),
                snapshotFields);
        Set<String> playerFields = Arrays.stream(VisiblePlayer.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "playerNumber", "character", "alive", "deathReason", "deathDay",
                "claimedRole", "claimedRoleOrder", "comingOutDay"), playerFields);
        assertFalse(playerFields.contains("actualRole"));
        assertFalse(playerFields.contains("suspicion"));
        assertFalse(playerFields.contains("nonHumanPlan"));
    }

    @Test
    void claimCommands_shouldDelegateThroughApplicationAndReturnCurrentUpdate()
    {
        TrackingLogic logic = new TrackingLogic();
        GameEventBuffer events = new GameEventBuffer();
        GameApplication application = new GameApplication(logic, events);

        GameUpdate roleUpdate = application.requestClaims(
                new AskClaimByRoleCommand(application.currentRevision(), Role.zhan));
        assertEquals(Role.zhan, logic.askedRole);
        assertEquals(application.currentUpdate(), roleUpdate);

        GameUpdate playersUpdate = application.requestClaims(
                new AskClaimByPlayersCommand(application.currentRevision(), List.of(2, 4)));
        assertEquals(List.of(2, 4), logic.askedPlayers);
        assertEquals(application.currentUpdate(), playersUpdate);
    }

    @Test
    void claimCommands_shouldRejectInvalidInputsAndDefensivelyCopyPlayers()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new AskClaimByRoleCommand(-1, Role.zhan));
        assertThrows(NullPointerException.class, () -> new AskClaimByRoleCommand(0, null));
        assertThrows(IllegalArgumentException.class, () -> new AskClaimByRoleCommand(0, Role.NONE));
        assertThrows(IllegalArgumentException.class,
                () -> new AskClaimByRoleCommand(0, Role.renlang));
        assertThrows(NullPointerException.class, () -> new AskClaimByPlayersCommand(0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AskClaimByPlayersCommand(0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AskClaimByPlayersCommand(0, List.of(0)));
        assertThrows(IllegalArgumentException.class,
                () -> new AskClaimByPlayersCommand(0, List.of(1, 1)));

        List<Integer> players = new ArrayList<>(List.of(1, 3));
        AskClaimByPlayersCommand command = new AskClaimByPlayersCommand(0, players);
        players.add(5);
        assertEquals(List.of(1, 3), command.players());
        assertThrows(UnsupportedOperationException.class, () -> command.players().add(5));

        TrackingLogic logic = new TrackingLogic();
        GameApplication application = new GameApplication(logic, new GameEventBuffer());
        assertThrows(IllegalArgumentException.class, () -> application.requestClaims(
                new AskClaimByPlayersCommand(application.currentRevision(), List.of(11))));
        logic.deadPlayers.add(4);
        assertThrows(IllegalArgumentException.class, () -> application.requestClaims(
                new AskClaimByPlayersCommand(application.currentRevision(), List.of(4))));
        assertNull(logic.askedPlayers);
    }

    @Test
    void executeVote_shouldTranslateMethodAndReturnCoreCompletionState()
    {
        TrackingLogic logic = new TrackingLogic();
        logic.voteCompleted = true;
        GameApplication application = new GameApplication(logic, new GameEventBuffer());

        List<Integer> candidates = new ArrayList<>(List.of(2, 4));
        logic.selectedVoteTargets.addAll(candidates);
        ExecuteVoteCommand command = new ExecuteVoteCommand(
                application.currentRevision(), VotingMethod.DESIGNATED, candidates, true);
        candidates.add(6);
        VoteExecutionResult result = application.executeVote(command);

        assertEquals(2, logic.votingMethod);
        assertEquals(List.of(2, 4), logic.voteCandidates);
        assertTrue(logic.avoidanceEnabled);
        assertTrue(result.voteCompleted());
        assertEquals(1, result.update().revision());
        assertEquals(application.currentUpdate(), result.update());
        assertThrows(UnsupportedOperationException.class, () -> command.candidates().add(6));

        logic.voteCompleted = false;
        VoteExecutionResult interrupted = application.executeVote(
                new ExecuteVoteCommand(
                        application.currentRevision(), VotingMethod.FREE,
                        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), true));
        assertFalse(interrupted.voteCompleted(), "回避 CO 时必须保留核心的未完成状态");
        assertEquals(2, interrupted.update().revision());
        assertEquals(0, logic.votingMethod);
    }

    @Test
    void gameUpdate_shouldPublishAndEnforceAuthoritativeVoteCandidates()
    {
        TrackingLogic logic = new TrackingLogic();
        logic.claimedRoles[2] = Role.zhan.ordinal();
        logic.skillTargets[2][1] = 13;
        logic.claimedRoles[4] = Role.wu.ordinal();
        logic.deadPlayers.add(10);
        logic.selectedVoteTargets.addAll(List.of(4, 6));
        GameApplication application = new GameApplication(logic, new GameEventBuffer());

        GameUpdate update = application.currentUpdate(0);

        assertEquals(BusinessPhase.DAY_DECISION, update.phase());
        assertTrue(update.revealedRoles().isEmpty(), "进行中的对局不能泄露真实职业");
        assertThrows(IllegalStateException.class,
                () -> SettlementRevealProjector.project(logic.context));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                update.legalVote(VotingMethod.FREE).orElseThrow().candidates());
        assertEquals(List.of(1, 4, 5, 6, 7, 8, 9),
                update.legalVote(VotingMethod.GREY).orElseThrow().candidates());
        assertEquals(List.of(4, 6),
                update.legalVote(VotingMethod.DESIGNATED).orElseThrow().candidates());
        assertEquals(List.of(Role.zhan, Role.ling, Role.lie, Role.gong, Role.mao),
                update.claimInquiry().roles());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                update.claimInquiry().players());
        LegalTargetSelection divination = update
                .legalTargetSelection(SelectionType.DIVINATION).orElseThrow();
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), divination.targets());
        assertEquals(List.of(new ClaimedRoleActor(2, 2)), divination.claimedActors());
        assertTrue(update.legalTargetSelection(SelectionType.GUARD)
                .orElseThrow().claimedActors().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> update.legalVoteActions().add(
                        new LegalVoteAction(VotingMethod.FREE, List.of(1))));
        assertThrows(UnsupportedOperationException.class,
                () -> update.claimInquiry().players().add(10));
        assertThrows(UnsupportedOperationException.class,
                () -> divination.targets().add(10));
        assertThrows(IllegalArgumentException.class, () -> application.executeVote(
                new ExecuteVoteCommand(
                        application.currentRevision(), VotingMethod.FREE,
                        List.of(1, 2), false)));
        assertNull(logic.voteCandidates);

        Arrays.fill(logic.actualRoles, Role.wu.ordinal());
        logic.actualRoles[2] = Role.renlang.ordinal();
        logic.endResult = GameResult.WOLF_WIN;
        GameUpdate finished = application.currentUpdate(0);
        assertEquals(BusinessPhase.FINISHED, finished.phase());
        assertTrue(finished.legalVoteActions().isEmpty());
        assertFalse(finished.claimInquiry().available());
        assertTrue(finished.legalTargetSelections().isEmpty());
        assertEquals(10, finished.revealedRoles().size());
        assertEquals(new RevealedPlayerRole(2, Role.renlang), finished.revealedRoles().get(1));
        assertThrows(UnsupportedOperationException.class,
                () -> finished.revealedRoles().add(new RevealedPlayerRole(11, Role.wu)));
        assertThrows(IllegalStateException.class, () -> application.executeVote(
                new ExecuteVoteCommand(
                        application.currentRevision(), VotingMethod.FREE,
                        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), false)));
        assertThrows(IllegalStateException.class, () -> application.requestClaims(
                new AskClaimByRoleCommand(application.currentRevision(), Role.zhan)));
        assertThrows(IllegalStateException.class, () -> application.setTargetSelection(
                new SetAllTargetSelectionCommand(
                        application.currentRevision(), SelectionType.VOTE, 1, true)));
    }

    @Test
    void executeVote_shouldRejectInvalidCandidatesBeforeCallingCore()
    {
        assertThrows(NullPointerException.class,
                () -> new ExecuteVoteCommand(0, null, List.of(1), false));
        assertThrows(NullPointerException.class,
                () -> new ExecuteVoteCommand(0, VotingMethod.FREE, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecuteVoteCommand(0, VotingMethod.FREE, List.of(), false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecuteVoteCommand(0, VotingMethod.FREE, List.of(0), false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecuteVoteCommand(0, VotingMethod.FREE, List.of(1, 1), false));

        TrackingLogic logic = new TrackingLogic();
        GameApplication application = new GameApplication(logic, new GameEventBuffer());
        assertThrows(IllegalArgumentException.class, () -> application.executeVote(
                new ExecuteVoteCommand(
                        application.currentRevision(), VotingMethod.FREE, List.of(11), false)));
        assertNull(logic.voteCandidates);

        logic.deadPlayers.add(4);
        assertThrows(IllegalArgumentException.class, () -> application.executeVote(
                new ExecuteVoteCommand(
                        application.currentRevision(), VotingMethod.DESIGNATED,
                        List.of(4), false)));
        assertNull(logic.voteCandidates);
    }

    @Test
    void allTargetSelection_shouldPreserveVoteAndSkillExpansionSemantics()
    {
        TrackingLogic logic = new TrackingLogic();
        GameApplication application = new GameApplication(logic, new GameEventBuffer());

        GameUpdate voteUpdate = application.setTargetSelection(
                new SetAllTargetSelectionCommand(
                        application.currentRevision(), SelectionType.VOTE, 6, true));
        assertEquals(List.of("setSelectedVoteTarget:[6, 4, true]"), logic.selectionWrites);
        assertEquals(1, voteUpdate.revision());
        assertEquals(application.currentUpdate(), voteUpdate);

        logic.selectionWrites.clear();
        logic.actualRoles[2] = Role.zhan.ordinal();
        logic.claimedRoles[3] = Role.zhan.ordinal();
        logic.claimedRoles[5] = Role.zhan.ordinal();
        logic.deadPlayers.add(5);
        GameUpdate divinationUpdate = application.setTargetSelection(
                new SetAllTargetSelectionCommand(
                        application.currentRevision(), SelectionType.DIVINATION, 6, true));
        assertEquals(2, divinationUpdate.revision());
        assertEquals(List.of(
                "setHiddenSeerScheduled:[6, 4, true]",
                "setClaimedRoleScheduled:[2, 6, 4, true]",
                "setClaimedRoleScheduled:[3, 6, 4, true]"), logic.selectionWrites);

        logic.selectionWrites.clear();
        logic.actualRoles[2] = Role.lie.ordinal();
        logic.claimedRoles[3] = Role.lie.ordinal();
        logic.claimedRoles[5] = Role.lie.ordinal();
        GameUpdate guardUpdate = application.setTargetSelection(
                new SetAllTargetSelectionCommand(
                        application.currentRevision(), SelectionType.GUARD, 6, false));
        assertEquals(3, guardUpdate.revision());
        assertEquals(List.of(
                "setHiddenHunterScheduled:[6, 4, false]",
                "setClaimedRoleScheduled:[2, 6, 4, false]",
                "setClaimedRoleScheduled:[3, 6, 4, false]"), logic.selectionWrites);
    }

    @Test
    void claimedRoleTargetSelection_shouldValidateActorAndTargetBeforeWriting()
    {
        TrackingLogic logic = new TrackingLogic();
        logic.claimedRoles[3] = Role.zhan.ordinal();
        GameApplication application = new GameApplication(logic, new GameEventBuffer());

        GameUpdate selected = application.setTargetSelection(
                new SetClaimedRoleTargetSelectionCommand(
                        application.currentRevision(), SelectionType.DIVINATION, 3, 6, true));
        assertEquals(1, selected.revision());
        assertEquals(List.of("setClaimedRoleScheduled:[3, 6, 4, true]"), logic.selectionWrites);

        assertThrows(NullPointerException.class,
                () -> new SetAllTargetSelectionCommand(0, null, 1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SetAllTargetSelectionCommand(0, SelectionType.VOTE, 0, true));
        assertThrows(NullPointerException.class,
                () -> new SetClaimedRoleTargetSelectionCommand(0, null, 1, 2, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SetClaimedRoleTargetSelectionCommand(
                        0, SelectionType.VOTE, 1, 2, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SetClaimedRoleTargetSelectionCommand(
                        0, SelectionType.GUARD, 0, 2, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SetClaimedRoleTargetSelectionCommand(
                        0, SelectionType.GUARD, 1, 0, true));

        assertThrows(IllegalArgumentException.class, () -> application.setTargetSelection(
                new SetAllTargetSelectionCommand(
                        application.currentRevision(), SelectionType.VOTE, 11, true)));
        assertThrows(IllegalArgumentException.class, () -> application.setTargetSelection(
                new SetClaimedRoleTargetSelectionCommand(
                        application.currentRevision(), SelectionType.DIVINATION,
                        11, 6, true)));
        assertThrows(IllegalArgumentException.class, () -> application.setTargetSelection(
                new SetClaimedRoleTargetSelectionCommand(
                        application.currentRevision(), SelectionType.DIVINATION,
                        3, 11, true)));

        logic.deadPlayers.add(6);
        assertThrows(IllegalArgumentException.class, () -> application.setTargetSelection(
                new SetClaimedRoleTargetSelectionCommand(
                        application.currentRevision(), SelectionType.DIVINATION,
                        3, 6, true)));
        assertEquals(1, application.currentRevision(), "失败的目标预约不得推进状态版本");
        logic.deadPlayers.clear();
        logic.claimedRoles[3] = Role.lie.ordinal();
        assertThrows(IllegalArgumentException.class, () -> application.setTargetSelection(
                new SetClaimedRoleTargetSelectionCommand(
                        application.currentRevision(), SelectionType.DIVINATION,
                        3, 6, true)));
    }

    private GameRecordManager records(String fileName)
    {
        return new GameRecordManager(tempDir.resolve(fileName).toString());
    }

    private static final class TrackingLogic implements MainLogicInterface
    {
        private final Set<Integer> deadPlayers = new HashSet<>();
        private final int[] actualRoles = new int[11];
        private final int[] claimedRoles = new int[11];
        private final int[][] skillTargets = new int[11][16];
        private final Set<Integer> selectedVoteTargets = new HashSet<>();
        private final List<String> selectionWrites = new ArrayList<>();
        private GameResult endResult = GameResult.NONE;
        private final GameContextView context = (GameContextView) Proxy.newProxyInstance(
                GameContextView.class.getClassLoader(),
                new Class<?>[]{GameContextView.class},
                (proxy, method, arguments) -> {
                    return switch (method.getName())
                    {
                        case "getPlayerSum" -> 10;
                        case "getGameDay" -> 4;
                        case "isAlive" -> !deadPlayers.contains((Integer) arguments[0]);
                        case "getAliveCounter" -> 10 - deadPlayers.size();
                        case "getDeathCounter" -> deadPlayers.size();
                        case "getPeiyi" -> peiyi.jianyi;
                        case "getEndResult" -> endResult;
                        case "getCharacterNumber" -> (Integer) arguments[0];
                        case "getDeathReason" -> deadPlayers.contains((Integer) arguments[0])
                                ? com.vocr.domain.whyDie.chuxing
                                : com.vocr.domain.whyDie.NONE;
                        case "getDeathDay" -> deadPlayers.contains((Integer) arguments[0]) ? 3 : 0;
                        case "getActualRole" -> actualRoles[(Integer) arguments[0]];
                        case "getClaimedRole" -> claimedRoles[(Integer) arguments[0]];
                        case "getSkillTarget" -> skillTargets[(Integer) arguments[0]][(Integer) arguments[1]];
                        case "isSelectedVoteTarget" -> selectedVoteTargets.contains((Integer) arguments[0]);
                        case "getClaimedRoleOrder" -> claimedRoles[(Integer) arguments[0]] == 0
                                ? 0 : (Integer) arguments[0];
                        case "getComingOutDay" -> claimedRoles[(Integer) arguments[0]] == 0 ? 0 : 2;
                        case "setSelectedVoteTarget" ->
                        {
                            int player = (Integer) arguments[0];
                            if ((Boolean) arguments[2]) selectedVoteTargets.add(player);
                            else selectedVoteTargets.remove(player);
                            selectionWrites.add(
                                    method.getName() + ":" + Arrays.toString(arguments));
                            yield null;
                        }
                        case "setClaimedRoleScheduled", "setHiddenSeerScheduled",
                                "setHiddenHunterScheduled" ->
                        {
                            selectionWrites.add(
                                    method.getName() + ":" + Arrays.toString(arguments));
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
        private Role askedRole;
        private List<Integer> askedPlayers;
        private int votingMethod = -1;
        private List<Integer> voteCandidates;
        private boolean avoidanceEnabled;
        private boolean voteCompleted;
        private int askRoleCalls;
        private Runnable onAskRole = () -> { };

        @Override public GameContextView getGameContext() { return context; }
        @Override public GameRecorder getRecorder() { return null; }
        @Override public void start(peiyi configuration) { }
        @Override public boolean shokei(int method, List<Integer> players, boolean avoid) {
            votingMethod = method;
            voteCandidates = List.copyOf(players);
            avoidanceEnabled = avoid;
            return voteCompleted;
        }
        @Override public void askCo(Role role) {
            askRoleCalls++;
            askedRole = role;
            onAskRole.run();
        }
        @Override public void askCo(List<Integer> players) { askedPlayers = List.copyOf(players); }
    }
}
