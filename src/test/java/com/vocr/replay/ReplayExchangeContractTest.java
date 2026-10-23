package com.vocr.replay;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Encoding/legacy contract only: no user data, Swing, HTTP or rule changes. */
class ReplayExchangeContractTest {
    @TempDir Path temporary;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private byte[] fixture(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/replay/" + name)) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    @Test void currentV3Sample_isRebuiltByActualDesktopParserAndRoundTripsThroughSlots() throws Exception {
        byte[] bytes = fixture("exchange-v3.json");
        ReplaySave[] slots = mapper.readValue(bytes, ReplaySave[].class);
        assertEquals(ReplayManager.MAX_SLOTS, slots.length);
        ReplaySave expected = slots[0];
        ReplaySave rebuilt = ReplaySave.fromRecords(0, expected.rawRecords);
        rebuilt.saveTime = expected.saveTime; // Timestamp is intentionally not a game identity.
        assertEquals(mapper.readTree(bytes).get(0), mapper.valueToTree(rebuilt));
        Path file = temporary.resolve("isolated-slots.json");
        Files.write(file, bytes);
        ReplayManager manager = new ReplayManager(file.toString());
        assertEquals(1, manager.getUsedSlotCount());
        assertEquals(expected.rawRecords, manager.getSlot(0).rawRecords);
        manager.save();
        assertEquals(mapper.readTree(bytes), mapper.readTree(Files.readAllBytes(file)));
        assertEquals(4, rebuilt.totalDays); // Last snapshot can be one day after END.
        assertEquals(expected.rawRecords.size(), rebuilt.totalEvents);
        assertTrue(rebuilt.rawRecords.stream().anyMatch(s -> s.contains("9007199254740993")));
        assertTrue(rebuilt.rawRecords.stream().anyMatch(s -> s.contains("ä/ü\n")));
    }

    @Test void v3HistoryCutoffAndExplicitZeroOverride_keepLegacyStFallbackSeparate() throws Exception {
        ReplaySave replay = mapper.readValue(fixture("exchange-v3.json"), ReplaySave[].class)[0];
        assertEquals(5, replay.getVisibleSkillTarget(1, 1, 1)); // black target 2 in P=3
        assertEquals(5, replay.getVisibleSkillTarget(1, 1, 2));
        assertEquals(0, replay.getVisibleSkillTarget(1, 1, 3)); // explicit zero is not old st
        assertEquals(1, replay.getVisibleSkillTarget(3, 1, 2));
        assertEquals(2, replay.getVisibleSkillTarget(3, 1, 3)); // later history, cutoff visible+1
        assertEquals(List.of(1, 3), replay.daySnapshots.get(1).greyTargetCharNums);
        assertEquals(List.of(28), replay.daySnapshots.get(1).designatedTargetCharNums);
        assertArrayEquals(new int[]{3, 0}, replay.daySnapshots.get(1).voteRounds.get(0).votes[2]);
        for (DaySnapshot day : replay.daySnapshots)
            for (DaySnapshot.PlayerStatus player : day.players) player.skillTargets = new int[0];
        assertEquals(5, replay.getVisibleSkillTarget(1, 1, 3)); // old snapshot skillDay+1 st
    }

    @Test void explicitVersionsZeroOneTwo_migrateWithoutInventingSkillHistoryOrChangingRawRecords() throws Exception {
        for (int version = 0; version <= 2; version++) {
            var source = mapper.readTree(fixture("legacy-v1.json"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) source.get(0)).put("version", version);
            Path file = temporary.resolve("v" + version + ".json");
            mapper.writeValue(file.toFile(), source);
            ReplayManager manager = new ReplayManager(file.toString());
            ReplaySave migrated = manager.getSlot(0);
            assertEquals(ReplaySave.CURRENT_VERSION, migrated.version);
            assertEquals(version < 2 ? -1 : 0, migrated.peiyiVillageCount);
            assertEquals(source.get(0).get("rawRecords"), mapper.valueToTree(migrated.rawRecords));
            assertArrayEquals(new int[0], migrated.daySnapshots.get(0).players[0].skillTargets);
            assertEquals(2, migrated.getVisibleSkillTarget(1, 1, 2));
            manager.save();
            assertEquals(mapper.valueToTree(migrated), mapper.valueToTree(new ReplayManager(file.toString()).getSlot(0)));
        }
    }

    @Test void oldRawRecording_keepsOriginalOrderAcrossJsonAndNeverSplitsEmbeddedNewlines() throws Exception {
        List<String> old = new String(fixture("legacy-record-v0.txt"), java.nio.charset.StandardCharsets.UTF_8).lines().toList();
        ReplaySave rebuilt = ReplaySave.fromRecords(0, old);
        ReplaySave decoded = mapper.readValue(mapper.writeValueAsBytes(rebuilt), ReplaySave.class);
        assertEquals(old, decoded.rawRecords);
        assertEquals(3, decoded.getVisibleSkillTarget(1, 1, 2));
        assertEquals(3, decoded.endResult);
        ReplaySave current = mapper.readValue(fixture("exchange-v3.json"), ReplaySave[].class)[0];
        assertNotEquals(current.rawRecords.size(), String.join("\n", current.rawRecords).lines().count());
    }
}
