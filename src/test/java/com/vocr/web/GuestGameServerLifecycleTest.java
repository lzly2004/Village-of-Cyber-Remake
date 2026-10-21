package com.vocr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.GameSession;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.peiyi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GuestGameServerLifecycleTest
{
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void slowCreation_shouldReserveCapacityWithoutBlockingExistingSessions() throws Exception
    {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (GuestGameServer server = new GuestGameServer(id -> {
            if (calls.incrementAndGet() == 2) {
                entered.countDown();
                await(proceed);
            }
            return SeededGuestSession.create(id, 4201);
        }, 2).start(0))
        {
            CompletableFuture<HttpResponse<String>> slow = null;
            try {
                String first = create(server);
                slow = client.sendAsync(http(server, "POST", "/api/v1/games", "{\"configuration\":\"jianyi\"}"),
                        HttpResponse.BodyHandlers.ofString());
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                JsonNode before = json.readTree(send(server, "GET", first, null).body());
                assertFalse(slow.isDone(), "factory must still be blocked during existing-session reads");
                assertEquals(503, send(server, "POST", "/api/v1/games",
                        "{\"configuration\":\"jianyi\"}").statusCode());
                int target = before.path("legalTargetSelections").get(0).path("targets").get(0).asInt();
                assertEquals(200, send(server, "PUT", first + "/vote-target",
                        "{\"expectedRevision\":1,\"target\":" + target + ",\"selected\":true}").statusCode());
                assertEquals(204, send(server, "DELETE", first, null).statusCode());
                String replacement = create(server);
                assertEquals(200, send(server, "GET", replacement, null).statusCode());
                assertFalse(slow.isDone(), "replacement must finish before slow creation resumes");
                proceed.countDown();
                assertEquals(201, slow.get(5, TimeUnit.SECONDS).statusCode());
                assertEquals(503, send(server, "POST", "/api/v1/games",
                        "{\"configuration\":\"jianyi\"}").statusCode());
            } finally {
                proceed.countDown();
                if (slow != null) slow.get(5, TimeUnit.SECONDS);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failedFactoryOrStart_shouldReleaseReservedCapacity(boolean factoryFails) throws Exception
    {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<GameSession> failed = new AtomicReference<>();
        try (GuestGameServer server = new GuestGameServer(id -> {
            if (calls.incrementAndGet() == 1 && factoryFails)
                throw new RuntimeException("factory probe");
            GameSession session = SeededGuestSession.create(id, 4201);
            if (calls.get() == 1) {
                session.close(); // startGame must reject this test-only candidate
                failed.set(session);
            }
            return session;
        }, 1).start(0))
        {
            assertEquals(factoryFails ? 500 : 409, send(server, "POST", "/api/v1/games",
                    "{\"configuration\":\"jianyi\"}").statusCode());
            if (failed.get() != null) assertEquals(GameSession.State.CLOSED, failed.get().state());
            assertNotNull(create(server));
        }
    }

    @Test
    void closeDuringCreation_shouldCloseCandidateAndNeverPublishIt() throws Exception
    {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicReference<GameSession> candidate = new AtomicReference<>();
        GuestGameServer server = new GuestGameServer(id -> {
            candidate.set(SeededGuestSession.create(id, 4201));
            entered.countDown();
            await(proceed);
            return candidate.get();
        }, 1);
        // Exercise registry publication directly so Jetty graceful shutdown cannot hide the race.
        var create = GuestGameServer.class.getDeclaredMethod("create", peiyi.class);
        create.setAccessible(true);
        CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> {
            try { create.invoke(server, peiyi.jianyi); return null; }
            catch (InvocationTargetException error) { return error.getCause(); }
            catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            server.close();
            proceed.countDown();
            assertInstanceOf(IllegalStateException.class, result.get(5, TimeUnit.SECONDS));
            assertEquals(GameSession.State.CLOSED, candidate.get().state());
            assertThrows(IllegalStateException.class, () -> server.start(0));
            InvocationTargetException retry = assertThrows(InvocationTargetException.class,
                    () -> create.invoke(server, peiyi.jianyi));
            assertInstanceOf(IllegalStateException.class, retry.getCause());
            server.close(); // idempotent
        } finally {
            proceed.countDown();
            result.get(5, TimeUnit.SECONDS);
            server.close();
        }
    }

    @Test
    void cli_shouldValidatePortsAndReportBindFailuresWithoutStackTraces() throws Exception
    {
        assertEquals(7070, GuestGameServer.parsePort(new String[0]));
        assertEquals(1, GuestGameServer.parsePort(new String[]{"1"}));
        assertEquals(65535, GuestGameServer.parsePort(new String[]{"65535"}));
        PrintStream original = System.err;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8);
             GuestGameServer occupied = new GuestGameServer().start(0))
        {
            System.setErr(capture);
            for (String port : new String[]{"abc", "-1", "0", "65536", "999999999999", " 7070", "+7070"})
                assertEquals(2, GuestGameServer.run(new String[]{port}));
            assertEquals(2, GuestGameServer.run(new String[]{"7070", "7071"}));
            assertEquals(1, GuestGameServer.run(new String[]{String.valueOf(occupied.port())}));
        } finally { System.setErr(original); }
        String diagnostic = output.toString(StandardCharsets.UTF_8);
        assertTrue(diagnostic.contains("用法"));
        assertTrue(diagnostic.contains("是否被占用"));
        assertFalse(diagnostic.contains("\tat "), diagnostic);
        assertFalse(diagnostic.contains("Exception:"), diagnostic);
    }

    private static void await(CountDownLatch latch)
    {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("probe timed out");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    private String create(GuestGameServer server) throws Exception
    {
        HttpResponse<String> response = send(server, "POST", "/api/v1/games", "{\"configuration\":\"jianyi\"}");
        assertEquals(201, response.statusCode(), response.body());
        return "/api/v1/games/" + json.readTree(response.body()).path("sessionId").asText();
    }

    private HttpResponse<String> send(GuestGameServer server, String method, String path, String body) throws Exception
    {
        return client.send(http(server, method, path, body), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest http(GuestGameServer server, String method, String path, String body)
    {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                .timeout(Duration.ofSeconds(3)).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build();
    }
}
