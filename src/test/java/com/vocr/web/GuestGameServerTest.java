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
            assertEquals(409, request(server, "POST", path + "/votes",
                    new ExecuteVoteCommand(direct.currentRevision(),
                            VotingMethod.DESIGNATED, List.of(1))).statusCode());
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
