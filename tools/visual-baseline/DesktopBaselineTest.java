package com.vocr.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.*;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.*;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplayManager;
import com.vocr.resources.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in rendering probe: real Swing handlers and images, isolated state, audio deliberately disabled.
 * PNGs are observations, never automatically approved visual goldens. */
class DesktopBaselineTest {
    @TempDir Path temporary;
    final Path output=Path.of("build/web050/desktop-parity-baseline");
    final List<Map<String,Object>> scenes=new ArrayList<>();
    final Map<String,ImageIcon> images=new HashMap<>();

    ResourcesInterface resources() {
        var actual=new Resources();actual.init();
        return (ResourcesInterface)Proxy.newProxyInstance(ResourcesInterface.class.getClassLoader(),new Class<?>[]{ResourcesInterface.class},(proxy,method,args)->{
            if(method.getName().equals("getImage")) {
                String path=args[0] instanceof UiImage image?image.resourcePath():"/images/"+args[0];
                return images.computeIfAbsent(path,p->{var url=getClass().getResource(p);assertNotNull(url,p);return new ImageIcon(url);});
            }
            if(method.getName().equals("getEventText"))return actual.getEventText((com.vocr.domain.Event)args[0]);
            if(method.getName().equals("getEventImage"))return actual.getEventImage((com.vocr.domain.Event)args[0]);
            if(method.getName().equals("getCurrentBgm"))return null;
            if(method.getReturnType()==boolean.class)return true;
            if(method.getReturnType()==void.class)return null;
            throw new UnsupportedOperationException(method.getName());
        });
    }
    void components(Component c,String path,List<Map<String,Object>> out) {
        var row=new LinkedHashMap<String,Object>();row.put("path",path);row.put("class",c.getClass().getSimpleName());
        var b=c.getBounds();row.put("bounds",List.of(b.x,b.y,b.width,b.height));row.put("visible",c.isVisible());
        if(c.getFont()!=null){var f=c.getFont();row.put("font",Map.of("name",f.getName(),"family",f.getFamily(Locale.ROOT),"size",f.getSize(),"style",f.getStyle(),"displaySampleMissingIndex",f.canDisplayUpTo("村人・占い師・第2日目 äü")));}
        if(c instanceof JTextArea a)row.put("text",a.getText());
        if(c instanceof Container parent){int i=0;for(var child:parent.getComponents())components(child,path+"/"+(i++),out);}
        out.add(row);
    }
    void capture(UI ui,String name)throws Exception {
        var window=ui.getJFrame();window.validate();assertTrue(window.isShowing());
        for(var entry:Map.of(name+"-window.png",(Component)window,name+"-client.png",window.getContentPane()).entrySet()) {
            var c=entry.getValue();assertTrue(c.getWidth()>0&&c.getHeight()>0);
            var image=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();c.paintAll(g);g.dispose();ImageIO.write(image,"png",output.resolve(entry.getKey()).toFile());
        }
        var list=new ArrayList<Map<String,Object>>();components(window,"window",list);
        var in=window.getInsets();var tx=window.getGraphicsConfiguration().getDefaultTransform();
        scenes.add(Map.of("name",name,"window",List.of(window.getWidth(),window.getHeight()),"client",List.of(window.getContentPane().getWidth(),window.getContentPane().getHeight()),"insets",List.of(in.top,in.left,in.bottom,in.right),"scale",List.of(tx.getScaleX(),tx.getScaleY()),"components",list));
    }
    <T> T find(Container c,Class<T> type) {for(var child:c.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof Container inner){T found=find(inner,type);if(found!=null)return found;}}return null;}

    void publicSnapshot(UI ui,String name)throws Exception {
        var current=ui.gameSession.currentUpdate();
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve(name+"-public.json").toFile(),ui.gameSession.currentUpdate(current.nextEventCursor()));
    }

    void click(UI ui,int x,int y) {
        for(Component child:ui.jPanel.getComponents()) {
            if(child instanceof JButton button && button.isVisible() && button.getX()==x && button.getY()==y){button.doClick();return;}
        }
        throw new AssertionError("Visible desktop button missing at "+x+","+y);
    }

    @Test @Timeout(60)
    void captureActualDesktopHandlersWithoutUserFilesOrAudio()throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());Files.createDirectories(output);
        var ctor=ReplayManager.class.getDeclaredConstructor(String.class);ctor.setAccessible(true);
        var ui=new UI(resources(),ctor.newInstance(temporary.resolve("replays.json").toString()),GameRecordManager.inMemory());
        ui.attachSessionFactory(()->SeededGuestSession.create("web050-reference",50050));
        try {
            SwingUtilities.invokeAndWait(()->{
                try {
                    ui.init();ui.getJFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);ui.run();capture(ui,"title");
                    ui.currentScene=UI.Scene.GAME_SCENE_SELECT;ui.run();capture(ui,"configuration");
                    ui.startNewGame(peiyi.jianyi);int n=ui.ctx.getPlayerSum();
                    ui.skillTargetPeople=new int[n+1][com.vocr.config.GameConstants.MAX_GAME_DAYS];
                    ui.skillTargetNames=new String[n+1][com.vocr.config.GameConstants.MAX_GAME_DAYS];
                    ui.skillTargetOrder=new int[n+1][com.vocr.config.GameConstants.MAX_GAME_DAYS];
                    ui.claimedRolenum=new int[n+1][com.vocr.config.GameConstants.MAX_GAME_DAYS];
                    ui.levelName=ImageNames.gameSetupTitle(peiyi.jianyi.ordinal());
                    while(ui.hasPendingEvents()&&!ui.hasPendingEvents(EventPresentationStage.DAY_DIALOGUE))ui.pollPresentedEvent();
                    assertTrue(ui.hasPendingEvents(EventPresentationStage.DAY_DIALOGUE));
                    ui.currentScene=UI.Scene.DIALOGUE_DAY;ui.run();
                    // First click completes typewriter text; it cannot advance this newly mounted dialogue.
                    var text=find(ui.diaPanel,JTextArea.class);assertNotNull(text);assertEquals("",text.getText());
                    var next=find(ui.diaPanel,JButton.class);assertNotNull(next);
                    var following=ui.peekPresentedEvent();int pending=ui.pendingEventCount();
                    next.doClick();assertFalse(text.getText().isEmpty());
                    assertEquals(following,ui.peekPresentedEvent());assertEquals(pending,ui.pendingEventCount());
                    capture(ui,"dialogue");
                    while(ui.hasPendingEvents())ui.pollPresentedEvent();
                    ui.currentScene=UI.Scene.GAME_SCENE_VOTE;ui.run();capture(ui,"vote");publicSnapshot(ui,"vote");
                    click(ui,1060,282);capture(ui,"orders-menu");
                    click(ui,1060,282);capture(ui,"claims-menu");
                    click(ui,1060,554);
                    click(ui,1060,282);click(ui,1060,418);capture(ui,"designation-menu");
                    click(ui,1060,282);capture(ui,"vote-selection");
                    click(ui,1060,554);click(ui,1060,554);capture(ui,"vote-confirm");
                    click(ui,1060,554);
                    click(ui,1060,282);click(ui,1060,418);click(ui,1060,418);capture(ui,"divination-selection");
                    click(ui,1060,554);click(ui,1060,282);click(ui,1060,418);click(ui,1060,554);capture(ui,"guard-selection");
                    click(ui,1060,554);click(ui,1060,282);click(ui,1060,282);click(ui,1060,146);capture(ui,"claim-selection");click(ui,1060,554);
                    var session=ui.gameSession;session.setAvoidance(new SetAvoidanceCommand(session.currentRevision(),false));int rounds=0;
                    while(session.state()!=GameSession.State.FINISHED) {
                        assertTrue(++rounds<=40);var ctx=session.contextView();int target=0;
                        for(int p=1;p<=ctx.getPlayerSum();p++)if(ctx.isAlive(p)&&ctx.getActualRole(p)!=Role.gong.ordinal()){target=p;break;}
                        if(target==0)for(int p=1;p<=ctx.getPlayerSum();p++)if(ctx.isAlive(p)){target=p;break;}
                        session.setTargetSelection(new SetAllTargetSelectionCommand(session.currentRevision(),SelectionType.VOTE,target,true));
                        assertTrue(session.executeVote(new ExecuteVoteCommand(session.currentRevision(),VotingMethod.DESIGNATED,List.of(target))).voteCompleted());
                    }
                    ui.ctx=session.contextView();ui.currentScene=UI.Scene.END_ANIME;ui.run();capture(ui,"settlement");publicSnapshot(ui,"settlement");
                }catch(Exception e){throw new RuntimeException(e);}
            });
            var root=new LinkedHashMap<String,Object>();root.put("source",System.getProperty("vocr.baseline.source"));root.put("java",System.getProperty("java.runtime.version"));root.put("os",System.getProperty("os.name"));root.put("dpi",Toolkit.getDefaultToolkit().getScreenResolution());root.put("declaredFont",UIConstants.FONT_FAMILY);root.put("declaredFontInstalled",Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)).contains(UIConstants.FONT_FAMILY));root.put("audio","disabled in capture proxy; no listening claim");root.put("data","temporary Replay and in-memory record; deterministic test session");root.put("scenes",scenes);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("desktop.json").toFile(),root);
            assertEquals(13,scenes.size());
        } finally {SwingUtilities.invokeAndWait(()->{if(ui.getJFrame()!=null){for(Window w:ui.getJFrame().getOwnedWindows())w.dispose();ui.getJFrame().dispose();}});if(ui.gameSession!=null)ui.gameSession.close();}
    }
}
