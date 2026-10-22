package com.vocr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.*;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Generates ephemeral, source-fingerprinted application/HTTP oracles, NOT new golden snapshots. */
class Web017JourneyTest
{
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path OUTPUT = Path.of("build/web017");
    private static boolean previousLogging;

    @BeforeAll static void muteLocalDiagnostics() {
        previousLogging = com.vocr.logging.DebugLogger.getInstance().isEnabled();
        com.vocr.logging.DebugLogger.getInstance().setEnabled(false);
    }

    @AfterAll static void restoreLocalDiagnostics() {
        com.vocr.logging.DebugLogger.getInstance().setEnabled(previousLogging);
    }

    @ParameterizedTest
    @EnumSource(value = peiyi.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void completeJourneysMatchAcrossApplicationAndHttp(peiyi configuration) throws Exception
    {
        for (String strategy : List.of("designated", "mixed"))
            try (Journey journey = new Journey(configuration, strategy))
            {
                if (strategy.equals("mixed")) journey.exerciseControls();
                int turns = 0;
                while (!journey.direct.isFinished()) {
                    assertTrue(turns < 80, "bounded test policy must reach settlement");
                    GameUpdate update = journey.direct.currentUpdate();
                    VotingMethod method = strategy.equals("designated") ? VotingMethod.DESIGNATED
                            : VotingMethod.values()[turns % VotingMethod.values().length];
                    if (method != VotingMethod.DESIGNATED && update.legalVote(method).isEmpty())
                        method = VotingMethod.FREE;
                    if (method == VotingMethod.DESIGNATED) journey.selectDesignated();
                    journey.vote(method);
                    turns++;
                }
                journey.save();
            }
    }

    @Test
    void fixedSeedMustActuallyInterruptAndPreserveCompletedHistory() throws Exception
    {
        try (Journey journey = new Journey(peiyi.tongchang, "interruption")) {
            journey.step("PUT", "/vote-target", new SetAllTargetSelectionCommand(
                    journey.revision(), SelectionType.VOTE, 9, true));
            GameUpdate before = journey.direct.currentUpdate();
            JsonNode result = journey.vote(VotingMethod.DESIGNATED);
            assertFalse(result.path("voteCompleted").asBoolean());
            assertEquals(before.voteHistory(), journey.direct.currentUpdate().voteHistory());
            assertEquals(before.view().gameDay(), journey.direct.currentUpdate().view().gameDay());
            assertTrue(journey.direct.currentUpdate().presentedEvents().stream()
                    .anyMatch(event -> event.stage() == EventPresentationStage.AFTERNOON_DIALOGUE));
            journey.save();
        }
    }

    static String sourceFingerprint() throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String directory : List.of("src/main/java", "src/test/java", "src/web/java"))
            try (var paths = Files.walk(Path.of(directory))) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                    digest.update(path.toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) 0);
                    digest.update(Files.readAllBytes(path));
                }
            }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static final class Journey implements AutoCloseable
    {
        final GameSession direct;
        final GuestGameServer server;
        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        final List<Map<String, Object>> steps = new ArrayList<>();
        final peiyi configuration;
        final String strategy, path;
        final JsonNode initial;
        final Path output;

        Journey(peiyi configuration, String strategy) throws Exception {
            this.configuration = configuration;
            this.strategy = strategy;
            Files.createDirectories(OUTPUT);
            output = OUTPUT.resolve(configuration.name() + "-" + strategy + ".json");
            Files.deleteIfExists(output); // Own exact disposable output; never a tracked golden file.
            direct = SeededGuestSession.create("oracle", 4200 + configuration.ordinal());
            direct.startGame(new StartGameCommand(0, configuration));
            server = new GuestGameServer(SeededGuestSession::createReleaseBaseline, 2).start(0);
            JsonNode created = request("POST", "/api/v1/games", Map.of("configuration", configuration.name()));
            GuestWireContract.assertCreated(created);
            initial = wire(direct.currentUpdate(0));
            assertEquals(initial, created.path("update"));
            path = "/api/v1/games/" + created.path("sessionId").asText();
        }

        long revision() { return direct.currentRevision(); }

        void exerciseControls() throws Exception {
            List<Integer> players = direct.currentUpdate().claimInquiry().players();
            step("POST", "/claims/by-players", new AskClaimByPlayersCommand(revision(),
                    List.of(players.getLast(), players.getFirst())));
            for (Role role : List.of(Role.zhan, Role.ling, Role.lie, Role.gong, Role.mao))
                step("POST", "/claims/by-role", new AskClaimByRoleCommand(revision(), role));
            for (boolean enabled : List.of(false, true))
                step("PUT", "/avoidance", new SetAvoidanceCommand(revision(), enabled));
            for (SelectionType type : List.of(SelectionType.DIVINATION, SelectionType.GUARD)) {
                LegalTargetSelection action = direct.currentUpdate().legalTargetSelections().stream()
                        .filter(value -> value.type() == type).findFirst().orElseThrow();
                if (configuration == peiyi.yaoohu && type == SelectionType.GUARD)
                    assertTrue(action.claimedActors().isEmpty(), "4203 guard-actor absence is an explicit fixture premise");
                else
                    assertFalse(action.claimedActors().isEmpty(), "must actually exercise individual skill controls");
                int target = action.targets().getFirst();
                for (boolean selected : List.of(true, false, true))
                    step("PUT", "/target-selections", new SetAllTargetSelectionCommand(revision(), type, target, selected));
                if (!action.claimedActors().isEmpty()) {
                    int actor = action.claimedActors().getFirst().playerNumber();
                    for (boolean selected : List.of(false, true))
                        step("PUT", "/claimed-target-selections", new SetClaimedRoleTargetSelectionCommand(
                                revision(), type, actor, target, selected));
                }
                step("PUT", "/target-selections", new SetAllTargetSelectionCommand(revision(), type, target, false));
            }
        }

        void selectDesignated() throws Exception {
            GameUpdate before = direct.currentUpdate();
            int target = before.view().players().stream().filter(VisiblePlayer::alive)
                    .filter(player -> player.claimedRole() != Role.gong).findFirst().orElseThrow().playerNumber();
            for (int previous : before.selections().voteTargets())
                if (previous != target) step("PUT", "/vote-target", new SetAllTargetSelectionCommand(
                        revision(), SelectionType.VOTE, previous, false));
            if (!direct.currentUpdate().selections().voteTargets().contains(target))
                step("PUT", "/vote-target", new SetAllTargetSelectionCommand(revision(), SelectionType.VOTE, target, true));
        }

        JsonNode vote(VotingMethod method) throws Exception {
            return step("POST", "/votes", new ExecuteVoteCommand(revision(), method,
                    direct.currentUpdate().legalVote(method).orElseThrow().candidates()));
        }

        JsonNode step(String method, String suffix, Object command) throws Exception {
            Object result = switch (command) {
                case AskClaimByRoleCommand c -> direct.requestClaims(c);
                case AskClaimByPlayersCommand c -> direct.requestClaims(c);
                case SetAvoidanceCommand c -> direct.setAvoidance(c);
                case SetAllTargetSelectionCommand c -> direct.setTargetSelection(c);
                case SetClaimedRoleTargetSelectionCommand c -> direct.setTargetSelection(c);
                case ExecuteVoteCommand c -> direct.executeVote(c);
                default -> throw new AssertionError("unknown policy command");
            };
            Object payload = suffix.equals("/vote-target")
                    ? new GuestGameServer.VoteTarget(((SetAllTargetSelectionCommand) command).expectedRevision(),
                            ((SetAllTargetSelectionCommand) command).target(), ((SetAllTargetSelectionCommand) command).selected())
                    : command;
            JsonNode expected = wire(result);
            JsonNode actual = request(method, path + suffix, payload);
            assertEquals(expected, actual, configuration + " " + strategy + " " + suffix);
            if (result instanceof VoteExecutionResult) GuestWireContract.assertVoteResult(actual);
            else GuestWireContract.assertUpdate(actual);
            JsonNode full = wire(direct.currentUpdate(0));
            assertEquals(full, request("GET", path, null));
            GuestWireContract.assertUpdate(full);
            for (var vote : direct.currentUpdate().voteHistory())
                for (int index = 0; index < vote.rounds().size(); index++) {
                    assertEquals(3, com.vocr.config.GameConstants.MAX_VOTE_ROUNDS,
                            "if round limit changes, re-audit desktop's explicit 1/2/3 pagination");
                    assertTrue(vote.rounds().size() <= com.vocr.config.GameConstants.MAX_VOTE_ROUNDS);
                    assertEquals(index + 1, vote.rounds().get(index).round());
                    assertEquals(index < vote.rounds().size() - 1, vote.rounds().get(index).leaders().size() > 1,
                            "nonfinal round tied; final round unique: frontend label contract");
                }
            steps.add(Map.of("method", method, "path", suffix, "body", wire(payload),
                    "response", expected, "full", full));
            return actual;
        }

        JsonNode request(String method, String suffix, Object body) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + suffix))
                    .timeout(Duration.ofSeconds(10));
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .header("Content-Type", "application/json");
            var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(suffix.equals("/api/v1/games") ? 201 : 200, response.statusCode(), response.body());
            return JSON.readTree(response.body());
        }

        void save() throws Exception {
            if (!strategy.equals("interruption")) {
                assertTrue(direct.isFinished());
                Set<VotingMethod> methods = new HashSet<>();
                direct.currentUpdate().voteHistory().forEach(vote -> methods.add(vote.method()));
                assertEquals(strategy.equals("mixed") ? EnumSet.allOf(VotingMethod.class)
                        : EnumSet.of(VotingMethod.DESIGNATED), methods, "policy must really execute every promised method");
            }
            JSON.writeValue(output.toFile(), Map.of("schema", 1, "sourceFingerprint", sourceFingerprint(),
                    "configuration", configuration.name(), "strategy", strategy,
                    "seed", 4200 + configuration.ordinal(), "initial", initial, "steps", steps));
            System.out.println("WEB017 " + configuration + " " + strategy + " steps=" + steps.size()
                    + " result=" + direct.currentUpdate().view().result());
        }

        @Override public void close() { direct.close(); server.close(); client.close(); }
    }

    private static JsonNode wire(Object value) throws Exception {
        return JSON.readTree(JSON.writeValueAsString(value));
    }
}
