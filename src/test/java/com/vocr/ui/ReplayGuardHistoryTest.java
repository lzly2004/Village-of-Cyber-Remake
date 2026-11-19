package com.vocr.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.replay.ReplaySave;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReplayGuardHistoryTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private com.fasterxml.jackson.databind.JsonNode fixture() throws Exception {
        try (var input = getClass().getResourceAsStream("/replay/hunter-guard-display.json")) {
            assertNotNull(input); return mapper.readTree(input);
        }
    }
    private ReplaySave replay() throws Exception { return mapper.treeToValue(fixture().get("replay"), ReplaySave.class); }

    @Test void bothClientsShareExactRowsForNeverCoLateCoAndDeath() throws Exception {
        var f = fixture(); var replay = mapper.treeToValue(f.get("replay"), ReplaySave.class);
        String before = mapper.writeValueAsString(replay);
        for (int day : List.of(3, 5, 6)) {
            var expected = f.get("expected").get(String.valueOf(day));
            var lines = ReplayGuardHistory.lines(replay, day);
            assertEquals(expected.size(), lines.size());
            for (int i = 0; i < lines.size(); i++) {
                assertEquals(expected.get(i).get("playerIndex").asInt(), lines.get(i).playerIndex());
                assertEquals(expected.get(i).get("text").asText(), lines.get(i).text());
                assertEquals(expected.get(i).get("unannounced").asBoolean(), lines.get(i).unannounced());
            }
            assertTrue(ReplayTextFormatter.buildNormalInfo(replay, day, 12).endsWith(lines.stream().map(ReplayGuardHistory.Line::text).reduce("", String::concat)));
        }
        assertEquals(before, mapper.writeValueAsString(replay));
    }

    @Test void actualHunterBecomesNormalOnCoDayWithoutChangingOldTargets() throws Exception {
        var replay = replay();
        for (var s : replay.daySnapshots) if (s.dayNumber >= 5) {
            var p = s.players[0]; p.claimedRole = 3; p.comingOutDay = 4;
        }
        assertTrue(ReplayGuardHistory.lines(replay, 3).getFirst().unannounced());
        assertFalse(ReplayGuardHistory.lines(replay, 4).getFirst().unannounced());
        assertEquals("青年 : 情報→少年\n", ReplayGuardHistory.lines(replay, 4).getFirst().text());
        assertTrue(ReplayGuardHistory.lines(replay, 3).getFirst().unannounced()); // navigating backward
    }

    @Test void futureHistoryCannotRepairAnOlderMissingDiary() throws Exception {
        var replay = replay();
        for (var s : replay.daySnapshots) if (s.dayNumber <= 4) {
            s.players[0].skillTargets = new int[0]; s.players[0].skillTarget = 0;
        }
        assertEquals("青年 : （記録なし）\n", ReplayGuardHistory.lines(replay, 3).getFirst().text());
        // A missing exact legacy snapshot cannot turn a later positional record into this night's target.
        replay.daySnapshots.removeIf(s -> s.dayNumber == 3);
        assertEquals("青年 : （記録なし）\n", ReplayGuardHistory.lines(replay, 3).getFirst().text());
    }

    @Test void styledPaneHasOnlyUnannouncedRowsGrayAndKeepsReadOnlyFont() throws Exception {
        var replay = replay(); var lines = ReplayGuardHistory.lines(replay, 3);
        var text = ReplayTextFormatter.buildNormalInfo(replay, 3, 12);
        SwingUtilities.invokeAndWait(() -> {
            var pane = ReplayGuardTextPane.create(text, lines);
            assertFalse(pane.isEditable()); assertFalse(pane.isOpaque());
            assertEquals(24, pane.getFont().getSize()); assertTrue(pane.getFont().isBold());
            for (var line : lines) {
                var attributes = pane.getStyledDocument().getCharacterElement(text.indexOf(line.text())).getAttributes();
                assertEquals(line.unannounced() ? ReplayGuardTextPane.UNANNOUNCED : Color.BLACK,
                        StyleConstants.getForeground(attributes));
            }
        });
    }

    @Test void unclaimedHunterBeforeAnyGuardOpportunityHasNoRow() throws Exception {
        for (int[] death : new int[][] {{1, 4}, {2, 1}, {2, 2}, {2, 3}}) {
            var replay = replay();
            for (var s : replay.daySnapshots) {
                var p = s.players[0]; p.deathDay = s.dayNumber > death[0] ? death[0] : 0;
                p.deathReason = s.dayNumber > death[0] ? death[1] : 0;
                p.skillTargets = new int[0]; p.skillTarget = 0;
            }
            String before = mapper.writeValueAsString(replay);
            for (int day : List.of(3, 4, 7))
                assertTrue(ReplayGuardHistory.lines(replay, day).stream().noneMatch(line -> line.playerIndex() == 1));
            assertEquals(before, mapper.writeValueAsString(replay));
        }
    }

    @Test void deathAfterOneGuardNightKeepsTargetOrReportsUnavailableRecord() throws Exception {
        var replay = replay();
        for (var s : replay.daySnapshots) {
            s.players[0].deathDay = s.dayNumber >= 4 ? 3 : 0;
            s.players[0].deathReason = s.dayNumber >= 4 ? 1 : 0;
        }
        assertEquals("青年 : 情報\n", ReplayGuardHistory.lines(replay, 4).getFirst().text());
        for (var s : replay.daySnapshots) {
            s.players[0].skillTargets = new int[0]; s.players[0].skillTarget = 0;
        }
        assertEquals("青年 : （記録なし）\n", ReplayGuardHistory.lines(replay, 4).getFirst().text());
    }
}
