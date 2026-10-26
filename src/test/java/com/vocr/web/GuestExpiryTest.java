package com.vocr.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.GameSession;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.peiyi;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GuestExpiryTest
{
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final AtomicLong clock = new AtomicLong();
    private final List<GameSession> created = new ArrayList<>();

    private GuestGameServer server(int capacity)
    {
        return new GuestGameServer(id -> {
            GameSession session = SeededGuestSession.create(id, 4201);
            created.add(session);
            return session;
        }, capacity, clock::get, Duration.ofNanos(10), Duration.ofNanos(25));
    }

    private String create(GuestGameServer server) throws Exception
    {
        var method = GuestGameServer.class.getDeclaredMethod("create", peiyi.class);
        method.setAccessible(true);
        return ((GameSession) method.invoke(server, peiyi.jianyi)).sessionId();
    }

    private HttpResponse<String> http(GuestGameServer server, String method, String path, String body) throws Exception
    {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/api/v1/games" + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void idleBoundary_reclaimsAndClosesWithoutChangingAnotherGuest() throws Exception
    {
        try (var server = server(2)) {
            String first = create(server);
            clock.set(5);
            String second = create(server);
            var before = server.withSession(second, session -> session.currentUpdate(0));
            clock.set(9);
            assertEquals(GameSession.State.RUNNING, created.getFirst().state());
            server.reapExpired();
            clock.set(10);
            server.reapExpired(); // Exact >= boundary, no wall clock / sleep.
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
            assertThrows(RuntimeException.class, () -> server.withSession(first, GameSession::currentUpdate));
            assertEquals(before, server.withSession(second, session -> session.currentUpdate(0)));
            assertNotNull(create(server)); // Reclaimed capacity, no active guest eviction.
        }
    }

    @Test
    void readRenewsIdleButNeverExtendsAbsoluteLifetime() throws Exception
    {
        try (var server = server(1)) {
            String id = create(server);
            for (long now : new long[]{9, 18, 24}) {
                clock.set(now);
                assertNotNull(server.withSession(id, GameSession::currentUpdate));
            }
            clock.set(25);
            assertThrows(RuntimeException.class, () -> server.withSession(id, GameSession::currentUpdate));
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
            assertNotNull(create(server));
        }
    }

    @Test
    void capacitySweepRecoversOrphanWithoutAnyFurtherAccessToItsCredential() throws Exception
    {
        try (var server = server(1)) {
            create(server); // Simulate lost POST response: browser never learns the credential.
            clock.set(10);
            assertNotNull(create(server)); // create performs its own sweep, not just timer.
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
            assertEquals(2, created.size());
        }
    }

    @Test
    void heldRequestSurvivesExpiryAndDelete_butNoNewRequestsCanEnter() throws Exception
    {
        try (var server = server(1).start(0)) {
            String id = create(server);
            CountDownLatch entered = new CountDownLatch(1), finish = new CountDownLatch(1);
            CompletableFuture<Long> active = CompletableFuture.supplyAsync(() -> server.withSession(id, session -> {
                entered.countDown();
                try { assertTrue(finish.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException error) { throw new AssertionError(error); }
                return session.currentRevision();
            }));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(204, http(server, "DELETE", "/" + id, null).statusCode());
                assertEquals(404, http(server, "GET", "/" + id, null).statusCode());
                clock.set(25);
                server.reapExpired();
                assertEquals(GameSession.State.RUNNING, created.getFirst().state());
                assertEquals(503, http(server, "POST", "", "{\"configuration\":\"jianyi\"}").statusCode());
                finish.countDown();
                assertEquals(1L, active.get(5, TimeUnit.SECONDS));
                assertEquals(GameSession.State.CLOSED, created.getFirst().state());
                assertNotNull(create(server));
            } finally { finish.countDown(); active.get(5, TimeUnit.SECONDS); }
        }
    }

    @Test
    void longRequestIsNotInterruptedByIdleSweep_andFinallyReleasesLeaseOnFailure() throws Exception
    {
        try (var server = server(1)) {
            String id = create(server);
            server.withSession(id, session -> {
                clock.set(10);
                server.reapExpired();
                assertEquals(GameSession.State.RUNNING, session.state());
                return null;
            });
            clock.set(19);
            assertThrows(IllegalArgumentException.class, () -> server.withSession(id, session -> {
                throw new IllegalArgumentException("bounded probe");
            }));
            clock.set(25);
            server.reapExpired();
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
        }
    }

    @Test
    void absoluteDeadlineDuringRequest_refusesNewBorrowersAndClosesOnReturn() throws Exception
    {
        try (var server = server(1).start(0)) {
            String id = create(server);
            long revision = server.withSession(id, session -> {
                clock.set(25);
                server.reapExpired();
                assertEquals(GameSession.State.RUNNING, session.state());
                assertThrows(RuntimeException.class, () -> server.withSession(id, GameSession::currentUpdate));
                return session.currentRevision();
            });
            assertEquals(1L, revision);
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
            assertNotNull(create(server));
        }
    }

    @Test
    void monotonicWraparound_keepsElapsedDurationSemantics() throws Exception
    {
        clock.set(Long.MAX_VALUE - 5);
        try (var server = server(1)) {
            create(server);
            clock.addAndGet(9);
            server.reapExpired();
            assertEquals(GameSession.State.RUNNING, created.getFirst().state());
            clock.incrementAndGet();
            server.reapExpired();
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
        }
    }

    @Test
    void credentialFailureIsUniformAndBounded_onReadWriteReplayAndDelete() throws Exception
    {
        try (var server = server(1).start(0)) {
            String id = create(server);
            assertTrue(id.matches("[0-9a-f-]{36}"));
            clock.set(10);
            List<String[]> routes = List.of(
                    new String[]{"GET", "?eventCursor=0", null},
                    new String[]{"GET", "/replay?eventCursor=0", null},
                    new String[]{"DELETE", "", null},
                    new String[]{"PUT", "/avoidance", "{\"expectedRevision\":1,\"enabled\":true}"},
                    new String[]{"POST", "/votes", "{\"expectedRevision\":1,\"method\":\"FREE\",\"candidates\":[1]}"},
                    new String[]{"PUT", "/vote-target", "{\"expectedRevision\":1,\"target\":1,\"selected\":true}"},
                    new String[]{"POST", "/claims/by-role", "{\"expectedRevision\":1,\"role\":\"zhan\"}"},
                    new String[]{"POST", "/claims/by-players", "{\"expectedRevision\":1,\"players\":[1]}"},
                    new String[]{"PUT", "/target-selections", "{\"expectedRevision\":1,\"type\":\"DIVINATION\",\"target\":1,\"selected\":true}"},
                    new String[]{"PUT", "/claimed-target-selections", "{\"expectedRevision\":1,\"type\":\"GUARD\",\"claimedPlayer\":1,\"target\":1,\"selected\":true}"});
            String expected = null;
            for (String token : List.of(id, UUID.randomUUID().toString(), "not-a-credential", id.toUpperCase())) {
                for (String[] route : routes) {
                    var response = http(server, route[0], "/" + token + route[1], route[2]);
                    assertEquals(404, response.statusCode(), response.body());
                    assertEquals("GAME_NOT_FOUND", json.readTree(response.body()).path("code").asText());
                    assertFalse(response.body().contains(token));
                    if (expected == null) expected = response.body();
                    assertEquals(expected, response.body());
                    assertEquals("no-store", response.headers().firstValue("cache-control").orElseThrow());
                    assertEquals("no-referrer", response.headers().firstValue("referrer-policy").orElseThrow());
                }
            }
            assertEquals(GameSession.State.CLOSED, created.getFirst().state());
        }
    }

    @Test
    void freshServiceRejectsOldCredential_andCloseStopsPeriodicWorker() throws Exception
    {
        String old;
        GuestGameServer first = server(1).start(0);
        try { old = create(first); }
        finally { first.close(); }
        var field = GuestGameServer.class.getDeclaredField("reaper");
        field.setAccessible(true);
        var worker = (ScheduledExecutorService) field.get(first);
        assertTrue(worker.isShutdown());
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        first.close();
        try (var next = server(1).start(0)) {
            assertEquals(404, http(next, "GET", "/" + old, null).statusCode());
            assertNotEquals(old, create(next));
        }
    }

    @Test
    void creationOutsideLockStillCannotPublishAfterShutdown() throws Exception
    {
        CountDownLatch entered = new CountDownLatch(1), finish = new CountDownLatch(1);
        AtomicReference<GameSession> candidate = new AtomicReference<>();
        var server = new GuestGameServer(id -> {
            candidate.set(SeededGuestSession.create(id, 4201));
            entered.countDown();
            try { assertTrue(finish.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException error) { throw new AssertionError(error); }
            return candidate.get();
        }, 1, clock::get, Duration.ofNanos(10), Duration.ofNanos(25));
        var future = CompletableFuture.runAsync(() -> assertThrows(Exception.class, () -> create(server)));
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            server.close();
            finish.countDown();
            future.get(5, TimeUnit.SECONDS);
            assertEquals(GameSession.State.CLOSED, candidate.get().state());
        } finally { finish.countDown(); server.close(); future.get(5, TimeUnit.SECONDS); }
    }

    @Test
    void durationsAndCapacityRejectInvalidPolicy()
    {
        assertThrows(IllegalArgumentException.class, () -> new GuestGameServer(GuestGameServer::newGuestSession, 0));
        assertThrows(IllegalArgumentException.class, () -> new GuestGameServer(GuestGameServer::newGuestSession,
                1, clock::get, Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new GuestGameServer(GuestGameServer::newGuestSession,
                1, clock::get, Duration.ofSeconds(1), Duration.ofSeconds(-1)));
    }
}
