package com.vocr.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplayManager;
import com.vocr.resources.Resources;
import com.vocr.resources.ResourcesInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in source scene capture: no live sessions, temporary Replay, in-memory ledger, no audio. */
class DesktopInfoRecordBaselineTest {
    @TempDir Path temporary;
    @Test @Timeout(60)
    void captureActualInfoAndRecordHandlers() throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());
        var helper=new DesktopBaselineTest();Files.createDirectories(helper.output);
        var base=helper.resources();var texts=new Resources();
        var resources=(ResourcesInterface)Proxy.newProxyInstance(ResourcesInterface.class.getClassLoader(),new Class<?>[]{ResourcesInterface.class},(proxy,method,args)->{
            if(method.getName().equals("getHelpText"))return texts.getHelpText((String)args[0]);
            return method.invoke(base,args);
        });
        var constructor=ReplayManager.class.getDeclaredConstructor(String.class);constructor.setAccessible(true);
        var record=GameRecordManager.inMemory();record.record(peiyi.jianyi,GameResult.VILLAGE_WIN);
        var ui=new UI(resources,constructor.newInstance(temporary.resolve("replays.json").toString()),record);
        ui.attachSessionFactory(()->{throw new AssertionError("Info/Record must not create a game");});
        try {
            SwingUtilities.invokeAndWait(()->{
                try {
                    ui.init();ui.getJFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    ui.currentScene=UI.Scene.RECORD_SCENE;ui.recordPage=0;ui.run();helper.capture(ui,"record-overall");
                    helper.click(ui,1050,250);assertEquals(1,ui.recordPage);helper.capture(ui,"record-jianyi");
                    helper.click(ui,1050,400);assertEquals(0,ui.recordPage);
                    ui.currentScene=UI.Scene.INFO_SCENE;ui.run();helper.capture(ui,"info-root");
                    helper.click(ui,80,80);assertEquals(UI.Scene.INFO_SCENE_1,ui.currentScene);helper.capture(ui,"info-group");
                    helper.click(ui,80,80);assertEquals(UI.Scene.INFO_SCENE_1_1,ui.currentScene);helper.capture(ui,"info-topic");
                    helper.click(ui,1050,400);assertEquals(UI.Scene.INFO_SCENE_1,ui.currentScene);
                    helper.click(ui,1050,400);assertEquals(UI.Scene.INFO_SCENE,ui.currentScene);
                    helper.click(ui,1050,560);assertEquals(UI.Scene.START_SCENE,ui.currentScene);
                }catch(Exception error){throw new RuntimeException(error);}
            });
            var proof=new LinkedHashMap<String,Object>();proof.put("source",System.getProperty("vocr.baseline.source"));proof.put("scenes",helper.scenes);
            proof.put("data","one jianyi village win, in-memory ledger; temporary Replay; no live game");
            proof.put("audio","capture proxy disables audio; source listener inspection supplies sound contract, not listening evidence");
            proof.put("language","actual bundled Chinese help sources; Web Japanese summaries remain an explicit translation difference");
            proof.put("declaredFont",UIConstants.FONT_FAMILY);proof.put("declaredFontInstalled",Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)).contains(UIConstants.FONT_FAMILY));
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(helper.output.resolve("info-record-desktop.json").toFile(),proof);
            assertEquals(5,helper.scenes.size());assertNull(ui.gameSession);
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.getJFrame()!=null){for(Window w:ui.getJFrame().getOwnedWindows())w.dispose();ui.getJFrame().dispose();}});}
    }
}
