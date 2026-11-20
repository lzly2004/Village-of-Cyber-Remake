package com.vocr.web;

import com.vocr.application.*;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.peiyi;
import com.vocr.logging.DebugLogger;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class WebOperationsTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> get(GuestGameServer server, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.port()+path))
                .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void healthDoesNotCreateGamesOrRefreshAnExistingCredential() throws Exception {
        AtomicLong now = new AtomicLong(); AtomicInteger creations = new AtomicInteger();
        AtomicReference<GameSession> game = new AtomicReference<>();
        try (var server = new GuestGameServer(id -> {
            creations.incrementAndGet(); var s = SeededGuestSession.create(id,4201); game.set(s); return s;
        }, 1, now::get, Duration.ofSeconds(10), Duration.ofMinutes(1)).start(0)) {
            var ready = get(server,"/api/v1/health");
            assertEquals(200,ready.statusCode()); assertEquals("{\"status\":\"ok\"}",ready.body());
            assertEquals("no-store",ready.headers().firstValue("Cache-Control").orElseThrow());
            assertEquals(0,creations.get());
            var created = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.port()+"/api/v1/games"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"configuration\":\"jianyi\"}")).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(201,created.statusCode());
            String path=created.headers().firstValue("Location").orElseThrow();
            var before=game.get().currentUpdate(0);
            now.set(Duration.ofSeconds(9).toNanos());
            assertEquals(200,get(server,"/api/v1/health").statusCode());
            assertEquals(before,game.get().currentUpdate(0)); assertEquals(1,creations.get());
            now.set(Duration.ofSeconds(11).toNanos());
            assertEquals(404,get(server,path).statusCode(),"health must not renew a guest");
        }
    }

    @Test void maintenanceFailureDegradesHealthAndSuccessRecoversWithoutLeakingCause() throws Exception {
        AtomicBoolean fail=new AtomicBoolean();
        var output=new ByteArrayOutputStream(); PrintStream previous=System.err;
        try(var capture=new PrintStream(output,true,StandardCharsets.UTF_8);
            var server=new GuestGameServer(id->SeededGuestSession.create(id,4201),1,
                    ()->{if(fail.get())throw new IllegalStateException("PRIVATE_REAPER_CAUSE");return System.nanoTime();},
                    Duration.ofMinutes(1),Duration.ofMinutes(2)).start(0)) {
            System.setErr(capture);
            fail.set(true); server.runMaintenance(); fail.set(false);
            var bad=get(server,"/api/v1/health");
            assertEquals(503,bad.statusCode());assertEquals("{\"status\":\"unavailable\"}",bad.body());
            server.runMaintenance(); assertEquals(200,get(server,"/api/v1/health").statusCode());
        } finally {System.setErr(previous);}
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("[HTTP] REAP_FAILED"));
        assertFalse(output.toString(StandardCharsets.UTF_8).contains("PRIVATE_REAPER_CAUSE"));
    }

    @Test void healthUsesGlobalAdmissionAndDoesNotAdvertisePrivateDiagnostics() throws Exception {
        var policy=new GuestRequestLimits.Policy(1,1,1,1,1,1,1,1,1,1,1,1);
        try(var server=new GuestGameServer(id->SeededGuestSession.create(id,4201),1,()->0,
                Duration.ofMinutes(1),Duration.ofMinutes(2),policy).start(0)) {
            assertEquals(200,get(server,"/api/v1/health").statusCode());
            assertEquals(429,get(server,"/api/v1/health").statusCode());
        }
    }

    @Test void completeGuestGameAndCloseRemainQuietAndKeepReplay() throws Exception {
        var output=new ByteArrayOutputStream(); PrintStream stdout=System.out,stderr=System.err;
        boolean enabled=DebugLogger.getInstance().isEnabled();
        try(var capture=new PrintStream(output,true,StandardCharsets.UTF_8)) {
            System.setOut(capture);System.setErr(capture);
            try(var server=new GuestGameServer().start(0)) {
                output.reset(); // Framework startup notice is outside the terminal/runtime quietness contract.
                var game=SeededGuestSession.create("PRIVATE_GAME_CREDENTIAL",4201);
                try {
                game.startGame(new StartGameCommand(0,peiyi.jianyi));
                int steps=0;
                while(!game.isFinished()) {
                    assertTrue(++steps<=80);
                    var free=game.currentUpdate().legalVote(VotingMethod.FREE).orElseThrow();
                    game.executeVote(new ExecuteVoteCommand(game.currentRevision(),free.method(),free.candidates()));
                }
                assertFalse(game.completedReplay(game.currentUpdate().nextEventCursor()).records().isEmpty());
                assertEquals(200,get(server,"/api/v1/health").statusCode());
                } finally {game.close();}
            }
        } finally {System.setOut(stdout);System.setErr(stderr);DebugLogger.getInstance().setEnabled(enabled);}
        assertEquals("",output.toString(StandardCharsets.UTF_8),"normal terminal and health requests must not log private results");
    }
}
