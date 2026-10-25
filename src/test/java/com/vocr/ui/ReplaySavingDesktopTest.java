package com.vocr.ui;

import com.vocr.core.SeededGuestSession;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplayManager;
import com.vocr.resources.ResourcesInterface;
import com.vocr.resources.UiImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

/** Real Swing frame + actual right-click confirmation path, isolated file and muted audio. */
class ReplaySavingDesktopTest {
    @TempDir Path directory;
    private static <T> T find(Container parent, Class<T> type, java.util.function.Predicate<T> matches) {
        for (Component child:parent.getComponents()) {
            if (type.isInstance(child) && matches.test(type.cast(child))) return type.cast(child);
            if (child instanceof Container container) {
                T result=find(container,type,matches);if(result!=null)return result;
            }
        }
        return null;
    }
    @Test @Timeout(15)
    void realWindowRightClickSavesThroughApplicationAndRendersTheStoredSlot() throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless(),"this smoke must actually show its own Swing frame");
        var constructor=ReplayManager.class.getDeclaredConstructor(String.class);constructor.setAccessible(true);
        var manager=constructor.newInstance(directory.resolve("replay.json").toString());
        var cache=new HashMap<String,ImageIcon>();
        var resources=(ResourcesInterface)Proxy.newProxyInstance(ResourcesInterface.class.getClassLoader(),new Class<?>[]{ResourcesInterface.class},(proxy,method,args)->{
            if(method.getName().equals("getImage")) {
                String path=args[0] instanceof UiImage image ? image.resourcePath() : "/images/"+args[0];
                return cache.computeIfAbsent(path,p->{var url=getClass().getResource(p);assertNotNull(url,p);return new ImageIcon(url);});
            }
            if(method.getName().equals("playBgm")||method.getName().equals("playSound"))return true;
            if(method.getReturnType()==void.class)return null;
            throw new UnsupportedOperationException(method.getName());
        });
        var ui=new UI(resources,manager,GameRecordManager.inMemory());
        ui.attachSessionFactory(()->SeededGuestSession.create("desktop-smoke",4201));
        try {
            SwingUtilities.invokeAndWait(()->{
                ui.init();ui.run();assertTrue(ui.getJFrame().isShowing());
                ui.startNewGame(peiyi.jianyi);
                ui.currentScene=UI.Scene.REPLAY_BROWSER_SCENE;ui.run();
                var empty=find(ui.jPanel,JTextArea.class,t->t.getText().startsWith("0. (空槽位)"));
                assertNotNull(empty);
                var timer=new Timer(50,null);
                long deadline=System.nanoTime()+2_000_000_000L;
                timer.addActionListener(event->{
                    for(Window window:ui.getJFrame().getOwnedWindows()) {
                        if(window instanceof JDialog dialog && dialog.isVisible()) {
                            var pane=find(dialog,JOptionPane.class,p->true);
                            if(pane!=null){pane.setValue(JOptionPane.YES_OPTION);timer.stop();return;}
                        }
                    }
                    if(System.nanoTime()>deadline){timer.stop();for(Window w:ui.getJFrame().getOwnedWindows())w.dispose();}
                });
                timer.start();
                try {empty.dispatchEvent(new MouseEvent(empty,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,5,5,1,true,MouseEvent.BUTTON3));}
                finally {timer.stop();}
                assertNotNull(manager.getSlot(0),"real confirmation must save the slot");
                assertEquals(peiyi.jianyi.name(),manager.getSlot(0).peiyiName);
                assertNotNull(find(ui.jPanel,JTextArea.class,t->t.getText().startsWith("0. 简易村")));
                var frame=ui.getJFrame();
                var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
                var graphics=image.createGraphics();frame.paintAll(graphics);graphics.dispose();
                try {var output=Path.of("build/web031/desktop-replay.png");Files.createDirectories(output.getParent());ImageIO.write(image,"png",output.toFile());}
                catch(java.io.IOException error){throw new AssertionError(error);}
            });
            var loaded=constructor.newInstance(directory.resolve("replay.json").toString());
            assertEquals(manager.getSlot(0).rawRecords,loaded.getSlot(0).rawRecords);
        } finally {
            SwingUtilities.invokeAndWait(()->{if(ui.getJFrame()!=null){for(Window w:ui.getJFrame().getOwnedWindows())w.dispose();ui.getJFrame().dispose();}});
        }
    }
}
