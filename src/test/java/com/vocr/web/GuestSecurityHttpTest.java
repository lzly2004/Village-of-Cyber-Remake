package com.vocr.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.GameSession;
import com.vocr.core.SeededGuestSession;
import com.vocr.logging.DebugLogger;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class GuestSecurityHttpTest
{
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final AtomicLong clock = new AtomicLong();
    private final List<GameSession> sessions = new ArrayList<>();
    private GuestRequestLimits.Policy policy(int burst, int createBurst, int sessionBurst, int replayBurst, int concurrent) {
        return new GuestRequestLimits.Policy(1,burst,1,createBurst,1,sessionBurst,1,replayBurst,concurrent,1,1,2);
    }
    private GuestGameServer server(GuestRequestLimits.Policy policy, Duration idle, Duration life) {
        return new GuestGameServer(id -> {
            var session = SeededGuestSession.create(id,4201); sessions.add(session); return session;
        }, 10, clock::get, idle, life, policy);
    }
    private GuestGameServer server(GuestRequestLimits.Policy policy) {
        return server(policy,Duration.ofHours(1),Duration.ofHours(24));
    }
    private HttpRequest request(GuestGameServer s, String method, String path, String body) {
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+s.port()+path)).timeout(Duration.ofSeconds(10));
        if (body != null) b.header("Content-Type","application/json");
        return b.method(method,body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private HttpResponse<String> send(GuestGameServer s, String method, String path, String body) throws Exception {
        return http.send(request(s,method,path,body),HttpResponse.BodyHandlers.ofString());
    }
    private String create(GuestGameServer s) throws Exception {
        var r=send(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\"}");
        assertEquals(201,r.statusCode(),r.body());
        return "/api/v1/games/"+json.readTree(r.body()).path("sessionId").asText();
    }
    private void limited(HttpResponse<String> r) throws Exception {
        assertEquals(429,r.statusCode(),r.body());
        assertEquals("REQUEST_LIMIT",json.readTree(r.body()).path("code").asText());
        assertTrue(Integer.parseInt(r.headers().firstValue("Retry-After").orElseThrow())>=1);
        assertEquals("no-store",r.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("no-referrer",r.headers().firstValue("Referrer-Policy").orElseThrow());
    }

    @Test void globalRateRunsBeforeParsingAndFactoryAndRefillsWithoutSleep() throws Exception {
        try(var s=server(policy(1,10,10,10,2)).start(0)) {
            assertEquals(400,send(s,"POST","/api/v1/games","not-json").statusCode());
            var spoofed=HttpRequest.newBuilder(request(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\"}"),(name,value)->true)
                    .header("X-Forwarded-For","203.0.113.17").header("Forwarded","for=203.0.113.18")
                    .header("Origin","https://example.invalid").build();
            var rejected=http.send(spoofed,HttpResponse.BodyHandlers.ofString());
            limited(rejected);assertTrue(rejected.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
            assertTrue(sessions.isEmpty());
            clock.set(1_000_000_000); assertNotNull(create(s)); assertEquals(1,sessions.size());
        }
    }

    @Test void createBudgetSurvivesDeletionAndDoesNotAllocateUnboundedClientKeys() throws Exception {
        try(var s=server(policy(100,1,100,100,2)).start(0)) {
            String first=create(s); assertEquals(204,send(s,"DELETE",first,null).statusCode());
            limited(send(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\"}"));
            assertEquals(1,sessions.size());
            clock.set(Duration.ofMinutes(1).toNanos()); assertNotNull(create(s));
            assertEquals(2,sessions.size());
        }
    }

    @Test void perSessionRejectionDoesNotExecuteCommandOrAffectAnotherGuest() throws Exception {
        try(var s=server(policy(100,10,1,10,3)).start(0)) {
            String a=create(s),b=create(s);
            assertEquals(200,send(s,"GET",a,null).statusCode());
            var before=sessions.getFirst().currentUpdate();
            limited(send(s,"PUT",a+"/avoidance","{\"expectedRevision\":1,\"enabled\":true}"));
            assertEquals(before,sessions.getFirst().currentUpdate());
            assertEquals(200,send(s,"GET",b,null).statusCode());
            clock.set(1_000_000_000); assertEquals(200,send(s,"GET",a,null).statusCode());
        }
    }

    @Test void replayRejectionsAreBoundedAndReleaseTheHandlerLeaseOnBusinessFailure() throws Exception {
        try(var s=server(policy(100,10,100,1,1)).start(0)) {
            String path=create(s);
            assertEquals(409,send(s,"GET",path+"/replay?eventCursor=0",null).statusCode());
            limited(send(s,"GET",path+"/replay?eventCursor=0",null));
            assertEquals(200,send(s,"GET",path,null).statusCode());
            clock.set(Duration.ofMinutes(1).toNanos());
            assertEquals(409,send(s,"GET",path+"/replay?eventCursor=0",null).statusCode());
        }
    }

    @Test void registeredReplayAliasesAreLimitedAndPinnedHeadDoesNotInvokeTheGetHandler() throws Exception {
        try(var s=server(policy(100,10,100,10,3)).start(0)) {
            String path=create(s);
            var field=GuestGameServer.class.getDeclaredField("requestLimits");field.setAccessible(true);
            var limits=(GuestRequestLimits)field.get(s);
            try(var held=limits.enter(false,true)) {
                limited(send(s,"GET",path+"/replay?eventCursor=0",null));
                limited(send(s,"GET",path+"/replay/?eventCursor=0",null));
                var head=send(s,"HEAD",path+"/replay?eventCursor=not-a-cursor",null);
                assertEquals(200,head.statusCode());assertTrue(head.body().isEmpty());
                // The pinned framework answers HEAD without invoking GET (which would reject this cursor).
                assertEquals(200,send(s,"GET",path,null).statusCode());
            }
            assertEquals(400,send(s,"GET",path+"/replay?eventCursor=not-a-cursor",null).statusCode());
            assertEquals(409,send(s,"GET",path+"/replay/?eventCursor=0",null).statusCode());
        }
    }

    @Test void registeredCreationAliasesCannotBypassTheSharedCreationBudget() throws Exception {
        try(var s=server(policy(100,1,100,10,3)).start(0)) {
            create(s);
            limited(send(s,"POST","/api/v1/games/","{\"configuration\":\"jianyi\"}"));
            assertEquals(1,sessions.size());
        }
    }

    @Test void slowCreateHoldsGlobalAdmissionAndReleasesItWithoutQueuedGameWork() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),proceed=new CountDownLatch(1);
        AtomicInteger calls=new AtomicInteger();
        try(var s=new GuestGameServer(id -> {
            calls.incrementAndGet(); entered.countDown();
            try { assertTrue(proceed.await(5,TimeUnit.SECONDS)); }
            catch(InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
            return SeededGuestSession.create(id,4201);
        },10,clock::get,Duration.ofHours(1),Duration.ofHours(24),policy(100,10,100,10,1)).start(0)) {
            CompletableFuture<HttpResponse<String>> pending=http.sendAsync(request(s,"POST","/api/v1/games",
                    "{\"configuration\":\"jianyi\"}"),HttpResponse.BodyHandlers.ofString());
            try {
                assertTrue(entered.await(5,TimeUnit.SECONDS));
                limited(send(s,"GET","/api/v1/games/unknown",null));
                limited(send(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\"}"));
                assertEquals(1,calls.get()); assertFalse(pending.isDone());
            } finally { proceed.countDown(); }
            assertEquals(201,pending.get(5,TimeUnit.SECONDS).statusCode());
            assertEquals(404,send(s,"GET","/api/v1/games/unknown",null).statusCode());
        }
    }

    @Test void rejectedRateDoesNotRenewIdleWhileAdmittedBusinessFailureDoes() throws Exception {
        try(var s=server(policy(100,10,1,10,2),Duration.ofNanos(10),Duration.ofNanos(25)).start(0)) {
            String path=create(s),id=path.substring(path.lastIndexOf('/')+1);
            clock.set(9);
            assertThrows(IllegalArgumentException.class,()->s.withSession(id,session->{throw new IllegalArgumentException("probe");}));
            clock.set(18); assertThrows(GuestRequestLimits.Rejected.class,()->s.withSession(id,GameSession::currentUpdate));
            assertEquals(GameSession.State.RUNNING,sessions.getFirst().state());
            clock.set(19); s.reapExpired(); assertEquals(GameSession.State.CLOSED,sessions.getFirst().state());
        }
    }

    @Test void businessRejectionsNeverExtendAbsoluteLifetime() throws Exception {
        try(var s=server(policy(100,10,100,10,2),Duration.ofNanos(10),Duration.ofNanos(25)).start(0)) {
            String path=create(s),id=path.substring(path.lastIndexOf('/')+1);
            for(long now:new long[]{9,18,24}) {
                clock.set(now);
                assertThrows(IllegalStateException.class,()->s.withSession(id,session->{throw new IllegalStateException("probe");}));
            }
            clock.set(25); assertThrows(RuntimeException.class,()->s.withSession(id,GameSession::currentUpdate));
            assertEquals(GameSession.State.CLOSED,sessions.getFirst().state());
        }
    }

    @Test void sessionBorrowLimitDoesNotQueueAnotherCommandAndReturnsCapacity() throws Exception {
        CountDownLatch entered=new CountDownLatch(2),proceed=new CountDownLatch(1);
        try(var s=server(policy(100,10,100,10,8)).start(0)) {
            String path=create(s),id=path.substring(path.lastIndexOf('/')+1);
            java.util.function.Supplier<Long> borrow=()->s.withSession(id,session->{
                entered.countDown();
                try { assertTrue(proceed.await(5,TimeUnit.SECONDS)); }
                catch(InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
                return session.currentRevision();
            });
            var first=CompletableFuture.supplyAsync(borrow);var second=CompletableFuture.supplyAsync(borrow);
            try {
                assertTrue(entered.await(5,TimeUnit.SECONDS));
                limited(send(s,"PUT",path+"/avoidance","{\"expectedRevision\":1,\"enabled\":true}"));
                assertEquals(1,sessions.getFirst().currentRevision());
            } finally { proceed.countDown();first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS); }
            assertEquals(200,send(s,"GET",path,null).statusCode());
        }
    }

    @Test void oversizedAndMalformedBodiesReleaseAdmissionAndNeverReachFactory() throws Exception {
        try(var s=server(policy(100,10,100,10,1)).start(0)) {
            var large=send(s,"POST","/api/v1/games","{\"configuration\":\""+"x".repeat(16_384)+"\"}");
            assertEquals(413,large.statusCode());
            assertEquals(400,send(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\",\"secret\":1}").statusCode());
            assertTrue(sessions.isEmpty()); assertNotNull(create(s));
        }
    }

    @Test void credentialsPayloadAndNestedExceptionNeverReachApplicationLogsOrErrorBodies() throws Exception {
        String secret="11111111-1111-4111-8111-111111111111",payload="PRIVATE_PAYLOAD_PROBE";
        var out=new ByteArrayOutputStream(); PrintStream stdout=System.out,stderr=System.err;
        boolean enabled=DebugLogger.getInstance().isEnabled();
        AtomicReference<String> liveId=new AtomicReference<>(); AtomicInteger calls=new AtomicInteger();
        try(var capture=new PrintStream(out,true,StandardCharsets.UTF_8)) {
            System.setOut(capture);System.setErr(capture);
            try(var s=new GuestGameServer(id->{
                if(calls.incrementAndGet()==1){liveId.set(id);return SeededGuestSession.create(id,4201);}
                throw new RuntimeException(secret,new IllegalStateException(payload));
            },10).start(0)) {
                assertFalse(DebugLogger.getInstance().isEnabled());
                DebugLogger.info(secret+payload); // Core private diagnostics are off in the standalone Web process.
                var created=send(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\"}");
                assertEquals(201,created.statusCode());
                assertTrue(created.headers().firstValue("Location").orElseThrow().contains(liveId.get()));
                var error=send(s,"POST","/api/v1/games","{\"configuration\":\"jianyi\"}");
                assertEquals(500,error.statusCode());assertFalse(error.body().contains(secret));assertFalse(error.body().contains(payload));
                var query=send(s,"GET","/api/v1/games/"+secret+"?eventCursor="+payload,null);
                assertEquals(400,query.statusCode());assertFalse(query.body().contains(payload));
                var missing=send(s,"GET","/api/v1/games/"+secret+"/"+payload,null);
                assertEquals(404,missing.statusCode());assertFalse(missing.body().contains(secret));assertFalse(missing.body().contains(payload));
            }
        } finally {System.setOut(stdout);System.setErr(stderr);DebugLogger.getInstance().setEnabled(enabled);}
        String logs=out.toString(StandardCharsets.UTF_8);
        assertTrue(logs.contains("[HTTP] REQUEST_FAILED"));
        assertFalse(logs.contains(liveId.get()));assertFalse(logs.contains(secret));
        assertFalse(logs.contains(payload));assertFalse(logs.contains("java.lang.RuntimeException"));
    }
}
