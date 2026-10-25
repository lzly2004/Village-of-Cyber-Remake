package com.vocr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.*;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.peiyi;
import com.vocr.replay.ReplaySave;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class CompletedReplayHttpTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private HttpResponse<String> request(GuestGameServer server, String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.port()+path)).timeout(Duration.ofSeconds(10));
        if (body != null) builder.header("Content-Type","application/json");
        return http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private String create(GuestGameServer server, peiyi config) throws Exception {
        var response = request(server,"POST","/api/v1/games",Map.of("configuration",config.name()));
        assertEquals(201,response.statusCode(),response.body());
        return "/api/v1/games/"+json.readTree(response.body()).path("sessionId").asText();
    }
    private JsonNode get(GuestGameServer server,String path) throws Exception { return json.readTree(request(server,"GET",path,null).body()); }

    @ParameterizedTest @EnumSource(value=peiyi.class,names="NONE",mode=EnumSource.Mode.EXCLUDE)
    void finishedReplayMatchesExistingV3WithoutWritingOrChangingAuthority(peiyi config) throws Exception {
        long seed=4200+config.ordinal();
        try (var server=new GuestGameServer(id -> SeededGuestSession.create(id,seed),3).start(0)) {
            var direct=SeededGuestSession.create("DIRECT_PRIVATE_CREDENTIAL",seed);
            direct.startGame(new StartGameCommand(0,config));
            String path=create(server,config), other=create(server,config);
            var otherBefore=get(server,other);
            assertEquals(409,request(server,"GET",path+"/replay?eventCursor="+direct.currentEventCursor(),null).statusCode());
            int turns=0;
            while (!direct.isFinished()) {
                assertTrue(++turns<80);
                var action=direct.currentUpdate().legalVote(VotingMethod.FREE).orElseThrow();
                var command=new ExecuteVoteCommand(direct.currentRevision(),action.method(),action.candidates());
                direct.executeVote(command);
                assertEquals(200,request(server,"POST",path+"/votes",command).statusCode());
            }
            long cursor=direct.currentEventCursor();
            var before=get(server,path);
            for (long invalid:List.of(0L,cursor-1,cursor+1)) {
                var rejected=request(server,"GET",path+"/replay?eventCursor="+invalid,null);
                assertEquals(400,rejected.statusCode(),rejected.body());
                assertFalse(rejected.body().contains("rawRecords"));
            }
            var response=request(server,"GET",path+"/replay?eventCursor="+cursor,null);
            assertEquals(200,response.statusCode(),response.body());
            assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());
            JsonNode file=json.readTree(response.body());
            assertTrue(file.isArray()); assertEquals(9,file.size());
            for (int i=1;i<9;i++) assertTrue(file.get(i).isNull());
            var expected=json.valueToTree(ReplaySave.fromRecords(0,direct.completedReplay(cursor).records()));
            ((com.fasterxml.jackson.databind.node.ObjectNode)expected).remove("saveTime");
            var actual=file.get(0).deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode)actual).remove("saveTime");
            assertEquals(expected,actual);
            assertEquals(3,actual.path("version").asInt());
            assertFalse(response.body().contains(path.substring(path.lastIndexOf('/')+1)));
            assertFalse(response.body().contains("sessionId"));
            assertFalse(response.body().contains("DIRECT_PRIVATE_CREDENTIAL"));
            var parallel=CompletableFuture.supplyAsync(() -> {
                try { return request(server,"GET",path+"/replay?eventCursor="+cursor,null).statusCode(); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            assertEquals(200,request(server,"GET",path+"/replay?eventCursor="+cursor,null).statusCode());
            assertEquals(409,request(server,"POST",path+"/votes",
                    new ExecuteVoteCommand(direct.currentRevision(),VotingMethod.FREE,List.of(1))).statusCode());
            assertEquals(200,parallel.join());
            assertEquals(before,get(server,path)); assertEquals(otherBefore,get(server,other));
            assertEquals(409,request(server,"GET",other+"/replay?eventCursor="+cursor,null).statusCode());
            assertEquals(204,request(server,"DELETE",path,null).statusCode());
            assertEquals(404,request(server,"GET",path+"/replay?eventCursor="+cursor,null).statusCode());
        }
    }
    @Test void cursorIsRequiredAndStrictAndInvalidReadsNeverReturnSecrets() throws Exception {
        try (var server=new GuestGameServer(id -> SeededGuestSession.create(id,4201),2).start(0)) {
            String path=create(server,peiyi.jianyi); var before=get(server,path);
            for (String query:List.of("","?eventCursor=-1","?eventCursor=null","?eventCursor=1.5","?eventCursor=01","?eventCursor=0&eventCursor=1","?eventCursor=999999999999999999999")) {
                var response=request(server,"GET",path+"/replay"+query,null);
                assertEquals(400,response.statusCode(),response.body());
                assertFalse(response.body().contains("rawRecords"));
            }
            assertEquals(before,get(server,path));
            assertEquals(404,request(server,"GET","/api/v1/games/unknown/replay?eventCursor=0",null).statusCode());
        }
    }
}
