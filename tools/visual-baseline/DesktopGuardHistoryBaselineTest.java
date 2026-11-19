package com.vocr.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplayManager;
import com.vocr.replay.ReplaySave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real Swing rendering, no user files/live session/audio and no global keyboard input. */
class DesktopGuardHistoryBaselineTest {
    @TempDir Path temporary;

    @Test @Timeout(60)
    void realReplayPanelChangesGrayAtCoDayAndReturnsToGray() throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());
        var mapper = new ObjectMapper();
        var fixture = mapper.readTree(Files.readString(Path.of("src/test/resources/replay/hunter-guard-display.json")));
        var replay = mapper.treeToValue(fixture.get("replay"), ReplaySave.class);
        var helper = new DesktopBaselineTest(); Files.createDirectories(helper.output);
        var constructor = ReplayManager.class.getDeclaredConstructor(String.class); constructor.setAccessible(true);
        var manager = constructor.newInstance(temporary.resolve("replays.json").toString());
        var ui = new UI(helper.resources(), manager, GameRecordManager.inMemory());
        ui.attachSessionFactory(() -> { throw new AssertionError("Replay cannot create a live game"); });
        try {
            SwingUtilities.invokeAndWait(() -> {
                ui.init(); ui.getJFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                ui.currentReplaySave = replay; ui.replayDay = 3; ui.currentScene = UI.Scene.REPLAY_PLAYER_SCENE; ui.run();
                try {
                    check(helper, ui, true); helper.capture(ui, "hunter-guard-before-co");
                    helper.click(ui, 1050, 250); helper.click(ui, 1050, 250);
                    assertEquals(5, ui.replayDay);
                    check(helper, ui, false); helper.capture(ui, "hunter-guard-after-co");
                    helper.click(ui, 1050, 400); helper.click(ui, 1050, 400);
                    assertEquals(3, ui.replayDay); check(helper, ui, true);
                    for (var snapshot : replay.daySnapshots) {
                        var hunter = snapshot.players[0];
                        hunter.deathDay = snapshot.dayNumber >= 3 ? 2 : 0;
                        hunter.deathReason = snapshot.dayNumber >= 3 ? 1 : 0;
                        hunter.skillTarget = 0; hunter.skillTargets = new int[0];
                    }
                    ui.run();
                    var pane = helper.find(ui.jPanel, JTextPane.class);
                    assertNotNull(pane); assertFalse(pane.getText().contains("青年 : "));
                    assertTrue(pane.getText().contains("研究 : "));
                    helper.capture(ui, "hunter-guard-before-action-death");
                    assertNull(ui.gameSession);
                } catch (Exception error) { throw new RuntimeException(error); }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (ui.getJFrame() != null) {
                    for (Window window : ui.getJFrame().getOwnedWindows()) window.dispose();
                    ui.getJFrame().dispose();
                }
            });
        }
    }

    private void check(DesktopBaselineTest helper, UI ui, boolean fakeUnannounced) {
        var pane = helper.find(ui.jPanel, JTextPane.class); assertNotNull(pane);
        int trueHunter = pane.getText().indexOf("青年 : ");
        int fakeHunter = pane.getText().indexOf("研究 : ");
        assertTrue(trueHunter >= 0 && fakeHunter >= 0);
        assertEquals(ReplayGuardTextPane.UNANNOUNCED, StyleConstants.getForeground(pane.getStyledDocument().getCharacterElement(trueHunter).getAttributes()));
        assertEquals(fakeUnannounced ? ReplayGuardTextPane.UNANNOUNCED : Color.BLACK,
                StyleConstants.getForeground(pane.getStyledDocument().getCharacterElement(fakeHunter).getAttributes()));
        assertEquals(new Rectangle(40, 228, 930, 430), pane.getParent().getParent().getBounds());
    }
}
