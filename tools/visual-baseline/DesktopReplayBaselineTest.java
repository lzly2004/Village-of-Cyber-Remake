package com.vocr.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplayManager;
import com.vocr.replay.ReplaySave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in real Swing Replay capture; fixed public exchange fixture, temporary slots, no audio. */
class DesktopReplayBaselineTest {
    @TempDir Path temporary;

    @Test @Timeout(60)
    void captureSourceReplayScenesWithoutUserFilesOrAudio() throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());
        var helper = new DesktopBaselineTest();
        Files.createDirectories(helper.output);
        var constructor = ReplayManager.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        var manager = constructor.newInstance(temporary.resolve("replays.json").toString());
        var mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var fixture = mapper.readValue(Files.readAllBytes(Path.of("src/test/resources/replay/exchange-v3.json")), ReplaySave[].class)[0];
        manager.saveToSlotChecked(0, fixture);
        var ui = new UI(helper.resources(), manager, GameRecordManager.inMemory());
        ui.attachSessionFactory(() -> { throw new AssertionError("Replay capture must not create a live game"); });
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ui.init(); ui.getJFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    ui.currentScene=UI.Scene.REPLAY_BROWSER_SCENE; ui.run(); helper.capture(ui,"replay-library-one");
                    ui.currentReplaySave=fixture; ui.replayDay=2;
                    ui.currentScene=UI.Scene.REPLAY_PLAYER_SCENE; ui.run(); helper.capture(ui,"replay-player-day2");
                    helper.click(ui,1050,550); helper.capture(ui,"replay-player-day2-votes");
                    helper.click(ui,1050,550); ui.replayDay=fixture.totalDays; ui.run(); helper.capture(ui,"replay-player-final");
                } catch(Exception error) { throw new RuntimeException(error); }
            });
            var proof = new LinkedHashMap<String,Object>();
            proof.put("source",System.getProperty("vocr.baseline.source"));
            proof.put("fixture","src/test/resources/replay/exchange-v3.json[0]");
            proof.put("data","temporary ReplayManager; in-memory GameRecord; no live session");
            proof.put("audio","disabled capture proxy; not listening evidence");
            proof.put("declaredFont",UIConstants.FONT_FAMILY);
            proof.put("declaredFontInstalled",Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)).contains(UIConstants.FONT_FAMILY));
            proof.put("scenes",helper.scenes);
            mapper.writerWithDefaultPrettyPrinter().writeValue(helper.output.resolve("replay-desktop.json").toFile(),proof);
            assertEquals(4,helper.scenes.size());
            assertNull(ui.gameSession);
        } finally {
            SwingUtilities.invokeAndWait(() -> { if(ui.getJFrame()!=null) { for(Window window:ui.getJFrame().getOwnedWindows())window.dispose();ui.getJFrame().dispose(); } });
        }
    }
}
