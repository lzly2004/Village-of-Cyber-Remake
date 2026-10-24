package com.vocr.replay;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplayFileExchangeTest {
    @TempDir Path directory;
    private final ObjectMapper mapper=new ObjectMapper();
    private byte[] resource(String name)throws Exception {try(var input=getClass().getResourceAsStream("/replay/"+name)){assertNotNull(input);return input.readAllBytes();}}
    @TestFactory Collection<DynamicTest> sharedFileBoundaryCases()throws Exception {
        JsonNode cases=mapper.readTree(resource("web035-file-cases.json"));var tests=new ArrayList<DynamicTest>();
        for(JsonNode c:cases)tests.add(DynamicTest.dynamicTest(c.get("name").asText(),()->{
            JsonNode slots=mapper.readTree(resource("exchange-v3.json"));
            for(JsonNode patch:c.get("patches")){
                String path=patch.get("path").asText();int split=path.lastIndexOf('/');JsonNode parent=slots.at(path.substring(0,split));String key=path.substring(split+1);
                if(patch.path("remove").asBoolean()){if(parent.isArray())((ArrayNode)parent).remove(Integer.parseInt(key));else((ObjectNode)parent).remove(key);}
                else if(parent.isArray())((ArrayNode)parent).set(Integer.parseInt(key),patch.get("value"));else((ObjectNode)parent).set(key,patch.get("value"));
            }
            byte[] bytes=mapper.writeValueAsBytes(slots),before=bytes.clone();
            if(c.get("ok").asBoolean()){
                var item=ReplayFileExchange.parse(bytes).getFirst();assertEquals(3,item.replay().version);assertEquals(0,item.replay().slotIndex);
                assertEquals(slots.get(0).get("rawRecords"),item.data().get("rawRecords"));
                assertEquals(item.replay().rawRecords,ReplayFileExchange.parse(ReplayFileExchange.encode(List.of(item.replay()))).getFirst().replay().rawRecords);
                if(c.get("name").asText().startsWith("unfinished-"))assertTrue(item.readOnly());
                if(c.get("name").asText().startsWith("version-")){assertEquals(slots.get(0).get("version").asInt()<=1?-1:0,item.replay().peiyiVillageCount);assertArrayEquals(new int[0],item.replay().daySnapshots.getFirst().players[0].skillTargets);}
                var detached=(ObjectNode)item.data();detached.put("totalDays",999);assertNotEquals(999,item.replay().totalDays);
            }else assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.parse(bytes));
            assertArrayEquals(before,bytes);
        }));return tests;
    }
    @Test void encodingBoundsDuplicateKeysAndMultiSlotEnvelope()throws Exception {
        String text=new String(resource("exchange-v3.json"),StandardCharsets.UTF_8);
        assertEquals(1,ReplayFileExchange.parse(("\uFEFF"+text.replace("\n","\r\n")).getBytes(StandardCharsets.UTF_8)).size());
        for(byte[] bad:List.of(new byte[]{(byte)255},("\uFEFF\uFEFF"+text).getBytes(StandardCharsets.UTF_8),(text+"{}").getBytes(StandardCharsets.UTF_8),text.replace("\"version\": 3","\"version\":3,\"notes\":0.1000000000000000001").getBytes(StandardCharsets.UTF_8),text.replace("\"version\": 3","\"version\": 3,\"ver\\u0073ion\":3").getBytes(StandardCharsets.UTF_8)))assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.parse(bad));
        byte[] padded=new byte[ReplayFileExchange.MAX_BYTES];Arrays.fill(padded,(byte)32);System.arraycopy(resource("exchange-v3.json"),0,padded,0,resource("exchange-v3.json").length);
        assertEquals(1,ReplayFileExchange.parse(padded).size());assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.parse(Arrays.copyOf(padded,padded.length+1)));
        var slots=(ArrayNode)mapper.readTree(resource("exchange-v3.json"));ObjectNode copy=slots.get(0).deepCopy();copy.put("slotIndex",8);slots.set(8,copy);
        var candidates=ReplayFileExchange.parse(mapper.writeValueAsBytes(slots));assertEquals(List.of(0,8),candidates.stream().map(ReplayFileExchange.Candidate::fileSlot).toList());assertEquals(0,candidates.getLast().replay().slotIndex);
    }
    @Test void invalidSelectedFileNeverCallsDestructiveManagerLoadOrChangesGoodFiles()throws Exception {
        Path good=directory.resolve("good.json"),bad=directory.resolve("bad.json");Files.write(good,resource("exchange-v3.json"));Files.writeString(bad,"broken");
        var manager=new ReplayManager(good.toString());byte[] before=Files.readAllBytes(good),badBefore=Files.readAllBytes(bad);
        assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.read(bad));assertArrayEquals(before,Files.readAllBytes(good));assertArrayEquals(badBefore,Files.readAllBytes(bad));assertEquals(1,manager.getUsedSlotCount());
        assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.parse(resource("legacy-v1.json")));
        try(var paths=Files.list(directory)){assertEquals(2,paths.count());}
    }
    @Test void exportRefusesUnconfirmedOverwriteAndSymlinksAndKeepsManagerSourceUnchanged()throws Exception {
        var replay=ReplayFileExchange.parse(resource("exchange-v3.json")).getFirst().replay();replay.slotIndex=8;
        byte[] data=ReplayFileExchange.encode(List.of(replay));assertEquals(8,replay.slotIndex);
        Path target=directory.resolve("export.json");Files.writeString(target,"original");
        assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.write(target,data,false));assertEquals("original",Files.readString(target));
        ReplayFileExchange.write(target,data,true);assertEquals(replay.rawRecords,ReplayFileExchange.read(target).getFirst().replay().rawRecords);
        Path link=directory.resolve("link.json");Files.createSymbolicLink(link,target.getFileName());
        assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.read(link));assertThrows(ReplayFileExchange.Problem.class,()->ReplayFileExchange.write(link,data,true));assertArrayEquals(data,Files.readAllBytes(target));
    }
}
