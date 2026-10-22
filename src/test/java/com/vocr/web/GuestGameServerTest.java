package com.vocr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.*;
import com.vocr.config.PathConfig;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.SelectionType;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class GuestGameServerTest
{
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @ParameterizedTest
    @EnumSource(value = peiyi.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void httpAndDirectSessions_shouldProduceIdenticalCompleteGames(peiyi configuration)
            throws Exception
    {
        long seed = 4200 + configuration.ordinal();
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, seed), 10).start(0))
        {
            GameSession direct = SeededGuestSession.create("direct", seed);
            direct.startGame(new StartGameCommand(0, configuration));
            HttpResponse<String> created = request(server, "POST", "/api/v1/games",
                    Map.of("configuration", configuration.name()));
            assertEquals(201, created.statusCode(), created.body());
            JsonNode body = json.readTree(created.body());
            String path = "/api/v1/games/" + body.get("sessionId").asText();
            assertEquals(wireJson(direct.currentUpdate(0)), body.get("update"));
            GuestWireContract.assertCreated(body);

            int rounds = 0;
            int interruptions = 0;
            while (!direct.isFinished())
            {
                assertTrue(++rounds <= 80, "对局必须能结束");
                GameUpdate before = direct.currentUpdate();
                long cursor = before.nextEventCursor();
                int target = before.view().players().stream().filter(VisiblePlayer::alive)
                        .filter(player -> player.claimedRole() != Role.gong)
                        .findFirst().orElseThrow().playerNumber();
                var selection = new SetAllTargetSelectionCommand(
                        direct.currentRevision(), SelectionType.VOTE, target, true);
                GameUpdate selected = direct.setTargetSelection(selection);
                HttpResponse<String> selectedHttp = request(server, "PUT",
                        path + "/vote-target", new GuestGameServer.VoteTarget(
                                selection.expectedRevision(), target, true));
                assertEquals(200, selectedHttp.statusCode(), selectedHttp.body());
                assertEquals(wireJson(selected), json.readTree(selectedHttp.body()));
                GuestWireContract.assertUpdate(json.readTree(selectedHttp.body()));

                List<Integer> candidates = selected.legalVote(VotingMethod.DESIGNATED)
                        .orElseThrow().candidates();
                var command = new ExecuteVoteCommand(
                        direct.currentRevision(), VotingMethod.DESIGNATED, candidates);
                VoteExecutionResult result = direct.executeVote(command);
                HttpResponse<String> voted = request(server, "POST", path + "/votes", command);
                assertEquals(200, voted.statusCode(), voted.body());
                assertEquals(wireJson(result), json.readTree(voted.body()));
                GuestWireContract.assertVoteResult(json.readTree(voted.body()));
                if (!result.voteCompleted()) {
                    interruptions++;
                    assertEquals(BusinessPhase.DAY_DECISION, result.update().phase());
                    assertEquals(selected.view().gameDay(), result.update().view().gameDay());
                    assertTrue(result.update().presentedEvents().stream()
                            .anyMatch(event -> event.stage() == EventPresentationStage.AFTERNOON_DIALOGUE));
                }
                // 事件续读可重复；投票成功或被 CO 回避打断都必须保持同一窗口。
                JsonNode read = json.readTree(request(server, "GET",
                        path + "?eventCursor=" + cursor, null).body());
                assertEquals(wireJson(direct.currentUpdate(cursor)), read);
                assertEquals(read, json.readTree(request(server, "GET",
                        path + "?eventCursor=" + cursor, null).body()));
                GuestWireContract.assertUpdate(read);
            }
            assertEquals(BusinessPhase.FINISHED, direct.currentUpdate().phase());
            assertFalse(direct.currentUpdate().revealedRoles().isEmpty());
            if (configuration == peiyi.tongchang)
                assertTrue(interruptions > 0, "fixed seed 4202 must exercise CO interruption over HTTP");
            JsonNode finished = json.readTree(request(server, "GET", path, null).body());
            HttpResponse<String> finishedClaim = request(server, "POST", path + "/claims/by-role",
                    new AskClaimByRoleCommand(direct.currentRevision(), Role.zhan));
            assertEquals(409, finishedClaim.statusCode(), finishedClaim.body());
            assertEquals("INVALID_STATE", json.readTree(finishedClaim.body()).path("code").asText());
            assertEquals(finished, json.readTree(request(server, "GET", path, null).body()));
            HttpResponse<String> finishedAvoidance = request(server, "PUT", path + "/avoidance",
                    new SetAvoidanceCommand(direct.currentRevision(), false));
            assertEquals(409, finishedAvoidance.statusCode(), finishedAvoidance.body());
            assertEquals("INVALID_STATE", json.readTree(finishedAvoidance.body()).path("code").asText());
            assertEquals(finished, json.readTree(request(server, "GET", path, null).body()));
            HttpResponse<String> finishedPlayers = request(server, "POST", path + "/claims/by-players",
                    new AskClaimByPlayersCommand(direct.currentRevision(), List.of(1)));
            assertEquals(409, finishedPlayers.statusCode(), finishedPlayers.body());
            assertEquals("INVALID_STATE", json.readTree(finishedPlayers.body()).path("code").asText());
            assertEquals(finished, json.readTree(request(server, "GET", path, null).body()));
            assertEquals(409, request(server, "POST", path + "/votes",
                    new ExecuteVoteCommand(direct.currentRevision(),
                            VotingMethod.DESIGNATED, List.of(1))).statusCode());
        }
    }

    @ParameterizedTest
    @EnumSource(value = peiyi.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void roleClaims_shouldMatchDirectCommandsAndRecoverFromCursor(peiyi configuration) throws Exception
    {
        long seed = 4200 + configuration.ordinal();
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, seed), 3).start(0))
        {
            GameSession direct = SeededGuestSession.create("claims-direct", seed);
            direct.startGame(new StartGameCommand(0, configuration));
            String path = create(server, configuration.name());
            String other = create(server, configuration.name());
            JsonNode otherBefore = json.readTree(request(server, "GET", other, null).body());
            for (Role role : List.of(Role.zhan, Role.ling, Role.lie, Role.gong, Role.mao))
            {
                GameUpdate before = direct.currentUpdate(0);
                AskClaimByRoleCommand command = new AskClaimByRoleCommand(before.revision(), role);
                GameUpdate expected = direct.requestClaims(command);
                HttpResponse<String> response = request(server, "POST", path + "/claims/by-role", command);
                assertEquals(200, response.statusCode(), response.body());
                JsonNode actual = json.readTree(response.body());
                assertEquals(wireJson(expected), actual);
                GuestWireContract.assertUpdate(actual);
                assertEquals(before.revision() + 1, actual.path("revision").asLong());
                String cursorPath = path + "?eventCursor=" + before.nextEventCursor();
                assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
                assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
            }
            assertEquals(wireJson(direct.currentUpdate(0)),
                    json.readTree(request(server, "GET", path, null).body()));
            assertEquals(otherBefore, json.readTree(request(server, "GET", other, null).body()));
        }
    }

    @Test
    void roleClaims_shouldRejectMalformedInputsAndConcurrentOldRevisionsWithoutReplaying() throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 2).start(0))
        {
            String path = create(server, "tongchang");
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            for (String payload : List.of("{}", "{\"expectedRevision\":1}",
                    "{\"role\":\"zhan\"}", "{\"expectedRevision\":1,\"role\":null}",
                    "{\"expectedRevision\":null,\"role\":\"zhan\"}",
                    "{\"expectedRevision\":-1,\"role\":\"zhan\"}",
                    "{\"expectedRevision\":1.5,\"role\":\"zhan\"}",
                    "{\"expectedRevision\":1,\"role\":\"NONE\"}",
                    "{\"expectedRevision\":1,\"role\":\"wu\"}",
                    "{\"expectedRevision\":1,\"role\":\"unknown\"}",
                    "{\"expectedRevision\":1,\"role\":\"zhan\",\"players\":[1]}", "null", "{"))
            {
                HttpResponse<String> response = raw(server, "POST", path + "/claims/by-role", payload);
                assertEquals(400, response.statusCode(), payload + ": " + response.body());
                assertEquals("INVALID_REQUEST", json.readTree(response.body()).path("code").asText());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            assertEquals(404, request(server, "POST", "/api/v1/games/missing/claims/by-role",
                    new AskClaimByRoleCommand(1, Role.zhan)).statusCode());
            HttpResponse<String> stale = request(server, "POST", path + "/claims/by-role",
                    new AskClaimByRoleCommand(0, Role.zhan));
            assertEquals(409, stale.statusCode());
            assertEquals("STALE_REVISION", json.readTree(stale.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            var first = client.sendAsync(http(server, "POST", path + "/claims/by-role",
                    "{\"expectedRevision\":1,\"role\":\"lie\"}"), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "POST", path + "/claims/by-role",
                    "{\"expectedRevision\":1,\"role\":\"ling\"}"), HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(first.join().statusCode(), second.join().statusCode())
                    .stream().sorted().toList());
            JsonNode after = json.readTree(request(server, "GET", path, null).body());
            assertEquals(2, after.path("revision").asLong());
            GuestWireContract.assertUpdate(after);
        }
    }

    @ParameterizedTest
    @EnumSource(value = peiyi.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void playerClaims_shouldMatchOrderedDirectCommandsAndIsolateOtherSessions(peiyi configuration) throws Exception
    {
        long seed = 4200 + configuration.ordinal();
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, seed), 3).start(0))
        {
            GameSession direct = SeededGuestSession.create("players-direct", seed);
            direct.startGame(new StartGameCommand(0, configuration));
            String path = create(server, configuration.name());
            String other = create(server, configuration.name());
            JsonNode isolated = json.readTree(request(server, "GET", other, null).body());
            List<Integer> legal = direct.currentUpdate(0).claimInquiry().players();
            for (List<Integer> players : List.of(List.of(legal.getLast(), legal.getFirst()),
                    List.of(legal.getFirst())))
            {
                GameUpdate before = direct.currentUpdate(0);
                var command = new AskClaimByPlayersCommand(before.revision(), players);
                GameUpdate expected = direct.requestClaims(command);
                HttpResponse<String> response = request(server, "POST", path + "/claims/by-players", command);
                assertEquals(200, response.statusCode(), response.body());
                JsonNode actual = json.readTree(response.body());
                assertEquals(wireJson(expected), actual);
                GuestWireContract.assertUpdate(actual);
                assertEquals(before.revision() + 1, actual.path("revision").asLong());
                String cursorPath = path + "?eventCursor=" + before.nextEventCursor();
                assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
                assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
            }
            assertEquals(wireJson(direct.currentUpdate(0)), json.readTree(request(server, "GET", path, null).body()));
            assertEquals(isolated, json.readTree(request(server, "GET", other, null).body()));
        }
    }

    @Test
    void playerClaims_shouldRejectInvalidCandidatesAndAcceptOnlyOneConcurrentRevision() throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 2).start(0))
        {
            String path = create(server, "tongchang");
            String endpoint = path + "/claims/by-players";
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            int legal = before.path("claimInquiry").path("players").get(0).asInt();
            int dead = 0;
            for (JsonNode player : before.path("view").path("players"))
                if (!player.path("alive").asBoolean()) dead = player.path("playerNumber").asInt();
            assertTrue(dead > 0, "fixed seed must include an actually ineligible dead candidate");
            for (String payload : List.of("{}", "null", "{", "{\"players\":[1]}",
                    "{\"expectedRevision\":1}", "{\"expectedRevision\":1,\"players\":null}",
                    "{\"expectedRevision\":1,\"players\":[]}",
                    "{\"expectedRevision\":1,\"players\":[null]}",
                    "{\"expectedRevision\":null,\"players\":[1]}",
                    "{\"expectedRevision\":-1,\"players\":[1]}",
                    "{\"expectedRevision\":1.5,\"players\":[1]}",
                    "{\"expectedRevision\":1,\"players\":[1.5]}",
                    "{\"expectedRevision\":1,\"players\":[0]}",
                    "{\"expectedRevision\":1,\"players\":[-1]}",
                    "{\"expectedRevision\":1,\"players\":[99999]}",
                    "{\"expectedRevision\":1,\"players\":[" + dead + "]}",
                    "{\"expectedRevision\":1,\"players\":[" + legal + "," + legal + "]}",
                    "{\"expectedRevision\":1,\"players\":[" + legal + "],\"role\":\"zhan\"}"))
            {
                HttpResponse<String> response = raw(server, "POST", endpoint, payload);
                assertEquals(400, response.statusCode(), payload + ": " + response.body());
                assertEquals("INVALID_REQUEST", json.readTree(response.body()).path("code").asText());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            assertEquals(404, request(server, "POST", "/api/v1/games/missing/claims/by-players",
                    new AskClaimByPlayersCommand(1, List.of(legal))).statusCode());
            HttpResponse<String> stale = request(server, "POST", endpoint,
                    new AskClaimByPlayersCommand(0, List.of(legal)));
            assertEquals(409, stale.statusCode());
            assertEquals("STALE_REVISION", json.readTree(stale.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            String payload = json.writeValueAsString(new AskClaimByPlayersCommand(1, List.of(legal)));
            var first = client.sendAsync(http(server, "POST", endpoint, payload), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "POST", endpoint, payload), HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(first.join().statusCode(), second.join().statusCode())
                    .stream().sorted().toList());
            JsonNode after = json.readTree(request(server, "GET", path, null).body());
            assertEquals(2, after.path("revision").asLong());
            GuestWireContract.assertUpdate(after);
        }
    }

    @ParameterizedTest
    @EnumSource(value = peiyi.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void avoidance_shouldMatchDirectStateAcrossRepeatedSettingsAndIsolateGuests(peiyi configuration) throws Exception
    {
        long seed = 4200 + configuration.ordinal();
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, seed), 3).start(0))
        {
            GameSession direct = SeededGuestSession.create("avoidance-direct", seed);
            direct.startGame(new StartGameCommand(0, configuration));
            String path = create(server, configuration.name());
            String other = create(server, configuration.name());
            JsonNode otherBefore = json.readTree(request(server, "GET", other, null).body());
            assertTrue(otherBefore.path("selections").path("avoidanceEnabled").asBoolean());
            for (boolean enabled : new boolean[]{false, false, true})
            {
                GameUpdate before = direct.currentUpdate(0);
                var command = new SetAvoidanceCommand(before.revision(), enabled);
                GameUpdate expected = direct.setAvoidance(command);
                HttpResponse<String> response = request(server, "PUT", path + "/avoidance", command);
                assertEquals(200, response.statusCode(), response.body());
                JsonNode actual = json.readTree(response.body());
                assertEquals(wireJson(expected), actual);
                GuestWireContract.assertUpdate(actual);
                assertEquals(before.revision() + 1, actual.path("revision").asLong());
                assertEquals(enabled, actual.path("selections").path("avoidanceEnabled").asBoolean());
                assertTrue(actual.path("events").isEmpty(), "setting the flag must not invent dialogue");
                String cursorPath = path + "?eventCursor=" + before.nextEventCursor();
                assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
                assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
                assertEquals(otherBefore, json.readTree(request(server, "GET", other, null).body()));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true,9,false", "false,9,true", "false,3,false"})
    void avoidance_shouldControlRealVotingButPreserveSharedRoleException(
            boolean enabled, int target, boolean voteCompleted) throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 1).start(0))
        {
            GameSession direct = SeededGuestSession.create("avoidance-vote", 4202);
            direct.startGame(new StartGameCommand(0, peiyi.tongchang));
            String path = create(server, "tongchang");
            var setting = new SetAvoidanceCommand(direct.currentRevision(), enabled);
            assertEquals(wireJson(direct.setAvoidance(setting)),
                    json.readTree(request(server, "PUT", path + "/avoidance", setting).body()));
            var selection = new SetAllTargetSelectionCommand(direct.currentRevision(), SelectionType.VOTE, target, true);
            GameUpdate selected = direct.setTargetSelection(selection);
            assertEquals(wireJson(selected), json.readTree(request(server, "PUT", path + "/vote-target",
                    new GuestGameServer.VoteTarget(selection.expectedRevision(), target, true)).body()));
            var command = new ExecuteVoteCommand(direct.currentRevision(), VotingMethod.DESIGNATED,
                    selected.legalVote(VotingMethod.DESIGNATED).orElseThrow().candidates());
            VoteExecutionResult expected = direct.executeVote(command);
            HttpResponse<String> response = request(server, "POST", path + "/votes", command);
            assertEquals(200, response.statusCode(), response.body());
            JsonNode actual = json.readTree(response.body());
            assertEquals(wireJson(expected), actual);
            GuestWireContract.assertVoteResult(actual);
            assertEquals(voteCompleted, actual.path("voteCompleted").asBoolean());
            assertEquals(enabled, actual.path("update").path("selections").path("avoidanceEnabled").asBoolean());
            if (!voteCompleted) assertTrue(expected.update().presentedEvents().stream()
                    .anyMatch(event -> event.stage() == EventPresentationStage.AFTERNOON_DIALOGUE));
            if (!enabled && target == 3) assertTrue(actual.path("update").path("events").findValuesAsText("eventname")
                    .contains("hbg"), "shared-role avoidance is independent of the flag");
        }
    }

    @Test
    void avoidance_shouldRejectInvalidSettingsAndOnlyAcceptOneConcurrentRevision() throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 2).start(0))
        {
            String path = create(server, "tongchang");
            String endpoint = path + "/avoidance";
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            for (String payload : List.of("{}", "null", "{", "{\"enabled\":false}",
                    "{\"expectedRevision\":1}", "{\"expectedRevision\":1,\"enabled\":null}",
                    "{\"expectedRevision\":null,\"enabled\":false}",
                    "{\"expectedRevision\":-1,\"enabled\":false}",
                    "{\"expectedRevision\":1.5,\"enabled\":false}",
                    "{\"expectedRevision\":1,\"enabled\":{}}", "{\"expectedRevision\":1,\"enabled\":[]}",
                    "{\"expectedRevision\":1,\"enabled\":\"not-a-boolean\"}",
                    "{\"expectedRevision\":1,\"enabled\":false,\"extra\":true}",
                    "{\"expectedRevision\":1,\"enabled\":false} {}"))
            {
                HttpResponse<String> response = raw(server, "PUT", endpoint, payload);
                assertEquals(400, response.statusCode(), payload + ": " + response.body());
                assertEquals("INVALID_REQUEST", json.readTree(response.body()).path("code").asText());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            assertEquals(404, request(server, "PUT", "/api/v1/games/missing/avoidance",
                    new SetAvoidanceCommand(1, false)).statusCode());
            HttpResponse<String> stale = request(server, "PUT", endpoint, new SetAvoidanceCommand(0, false));
            assertEquals(409, stale.statusCode());
            assertEquals("STALE_REVISION", json.readTree(stale.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            String payload = json.writeValueAsString(new SetAvoidanceCommand(1, false));
            var first = client.sendAsync(http(server, "PUT", endpoint, payload), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "PUT", endpoint, payload), HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(first.join().statusCode(), second.join().statusCode())
                    .stream().sorted().toList());
            JsonNode after = json.readTree(request(server, "GET", path, null).body());
            assertEquals(2, after.path("revision").asLong());
            assertFalse(after.path("selections").path("avoidanceEnabled").asBoolean());
            GuestWireContract.assertUpdate(after);
        }
    }

    @Test
    void invalidRequestsAndConcurrentStaleWrites_shouldNotMutateState() throws Exception
    {
        try (GuestGameServer server = new GuestGameServer().start(0))
        {
            String path = create(server, "jianyi");
            assertEquals(404, request(server, "GET", "/api/v1/games/missing", null).statusCode());
            assertEquals(400, request(server, "GET", path + "?eventCursor=-1", null).statusCode());
            assertEquals(400, request(server, "GET", path + "?eventCursor=999999", null).statusCode());
            assertEquals(400, request(server, "POST", "/api/v1/games",
                    Map.of("configuration", "NONE")).statusCode());
            assertEquals(400, raw(server, "POST", "/api/v1/games", "{").statusCode());
            assertEquals(400, raw(server, "PUT", path + "/vote-target",
                    "{\"target\":1,\"selected\":true}").statusCode());
            assertEquals(400, raw(server, "PUT", path + "/vote-target",
                    "{\"expectedRevision\":1,\"target\":1.5,\"selected\":true}").statusCode());
            assertEquals(400, raw(server, "POST", "/api/v1/games",
                    "{\"configuration\":\"jianyi\"} {}").statusCode());
            assertEquals(413, raw(server, "POST", "/api/v1/games",
                    " ".repeat(16_385)).statusCode());
            JsonNode update = json.readTree(request(server, "GET", path, null).body());
            int target = 0;
            for (JsonNode player : update.path("view").path("players"))
                if (player.path("alive").asBoolean()) {
                    target = player.path("playerNumber").asInt();
                    break;
                }
            String payload = json.writeValueAsString(
                    new GuestGameServer.VoteTarget(1, target, true));
            var first = client.sendAsync(http(server, "PUT", path + "/vote-target", payload),
                    HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "PUT", path + "/vote-target", payload),
                    HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(
                    first.join().statusCode(), second.join().statusCode()).stream().sorted().toList());
            JsonNode stale = json.readTree((first.join().statusCode() == 409
                    ? first : second).join().body());
            assertEquals("STALE_REVISION", stale.path("code").asText());
            assertEquals(2, stale.path("currentRevision").asLong());
            assertEquals(2, json.readTree(request(server, "GET", path, null).body())
                    .path("revision").asLong());
        }
    }

    @Test
    void defaultGuests_shouldBeIsolatedAndNeverWriteDesktopRecords() throws Exception
    {
        Path records = Path.of(PathConfig.GAME_RECORD);
        byte[] before = Files.exists(records) ? Files.readAllBytes(records) : null;
        try (GuestGameServer server = new GuestGameServer().start(0))
        {
            String first = create(server, "jianyi");
            String second = create(server, "tongchang");
            assertNotEquals(first, second);
            assertEquals("jianyi", json.readTree(request(server, "GET", first, null).body())
                    .path("view").path("configuration").asText());
            assertEquals("tongchang", json.readTree(request(server, "GET", second, null).body())
                    .path("view").path("configuration").asText());
            JsonNode firstUpdate = json.readTree(request(server, "GET", first, null).body());
            int target = 0;
            for (JsonNode player : firstUpdate.path("view").path("players"))
                if (player.path("alive").asBoolean()) {
                    target = player.path("playerNumber").asInt();
                    break;
                }
            JsonNode secondBefore = json.readTree(request(server, "GET", second, null).body());
            assertEquals(200, request(server, "PUT", first + "/vote-target",
                    new GuestGameServer.VoteTarget(1, target, true)).statusCode());
            assertEquals(secondBefore, json.readTree(
                    request(server, "GET", second, null).body()));
            assertEquals(204, request(server, "DELETE", first, null).statusCode());
            assertEquals(404, request(server, "GET", first, null).statusCode());
            assertEquals(200, request(server, "GET", second, null).statusCode());
        }
        assertArrayEquals(before, Files.exists(records) ? Files.readAllBytes(records) : null);
    }

    @Test
    void capacity_shouldRejectCreationUntilAnExistingSessionIsReleased() throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                GuestGameServer::newGuestSession, 1).start(0))
        {
            String first = create(server, "jianyi");
            assertEquals(503, request(server, "POST", "/api/v1/games",
                    Map.of("configuration", "jianyi")).statusCode());
            assertEquals(204, request(server, "DELETE", first, null).statusCode());
            assertNotNull(create(server, "tongchang"));
        }
    }

    @Test
    void unknownRequestFields_shouldBeRejectedBeforeCreatingOrMutatingSession() throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(GuestGameServer::newGuestSession, 1).start(0))
        {
            HttpResponse<String> rejected = raw(server, "POST", "/api/v1/games",
                    "{\"configuration\":\"jianyi\",\"targe\":1}");
            assertEquals(400, rejected.statusCode(), rejected.body());
            assertEquals("INVALID_REQUEST", json.readTree(rejected.body()).path("code").asText());
            String path = create(server, "jianyi"); // invalid create did not consume capacity
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            int target = 0;
            for (JsonNode action : before.path("legalTargetSelections"))
                if (action.path("type").asText().equals("VOTE"))
                    target = action.path("targets").get(0).asInt();
            assertTrue(target > 0, "use an otherwise legal target to isolate unknown-field rejection");
            HttpResponse<String> write = raw(server, "PUT", path + "/vote-target",
                    "{\"expectedRevision\":1,\"target\":" + target
                            + ",\"selected\":true,\"actualRole\":\"lang\"}");
            assertEquals(400, write.statusCode(), write.body());
            assertEquals("INVALID_REQUEST", json.readTree(write.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
        }
    }

    @ParameterizedTest
    @EnumSource(value = peiyi.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void freeAndGreyVotes_shouldMatchDirectCompleteGames(peiyi configuration) throws Exception
    {
        long seed = 4200 + configuration.ordinal();
        for (VotingMethod preferred : List.of(VotingMethod.FREE, VotingMethod.GREY))
        {
            try (GuestGameServer server = new GuestGameServer(
                    id -> SeededGuestSession.create(id, seed), 2).start(0))
            {
                GameSession direct = SeededGuestSession.create("vote-direct", seed);
                direct.startGame(new StartGameCommand(0, configuration));
                String path = create(server, configuration.name());
                String other = create(server, configuration.name());
                JsonNode isolated = json.readTree(request(server, "GET", other, null).body());
                int turns = 0;
                int preferredVotes = 0;
                int unavailableGrey = 0;
                while (!direct.isFinished())
                {
                    assertTrue(++turns <= 80, "fixed-seed game must finish");
                    GameUpdate before = direct.currentUpdate();
                    LegalVoteAction action = before.legalVote(preferred)
                            .orElseGet(() -> before.legalVote(VotingMethod.FREE).orElseThrow());
                    if (action.method() == preferred) preferredVotes++;
                    if (before.legalVote(VotingMethod.GREY).isEmpty())
                    {
                        unavailableGrey++;
                        JsonNode unchanged = json.readTree(request(server, "GET", path, null).body());
                        assertEquals(400, request(server, "POST", path + "/votes",
                                new ExecuteVoteCommand(before.revision(), VotingMethod.GREY,
                                        action.candidates())).statusCode());
                        assertEquals(unchanged, json.readTree(request(server, "GET", path, null).body()));
                    }
                    ExecuteVoteCommand command = new ExecuteVoteCommand(before.revision(),
                            action.method(), action.candidates());
                    VoteExecutionResult expected = direct.executeVote(command);
                    HttpResponse<String> response = request(server, "POST", path + "/votes", command);
                    assertEquals(200, response.statusCode(), response.body());
                    JsonNode actual = json.readTree(response.body());
                    assertEquals(wireJson(expected), actual);
                    GuestWireContract.assertVoteResult(actual);
                    assertEquals(before.revision() + 1, expected.update().revision());
                    assertTrue(expected.voteCompleted());
                    assertEquals(action.method(), expected.update().latestVoteResult().orElseThrow().method());
                    String cursorPath = path + "?eventCursor=" + before.nextEventCursor();
                    assertEquals(actual.path("update"), json.readTree(request(server, "GET", cursorPath, null).body()));
                    assertEquals(actual.path("update"), json.readTree(request(server, "GET", cursorPath, null).body()));
                }
                assertTrue(preferredVotes > 0, "must really exercise " + preferred);
                if (configuration == peiyi.tongchang && preferred == VotingMethod.GREY)
                    assertTrue(unavailableGrey > 0, "seed 4202 must reject GREY after its candidates are exhausted");
                JsonNode finished = json.readTree(request(server, "GET", path, null).body());
                for (VotingMethod method : List.of(VotingMethod.FREE, VotingMethod.GREY))
                {
                    HttpResponse<String> rejected = request(server, "POST", path + "/votes",
                            new ExecuteVoteCommand(direct.currentRevision(), method, List.of(1)));
                    assertEquals(409, rejected.statusCode(), rejected.body());
                    assertEquals("INVALID_STATE", json.readTree(rejected.body()).path("code").asText());
                    assertEquals(finished, json.readTree(request(server, "GET", path, null).body()));
                }
                assertEquals(isolated, json.readTree(request(server, "GET", other, null).body()));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = VotingMethod.class, names = {"FREE", "GREY"})
    void alternativeVotes_shouldRejectInvalidCandidatesAndConcurrentOldRevision(VotingMethod method) throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 1).start(0))
        {
            String path = create(server, "tongchang");
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            JsonNode action = java.util.stream.StreamSupport.stream(before.path("legalVoteActions").spliterator(), false)
                    .filter(value -> value.path("method").asText().equals(method.name())).findFirst().orElseThrow();
            List<Integer> candidates = new java.util.ArrayList<>();
            action.path("candidates").forEach(value -> candidates.add(value.asInt()));
            assertTrue(candidates.size() > 1, "fixed fixture must distinguish order and subsets");
            long revision = before.path("revision").asLong();
            var command = new ExecuteVoteCommand(revision, method, candidates);
            String valid = json.writeValueAsString(command);
            var reversed = new java.util.ArrayList<>(candidates);
            java.util.Collections.reverse(reversed);
            for (List<Integer> invalid : List.of(reversed, candidates.subList(0, 1),
                    List.of(candidates.get(0), candidates.get(0)), List.of(999), List.of(0)))
            {
                var payload = json.createObjectNode().put("expectedRevision", revision).put("method", method.name());
                payload.set("candidates", json.valueToTree(invalid));
                HttpResponse<String> rejected = request(server, "POST", path + "/votes", payload);
                assertEquals(400, rejected.statusCode(), rejected.body());
                assertEquals("INVALID_REQUEST", json.readTree(rejected.body()).path("code").asText());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            for (String invalid : List.of(
                    valid.replace("\"candidates\":" + action.path("candidates"), "\"candidates\":[]"),
                    valid.replace("\"candidates\":" + action.path("candidates"), "\"candidates\":null"),
                    valid.replace("\"candidates\":" + action.path("candidates"), "\"candidates\":[null]"),
                    valid.replace("\"candidates\":" + action.path("candidates"), "\"candidates\":[1.5]"),
                    valid.replace("\"candidates\":" + action.path("candidates"), "\"candidates\":{}"),
                    valid.replace("\"expectedRevision\":" + revision + ",", ""),
                    valid.replace("\"expectedRevision\":" + revision, "\"expectedRevision\":null"),
                    valid.replace("\"expectedRevision\":" + revision, "\"expectedRevision\":1.5"),
                    valid.replace("\"" + method.name() + "\"", "\"UNKNOWN\""),
                    valid.replace("\"" + method.name() + "\"", "null"),
                    valid.substring(0, valid.length() - 1) + ",\"extra\":1}", valid + " {}", "{"))
            {
                assertNotEquals(valid, invalid, "negative payload must differ from valid command");
                HttpResponse<String> rejected = raw(server, "POST", path + "/votes", invalid);
                assertEquals(400, rejected.statusCode(), invalid + ": " + rejected.body());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            HttpResponse<String> stale = request(server, "POST", path + "/votes",
                    new ExecuteVoteCommand(revision - 1, method, candidates));
            assertEquals(409, stale.statusCode());
            assertEquals("STALE_REVISION", json.readTree(stale.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            var first = client.sendAsync(http(server, "POST", path + "/votes", valid), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "POST", path + "/votes", valid), HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(first.join().statusCode(), second.join().statusCode()).stream().sorted().toList());
            JsonNode after = json.readTree(request(server, "GET", path, null).body());
            assertEquals(revision + 1, after.path("revision").asLong());
            assertEquals(1, after.path("voteHistory").size());
            GuestWireContract.assertUpdate(after);
            int dead = java.util.stream.StreamSupport.stream(after.path("view").path("players").spliterator(), false)
                    .filter(player -> !player.path("alive").asBoolean()).findFirst().orElseThrow().path("playerNumber").asInt();
            assertEquals("DAY_DECISION", after.path("phase").asText(), "fixture must allow testing a dead candidate");
            assertEquals(400, request(server, "POST", path + "/votes",
                    new ExecuteVoteCommand(after.path("revision").asLong(), method, List.of(dead))).statusCode());
            assertEquals(after, json.readTree(request(server, "GET", path, null).body()));
        }
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> skillConfigurations()
    {
        return java.util.Arrays.stream(peiyi.values()).filter(configuration -> configuration != peiyi.NONE)
                .flatMap(configuration -> java.util.stream.Stream.of(SelectionType.DIVINATION, SelectionType.GUARD)
                        .map(type -> org.junit.jupiter.params.provider.Arguments.of(configuration, type)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("skillConfigurations")
    void allSkillSelections_shouldMatchDirectAcrossDaysAndCompleteGames(peiyi configuration, SelectionType type) throws Exception
    {
        long seed = 4200 + configuration.ordinal();
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, seed), 2).start(0))
        {
            GameSession direct = SeededGuestSession.create("divination-direct", seed);
            direct.startGame(new StartGameCommand(0, configuration));
            String path = create(server, configuration.name());
            String other = create(server, configuration.name());
            JsonNode isolated = json.readTree(request(server, "GET", other, null).body());
            int turns = 0;
            while (!direct.isFinished())
            {
                assertTrue(++turns <= 80, "fixed-seed scheduled game must finish");
                GameUpdate before = direct.currentUpdate();
                assertFalse(before.selections().skillSelections(type).hasSelections(), "new day must not carry old reservations");
                List<Integer> targets = before.legalTargetSelection(type).orElseThrow().targets();
                assertTrue(targets.size() >= 2);
                int first = targets.get(0), second = targets.get(1);
                SelectionType otherType = type == SelectionType.DIVINATION ? SelectionType.GUARD : SelectionType.DIVINATION;
                var otherSelection = new SetAllTargetSelectionCommand(direct.currentRevision(), otherType, first, true);
                GameUpdate primed = direct.setTargetSelection(otherSelection);
                HttpResponse<String> primedHttp = request(server, "PUT", path + "/target-selections", otherSelection);
                assertEquals(200, primedHttp.statusCode(), primedHttp.body());
                assertEquals(wireJson(primed), json.readTree(primedHttp.body()));
                GuestWireContract.assertUpdate(json.readTree(primedHttp.body()));
                for (var choice : List.of(Map.entry(first, true), Map.entry(first, true),
                        Map.entry(second, true), Map.entry(first, false), Map.entry(first, false)))
                {
                    GameUpdate prior = direct.currentUpdate();
                    var command = new SetAllTargetSelectionCommand(prior.revision(),
                            type, choice.getKey(), choice.getValue());
                    GameUpdate expected = direct.setTargetSelection(command);
                    HttpResponse<String> response = request(server, "PUT", path + "/target-selections", command);
                    assertEquals(200, response.statusCode(), response.body());
                    JsonNode actual = json.readTree(response.body());
                    assertEquals(wireJson(expected), actual);
                    GuestWireContract.assertUpdate(actual);
                    assertEquals(prior.revision() + 1, expected.revision());
                    assertTrue(expected.events().isEmpty(), "reservation must not invent dialogue");
                    assertEquals(prior.selections().voteTargets(), expected.selections().voteTargets());
                    assertEquals(prior.selections().skillSelections(otherType), expected.selections().skillSelections(otherType));
                    var selected = expected.selections().skillSelections(type);
                    assertEquals(choice.getValue(), selected.hiddenTargets().contains(choice.getKey()));
                    for (var actor : selected.claimedActors())
                        assertEquals(choice.getValue(), actor.targets().contains(choice.getKey()));
                    String cursorPath = path + "?eventCursor=" + prior.nextEventCursor();
                    assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
                    assertEquals(actual, json.readTree(request(server, "GET", cursorPath, null).body()));
                }
                assertEquals(List.of(second), direct.currentUpdate().selections().skillSelections(type).hiddenTargets());
                GameUpdate selected = direct.currentUpdate();
                var action = selected.legalVote(VotingMethod.FREE).orElseThrow();
                var vote = new ExecuteVoteCommand(selected.revision(), action.method(), action.candidates());
                VoteExecutionResult expected = direct.executeVote(vote);
                HttpResponse<String> response = request(server, "POST", path + "/votes", vote);
                assertEquals(200, response.statusCode(), response.body());
                assertEquals(wireJson(expected), json.readTree(response.body()));
                GuestWireContract.assertVoteResult(json.readTree(response.body()));
            }
            JsonNode finished = json.readTree(request(server, "GET", path, null).body());
            HttpResponse<String> rejected = request(server, "PUT", path + "/target-selections",
                    new SetAllTargetSelectionCommand(direct.currentRevision(), type, 1, true));
            assertEquals(409, rejected.statusCode(), rejected.body());
            assertEquals("INVALID_STATE", json.readTree(rejected.body()).path("code").asText());
            assertEquals(finished, json.readTree(request(server, "GET", path, null).body()));
            assertEquals(isolated, json.readTree(request(server, "GET", other, null).body()));
        }
    }

    @ParameterizedTest
    @EnumSource(value = SelectionType.class, names = {"DIVINATION", "GUARD"})
    void allSkillSelections_shouldRejectMalformedInputsAndConcurrentStaleWrites(SelectionType type) throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(
                id -> SeededGuestSession.create(id, 4202), 1).start(0))
        {
            String path = create(server, "tongchang");
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            long revision = before.path("revision").asLong();
            int target = before.path("legalTargetSelections").get(0).path("targets").get(0).asInt();
            var command = new SetAllTargetSelectionCommand(revision, type, target, true);
            String valid = json.writeValueAsString(command);
            List<String> invalid = new java.util.ArrayList<>();
            for (String field : List.of("expectedRevision", "type", "target", "selected"))
            {
                var missing = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(valid);
                missing.remove(field);
                invalid.add(missing.toString());
                var nil = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(valid);
                nil.putNull(field);
                invalid.add(nil.toString());
            }
            for (String invalidType : List.of("VOTE", "UNKNOWN"))
                invalid.add(valid.replace(type.name(), invalidType));
            invalid.add(valid.replace("\"target\":" + target, "\"target\":999"));
            invalid.add(valid.replace("\"target\":" + target, "\"target\":0"));
            invalid.add(valid.replace("\"target\":" + target, "\"target\":-1"));
            invalid.add(valid.replace("\"target\":" + target, "\"target\":1.5"));
            invalid.add(valid.replace("\"expectedRevision\":" + revision, "\"expectedRevision\":-1"));
            invalid.add(valid.replace("\"expectedRevision\":" + revision, "\"expectedRevision\":1.5"));
            invalid.add(valid.replace("\"selected\":true", "\"selected\":{}"));
            invalid.add(valid.substring(0, valid.length() - 1) + ",\"claimedPlayer\":2}");
            invalid.add(valid + " {}");
            invalid.add("{");
            for (String payload : invalid)
            {
                assertNotEquals(valid, payload);
                HttpResponse<String> response = raw(server, "PUT", path + "/target-selections", payload);
                assertEquals(400, response.statusCode(), payload + ": " + response.body());
                assertEquals("INVALID_REQUEST", json.readTree(response.body()).path("code").asText());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            assertEquals(404, request(server, "PUT", "/api/v1/games/missing/target-selections", command).statusCode());
            HttpResponse<String> stale = request(server, "PUT", path + "/target-selections",
                    new SetAllTargetSelectionCommand(revision - 1, type, target, true));
            assertEquals(409, stale.statusCode());
            assertEquals("STALE_REVISION", json.readTree(stale.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            var first = client.sendAsync(http(server, "PUT", path + "/target-selections", valid), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "PUT", path + "/target-selections", valid), HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(first.join().statusCode(), second.join().statusCode()).stream().sorted().toList());
            JsonNode selected = json.readTree(request(server, "GET", path, null).body());
            assertEquals(revision + 1, selected.path("revision").asLong());
            GuestWireContract.assertUpdate(selected);
            JsonNode free = java.util.stream.StreamSupport.stream(selected.path("legalVoteActions").spliterator(), false)
                    .filter(action -> action.path("method").asText().equals("FREE")).findFirst().orElseThrow();
            HttpResponse<String> voted = request(server, "POST", path + "/votes", Map.of(
                    "expectedRevision", selected.path("revision").asLong(), "method", "FREE", "candidates", free.path("candidates")));
            assertEquals(200, voted.statusCode(), voted.body());
            JsonNode after = json.readTree(request(server, "GET", path, null).body());
            assertEquals("DAY_DECISION", after.path("phase").asText());
            int dead = java.util.stream.StreamSupport.stream(after.path("view").path("players").spliterator(), false)
                    .filter(player -> !player.path("alive").asBoolean()).findFirst().orElseThrow().path("playerNumber").asInt();
            assertEquals(400, request(server, "PUT", path + "/target-selections",
                    new SetAllTargetSelectionCommand(after.path("revision").asLong(), type, dead, true)).statusCode());
            assertEquals(after, json.readTree(request(server, "GET", path, null).body()));
        }
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> claimedSkillConfigurations()
    {
        var baseline = java.util.Arrays.stream(peiyi.values()).filter(configuration -> configuration != peiyi.NONE)
                .flatMap(configuration -> java.util.stream.Stream.of(SelectionType.DIVINATION, SelectionType.GUARD)
                        .map(type -> org.junit.jupiter.params.provider.Arguments.of(configuration, type, 4200L + configuration.ordinal())));
        return java.util.stream.Stream.concat(baseline, java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(peiyi.yaoohu, SelectionType.GUARD, 4202L)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("claimedSkillConfigurations")
    void claimedSkillSelections_shouldMatchDirectAndComposeWithAllSelections(peiyi configuration, SelectionType type, long seed) throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(id -> SeededGuestSession.create(id, seed), 2).start(0))
        {
            GameSession direct = SeededGuestSession.create("claimed-direct", seed);
            direct.startGame(new StartGameCommand(0, configuration));
            String path = create(server, configuration.name());
            String other = create(server, configuration.name());
            JsonNode isolated = json.readTree(request(server, "GET", other, null).body());
            int turns = 0, individualWrites = 0;
            while (!direct.isFinished())
            {
                assertTrue(++turns <= 80);
                assertFalse(direct.currentUpdate().selections().skillSelections(type).hasSelections());
                var ask = new AskClaimByRoleCommand(direct.currentRevision(), type == SelectionType.DIVINATION ? Role.zhan : Role.lie);
                GameUpdate asked = direct.requestClaims(ask);
                var askedHttp = request(server, "POST", path + "/claims/by-role", ask);
                assertEquals(200, askedHttp.statusCode(), askedHttp.body());
                assertEquals(wireJson(asked), json.readTree(askedHttp.body()));
                GuestWireContract.assertUpdate(json.readTree(askedHttp.body()));
                var action = asked.legalTargetSelection(type).orElseThrow();
                if (!action.claimedActors().isEmpty())
                {
                    int actor = action.claimedActors().getFirst().playerNumber();
                    int first = action.targets().get(0), second = action.targets().get(1);
                    SelectionType otherType = type == SelectionType.DIVINATION ? SelectionType.GUARD : SelectionType.DIVINATION;
                    compareSkillWrite(server, path, direct, new SetAllTargetSelectionCommand(direct.currentRevision(), otherType, second, true));
                    compareSkillWrite(server, path, direct, new SetAllTargetSelectionCommand(direct.currentRevision(), type, first, true));
                    for (var choice : List.of(Map.entry(first, false), Map.entry(first, false),
                            Map.entry(second, true), Map.entry(second, true)))
                    {
                        GameUpdate prior = direct.currentUpdate();
                        var command = new SetClaimedRoleTargetSelectionCommand(prior.revision(), type, actor, choice.getKey(), choice.getValue());
                        GameUpdate actual = compareSkillWrite(server, path, direct, command);
                        individualWrites++;
                        var oldSelections = prior.selections().skillSelections(type);
                        var selections = actual.selections().skillSelections(type);
                        assertEquals(oldSelections.hiddenTargets(), selections.hiddenTargets());
                        assertEquals(prior.selections().skillSelections(otherType), actual.selections().skillSelections(otherType));
                        assertEquals(prior.selections().voteTargets(), actual.selections().voteTargets());
                        for (var selected : selections.claimedActors())
                            if (selected.playerNumber() == actor)
                                assertEquals(choice.getValue(), selected.targets().contains(choice.getKey()));
                            else
                                assertTrue(oldSelections.claimedActors().contains(selected), "individual write must not touch another actor");
                    }
                    var allSet = compareSkillWrite(server, path, direct,
                            new SetAllTargetSelectionCommand(direct.currentRevision(), type, second, true));
                    assertTrue(allSet.selections().skillSelections(type).hiddenTargets().contains(second));
                    assertTrue(allSet.selections().skillSelections(type).claimedActors().stream().allMatch(value -> value.targets().contains(second)));
                    compareSkillWrite(server, path, direct,
                            new SetClaimedRoleTargetSelectionCommand(direct.currentRevision(), type, actor, second, false));
                    var allClear = compareSkillWrite(server, path, direct,
                            new SetAllTargetSelectionCommand(direct.currentRevision(), type, second, false));
                    assertFalse(allClear.selections().skillSelections(type).isTargetSelected(second));
                    assertTrue(allClear.selections().skillSelections(type).hiddenTargets().contains(first), "other targets must survive all-target command");
                }
                else
                {
                    var rejected = request(server, "PUT", path + "/claimed-target-selections",
                            new SetClaimedRoleTargetSelectionCommand(direct.currentRevision(), type,
                                    action.targets().getFirst(), action.targets().getFirst(), true));
                    assertEquals(400, rejected.statusCode(), rejected.body());
                    assertEquals(wireJson(direct.currentUpdate(0)), json.readTree(request(server, "GET", path, null).body()));
                }
                var free = direct.currentUpdate().legalVote(VotingMethod.FREE).orElseThrow();
                var vote = new ExecuteVoteCommand(direct.currentRevision(), free.method(), free.candidates());
                var expected = direct.executeVote(vote);
                var response = request(server, "POST", path + "/votes", vote);
                assertEquals(200, response.statusCode(), response.body());
                assertEquals(wireJson(expected), json.readTree(response.body()));
                GuestWireContract.assertVoteResult(json.readTree(response.body()));
            }
            if (configuration == peiyi.yaoohu && type == SelectionType.GUARD && seed == 4203)
            {
                assertEquals(0, individualWrites, "baseline has no public guard; must test refusal rather than invent an actor");
                int hunter = direct.currentUpdate().revealedRoles().stream().filter(value -> value.actualRole() == Role.lie)
                        .findFirst().orElseThrow().playerNumber();
                var hunterView = direct.currentUpdate().view().players().stream().filter(value -> value.playerNumber() == hunter).findFirst().orElseThrow();
                assertFalse(hunterView.alive());
                assertEquals(1, hunterView.deathDay(), "baseline hunter dies before the first public request");
            }
            else assertTrue(individualWrites > 0, "fixed fixture must exercise public " + type + " actor");
            JsonNode finished = json.readTree(request(server, "GET", path, null).body());
            var rejected = request(server, "PUT", path + "/claimed-target-selections",
                    new SetClaimedRoleTargetSelectionCommand(direct.currentRevision(), type, 1, 1, true));
            assertEquals(409, rejected.statusCode(), rejected.body());
            assertEquals("INVALID_STATE", json.readTree(rejected.body()).path("code").asText());
            assertEquals(finished, json.readTree(request(server, "GET", path, null).body()));
            assertEquals(isolated, json.readTree(request(server, "GET", other, null).body()));
        }
    }

    private GameUpdate compareSkillWrite(GuestGameServer server, String path, GameSession direct, RevisionedCommand command) throws Exception
    {
        long cursor = direct.currentUpdate().nextEventCursor();
        GameUpdate expected;
        String endpoint;
        if (command instanceof SetClaimedRoleTargetSelectionCommand individual)
        {
            expected = direct.setTargetSelection(individual);
            endpoint = "/claimed-target-selections";
        }
        else
        {
            expected = direct.setTargetSelection((SetAllTargetSelectionCommand) command);
            endpoint = "/target-selections";
        }
        var response = request(server, "PUT", path + endpoint, command);
        assertEquals(200, response.statusCode(), response.body());
        JsonNode actual = json.readTree(response.body());
        assertEquals(wireJson(expected), actual);
        GuestWireContract.assertUpdate(actual);
        assertEquals(command.expectedRevision() + 1, expected.revision());
        assertTrue(expected.events().isEmpty());
        for (int repeat = 0; repeat < 2; repeat++)
            assertEquals(actual, json.readTree(request(server, "GET", path + "?eventCursor=" + cursor, null).body()));
        return expected;
    }

    @ParameterizedTest
    @EnumSource(value = SelectionType.class, names = {"DIVINATION", "GUARD"})
    void claimedSkillSelections_shouldRejectInvalidActorsPayloadsAndConcurrentOldRevision(SelectionType type) throws Exception
    {
        try (GuestGameServer server = new GuestGameServer(id -> SeededGuestSession.create(id, 4202), 1).start(0))
        {
            String path = create(server, "tongchang");
            JsonNode initial = json.readTree(request(server, "GET", path, null).body());
            var asked = request(server, "POST", path + "/claims/by-role", new AskClaimByRoleCommand(
                    initial.path("revision").asLong(), type == SelectionType.DIVINATION ? Role.zhan : Role.lie));
            assertEquals(200, asked.statusCode(), asked.body());
            JsonNode before = json.readTree(request(server, "GET", path, null).body());
            var action = java.util.stream.StreamSupport.stream(before.path("legalTargetSelections").spliterator(), false)
                    .filter(value -> value.path("type").asText().equals(type.name())).findFirst().orElseThrow();
            int actor = action.path("claimedActors").get(0).path("playerNumber").asInt();
            int target = action.path("targets").get(0).asInt();
            long revision = before.path("revision").asLong();
            String valid = json.writeValueAsString(new SetClaimedRoleTargetSelectionCommand(revision, type, actor, target, true));
            List<String> invalid = new java.util.ArrayList<>();
            for (String field : List.of("expectedRevision", "type", "claimedPlayer", "target", "selected"))
            {
                var missing = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(valid);
                missing.remove(field);
                invalid.add(missing.toString());
                var nil = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(valid);
                nil.putNull(field);
                invalid.add(nil.toString());
            }
            for (String field : List.of("claimedPlayer", "target"))
                for (String value : List.of("0", "-1", "999", "1.5"))
                    invalid.add(valid.replace("\"" + field + "\":" + (field.equals("target") ? target : actor), "\"" + field + "\":" + value));
            for (String value : List.of("VOTE", "UNKNOWN")) invalid.add(valid.replace(type.name(), value));
            for (String value : List.of("-1", "1.5")) invalid.add(valid.replace("\"expectedRevision\":" + revision, "\"expectedRevision\":" + value));
            invalid.add(valid.replace("\"selected\":true", "\"selected\":{}"));
            invalid.add(valid.substring(0, valid.length() - 1) + ",\"actualRole\":\"lang\"}");
            invalid.add(valid + " {}");
            invalid.add("{");
            int nonActor = java.util.stream.StreamSupport.stream(before.path("view").path("players").spliterator(), false)
                    .filter(value -> value.path("alive").asBoolean() && !value.path("claimedRole").asText().equals(type == SelectionType.DIVINATION ? "zhan" : "lie"))
                    .findFirst().orElseThrow().path("playerNumber").asInt();
            invalid.add(valid.replace("\"claimedPlayer\":" + actor, "\"claimedPlayer\":" + nonActor));
            for (String payload : invalid)
            {
                assertNotEquals(valid, payload);
                var rejected = raw(server, "PUT", path + "/claimed-target-selections", payload);
                assertEquals(400, rejected.statusCode(), payload + ": " + rejected.body());
                assertEquals("INVALID_REQUEST", json.readTree(rejected.body()).path("code").asText());
                assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            }
            var stale = request(server, "PUT", path + "/claimed-target-selections",
                    new SetClaimedRoleTargetSelectionCommand(revision - 1, type, actor, target, true));
            assertEquals(409, stale.statusCode());
            assertEquals("STALE_REVISION", json.readTree(stale.body()).path("code").asText());
            assertEquals(before, json.readTree(request(server, "GET", path, null).body()));
            assertEquals(404, raw(server, "PUT", "/api/v1/games/missing/claimed-target-selections", valid).statusCode());
            var first = client.sendAsync(http(server, "PUT", path + "/claimed-target-selections", valid), HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(http(server, "PUT", path + "/claimed-target-selections", valid), HttpResponse.BodyHandlers.ofString());
            CompletableFuture.allOf(first, second).join();
            assertEquals(List.of(200, 409), List.of(first.join().statusCode(), second.join().statusCode()).stream().sorted().toList());
            JsonNode after = json.readTree(request(server, "GET", path, null).body());
            assertEquals(revision + 1, after.path("revision").asLong());
            GuestWireContract.assertUpdate(after);
            var free = java.util.stream.StreamSupport.stream(after.path("legalVoteActions").spliterator(), false)
                    .filter(value -> value.path("method").asText().equals("FREE")).findFirst().orElseThrow();
            var voted = request(server, "POST", path + "/votes", Map.of("expectedRevision", after.path("revision").asLong(),
                    "method", "FREE", "candidates", free.path("candidates")));
            assertEquals(200, voted.statusCode(), voted.body());
            JsonNode nextDay = json.readTree(request(server, "GET", path, null).body());
            assertEquals("DAY_DECISION", nextDay.path("phase").asText());
            int dead = java.util.stream.StreamSupport.stream(nextDay.path("view").path("players").spliterator(), false)
                    .filter(value -> !value.path("alive").asBoolean()).findFirst().orElseThrow().path("playerNumber").asInt();
            var nextAction = java.util.stream.StreamSupport.stream(nextDay.path("legalTargetSelections").spliterator(), false)
                    .filter(value -> value.path("type").asText().equals(type.name())).findFirst().orElseThrow();
            assertFalse(nextAction.path("claimedActors").isEmpty(), "fixture must retain a living public actor for dead-target validation");
            int livingActor = nextAction.path("claimedActors").get(0).path("playerNumber").asInt();
            int livingTarget = nextAction.path("targets").get(0).asInt();
            for (var invalidCommand : List.of(
                    new SetClaimedRoleTargetSelectionCommand(nextDay.path("revision").asLong(), type, dead, livingTarget, true),
                    new SetClaimedRoleTargetSelectionCommand(nextDay.path("revision").asLong(), type, livingActor, dead, true)))
            {
                var rejected = request(server, "PUT", path + "/claimed-target-selections", invalidCommand);
                assertEquals(400, rejected.statusCode(), rejected.body());
                assertEquals(nextDay, json.readTree(request(server, "GET", path, null).body()));
            }
        }
    }

    private JsonNode wireJson(Object value) throws Exception
    {
        // 经相同 JSON 文本边界比较，避免内存 LongNode 与解析后 IntNode 类型差异。
        return json.readTree(json.writeValueAsString(value));
    }

    private String create(GuestGameServer server, String configuration) throws Exception
    {
        HttpResponse<String> created = request(server, "POST", "/api/v1/games",
                Map.of("configuration", configuration));
        assertEquals(201, created.statusCode(), created.body());
        return "/api/v1/games/" + json.readTree(created.body()).path("sessionId").asText();
    }

    private HttpResponse<String> request(GuestGameServer server, String method,
                                         String path, Object body) throws Exception
    {
        return raw(server, method, path, body == null ? null : json.writeValueAsString(body));
    }

    private HttpResponse<String> raw(GuestGameServer server, String method,
                                     String path, String body) throws Exception
    {
        return client.send(http(server, method, path, body), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest http(GuestGameServer server, String method, String path, String body)
    {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build();
    }
}
