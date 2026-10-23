package com.vocr.replay;

import com.vocr.application.ReplayRecording;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import static org.junit.jupiter.api.Assertions.*;

class ReplaySlotSinkTest {
    @TempDir Path directory;
    private ReplayRecording recording() throws Exception {
        try (var input=getClass().getResourceAsStream("/replay/exchange-v3.json")) {
            var tree=new ObjectMapper().readTree(input).get(0).get("rawRecords");
            var records=new java.util.ArrayList<String>();tree.forEach(n->records.add(n.asText()));
            return new ReplayRecording(records);
        }
    }
    @Test void adapterWritesExistingNineSlotV3AndPreservesOtherSlots() throws Exception {
        var path=directory.resolve("slots.json");
        var manager=new ReplayManager(path.toString());
        var records=recording();
        new ReplaySlotSink(manager,0).save(records);
        var original=manager.getSlot(0);
        new ReplaySlotSink(manager,8).save(records);
        assertSame(original,manager.getSlot(0));
        var root=new ObjectMapper().readTree(Files.readAllBytes(path));
        assertEquals(9,root.size());
        assertEquals(3,root.get(8).get("version").asInt());
        assertEquals(8,root.get(8).get("slotIndex").asInt());
        assertEquals(new ObjectMapper().valueToTree(records.records()),root.get(8).get("rawRecords"));
        assertEquals(records.records(),new ReplayManager(path.toString()).getSlot(8).rawRecords);
        assertEquals(4,manager.getSlot(8).totalDays);
        try(var files=Files.list(directory)){assertFalse(files.anyMatch(p->p.getFileName().toString().endsWith(".tmp")));}
    }
    @Test void failedReplacementKeepsPublishedSlotsAndExistingTargetAndRemovesOwnTemporaryFile() throws Exception {
        var path=directory.resolve("slots.json");
        var manager=new ReplayManager(path.toString());
        var records=recording();
        new ReplaySlotSink(manager,0).save(records);
        var original=manager.getSlot(0);
        Files.move(path,directory.resolve("original-good.json"));
        Files.createDirectory(path); Files.writeString(path.resolve("sentinel"),"do not overwrite");
        assertThrows(UncheckedIOException.class, () -> new ReplaySlotSink(manager,1).save(records));
        assertSame(original,manager.getSlot(0));
        assertNull(manager.getSlot(1));
        assertEquals("do not overwrite",Files.readString(path.resolve("sentinel")));
        assertEquals(records.records(),new ReplayManager(directory.resolve("original-good.json").toString()).getSlot(0).rawRecords);
        try(var files=Files.list(directory)){assertFalse(files.anyMatch(p->p.getFileName().toString().endsWith(".tmp")));}
    }
    @Test void invalidSlotIsRejectedBeforeTouchingDestination() throws Exception {
        var path=directory.resolve("slots.json");
        var manager=new ReplayManager(path.toString());
        var before=Files.readAllBytes(path);
        assertThrows(IllegalArgumentException.class, () -> new ReplaySlotSink(manager,-1));
        assertThrows(IllegalArgumentException.class, () -> new ReplaySlotSink(manager,9));
        assertArrayEquals(before,Files.readAllBytes(path));
    }
}
