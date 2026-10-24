package com.vocr.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.application.*;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.*;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.*;
import com.vocr.resources.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/** Own visible Swing frame, real file choosers and confirmation dialogs; no keyboard or global process control. */
class ReplayFileDesktopTest {
    @TempDir Path directory;
    private static <T> T find(Container parent,Class<T> type,java.util.function.Predicate<T> match) {
        for(Component child:parent.getComponents()) {
            if(type.isInstance(child)&&match.test(type.cast(child)))return type.cast(child);
            if(child instanceof Container container){T result=find(container,type,match);if(result!=null)return result;}
        }return null;
    }
    private byte[] resource(String name)throws Exception {try(var input=getClass().getResourceAsStream("/replay/"+name)){assertNotNull(input);return input.readAllBytes();}}
    private ReplayManager manager(Path file)throws Exception {var c=ReplayManager.class.getDeclaredConstructor(String.class);c.setAccessible(true);return c.newInstance(file.toString());}
    private UI ui(ReplayManager manager) {
        var cache=new HashMap<String,ImageIcon>();
        var resources=(ResourcesInterface)Proxy.newProxyInstance(ResourcesInterface.class.getClassLoader(),new Class<?>[]{ResourcesInterface.class},(proxy,method,args)->{
            if(method.getName().equals("getImage")){
                String path=args[0] instanceof UiImage image?image.resourcePath():"/images/"+args[0];
                return cache.computeIfAbsent(path,p->{var url=getClass().getResource(p);assertNotNull(url,p);return new ImageIcon(url);});
            }
            if(method.getName().equals("playBgm")||method.getName().equals("playSound"))return true;
            if(method.getReturnType()==void.class)return null;throw new UnsupportedOperationException(method.getName());
        });
        var ui=new UI(resources,manager,GameRecordManager.inMemory());ui.attachSessionFactory(()->SeededGuestSession.create("desktop035-public-fixture",35035));return ui;
    }
    private static void screenshot(Window window,String name) {
        var image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();window.paintAll(graphics);graphics.dispose();
        try{Path path=Path.of("build/web035",name);Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());}catch(Exception e){throw new AssertionError(e);}
    }
    private static void click(UI ui,String name) {var button=find(ui.jPanel,JButton.class,b->name.equals(b.getName()));assertNotNull(button,name);button.doClick();}
    private static void dialogs(UI ui,List<Consumer<JDialog>> steps,Runnable action) {
        var index=new AtomicInteger();var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        AWTEventListener listener=event->{
            if(event instanceof WindowEvent w && w.getID()==WindowEvent.WINDOW_OPENED && w.getWindow() instanceof JDialog dialog){
                Window owner=dialog;while(owner!=null && owner!=ui.getJFrame())owner=owner.getOwner();
                if(owner!=ui.getJFrame())return;
                SwingUtilities.invokeLater(()->{
                    try{int step=index.getAndIncrement();assertTrue(step<steps.size(),dialog.getTitle());steps.get(step).accept(dialog);}
                    catch(Throwable error){failure.set(error);dialog.dispose();}
                });
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(listener,AWTEvent.WINDOW_EVENT_MASK);
        try{action.run();if(failure.get()!=null)throw new AssertionError(failure.get());assertEquals(steps.size(),index.get());}
        finally{Toolkit.getDefaultToolkit().removeAWTEventListener(listener);}
    }
    private Consumer<JDialog> file(Path path,boolean cancel) {return dialog->{var chooser=find(dialog,JFileChooser.class,c->true);assertNotNull(chooser);chooser.setCurrentDirectory(path.toAbsolutePath().getParent().toFile());chooser.setSelectedFile(path.toFile());if(cancel)chooser.cancelSelection();else chooser.approveSelection();};}
    private Consumer<JDialog> input(String title,int index) {return dialog->{assertEquals(title,dialog.getTitle());var pane=find(dialog,JOptionPane.class,p->true);assertNotNull(pane);pane.setInputValue(pane.getSelectionValues()[index]);pane.setValue(JOptionPane.OK_OPTION);};}
    private Consumer<JDialog> value(String title,int value) {return dialog->{assertEquals(title,dialog.getTitle());find(dialog,JOptionPane.class,p->true).setValue(value);};}
    private static void close(UI ui)throws Exception {SwingUtilities.invokeAndWait(()->{if(ui.getJFrame()!=null){for(Window w:ui.getJFrame().getOwnedWindows())w.dispose();ui.getJFrame().dispose();}});if(ui.gameSession!=null)ui.gameSession.close();}
    @Test @Timeout(30)
    void actualCoreGameIsSavedByDesktopRightClickThenExportedByRealFileChooser()throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());var manager=manager(directory.resolve("actual-slots.json"));var ui=ui(manager);
        Path output=Path.of("build/web035/desktop-actual.json").toAbsolutePath();Files.createDirectories(output.getParent());
        try{SwingUtilities.invokeAndWait(()->{
            ui.init();ui.getJFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);ui.startNewGame(peiyi.jianyi);
            var session=ui.gameSession;session.setAvoidance(new SetAvoidanceCommand(session.currentRevision(),false));
            int guard=0;
            while(session.state()!=GameSession.State.FINISHED){
                assertTrue(++guard<=40);var context=session.contextView();int target=0;
                for(int p=1;p<=context.getPlayerSum();p++)if(context.isAlive(p)&&context.getActualRole(p)!=Role.gong.ordinal()){target=p;break;}
                if(target==0)for(int p=1;p<=context.getPlayerSum();p++)if(context.isAlive(p)){target=p;break;}
                session.setTargetSelection(new SetAllTargetSelectionCommand(session.currentRevision(),SelectionType.VOTE,target,true));
                assertTrue(session.executeVote(new ExecuteVoteCommand(session.currentRevision(),VotingMethod.DESIGNATED,List.of(target))).voteCompleted());
            }
            ui.currentScene=UI.Scene.REPLAY_BROWSER_SCENE;ui.run();assertTrue(ui.getJFrame().isShowing());
            var empty=find(ui.jPanel,JTextArea.class,t->t.getText().startsWith("0. (空槽位)"));assertNotNull(empty);
            dialogs(ui,List.of(value("保存确认",JOptionPane.YES_OPTION)),()->empty.dispatchEvent(new MouseEvent(empty,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,5,5,1,true,MouseEvent.BUTTON3)));
            assertNotNull(manager.getSlot(0));assertTrue(manager.getSlot(0).endResult>0);assertEquals(session.contextView().getPlayerSum(),manager.getSlot(0).daySnapshots.getFirst().players.length);assertTrue(manager.getSlot(0).daySnapshots.getFirst().players.length>3);
            screenshot(ui.getJFrame(),"desktop-real-save.png");
            var actions=new ArrayList<Consumer<JDialog>>();actions.add(input("选择导出录像",0));actions.add(file(output,false));if(Files.exists(output))actions.add(value("确认覆盖导出文件",JOptionPane.YES_OPTION));
            dialogs(ui,actions,()->click(ui,"replay-file-export"));
        });
            assertEquals(manager.getSlot(0).rawRecords,ReplayFileExchange.read(output).getFirst().replay().rawRecords);
            assertFalse(manager.getSlot(0).rawRecords.stream().anyMatch(r->r.contains("sessionId")||r.contains("/api/v1/games/")||r.contains(System.getProperty("user.home"))));
        }finally{close(ui);}
    }
    @Test @Timeout(30)
    void realImportPreviewTargetConfirmationCancelAndReExportPreserveGoodSlots()throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());Path slots=directory.resolve("slots.json");Files.write(slots,resource("exchange-v3.json"));var manager=manager(slots);var ui=ui(manager);
        String browser=System.getenv("VOCR_WEB035_BROWSER_EXPORT");Path incoming=directory.resolve("incoming.json");
        Files.write(incoming,browser==null?resource("desktop-saved-v3.json"):Files.readAllBytes(Path.of(browser)));
        Path bad=directory.resolve("bad.json"),exported=directory.resolve("reexported.json");Files.writeString(bad,"[]");byte[] before=Files.readAllBytes(slots);
        try{SwingUtilities.invokeAndWait(()->{
            ui.init();ui.getJFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);ui.currentScene=UI.Scene.REPLAY_BROWSER_SCENE;ui.run();
            dialogs(ui,List.of(file(incoming,true)),()->click(ui,"replay-file-import"));
            dialogs(ui,List.of(file(incoming,false),value("录像预览",JOptionPane.CLOSED_OPTION)),()->click(ui,"replay-file-import"));
            dialogs(ui,List.of(file(incoming,false),input("录像预览",0),input("选择目标槽位",8),value("确认写入录像",JOptionPane.NO_OPTION)),()->click(ui,"replay-file-import"));
            dialogs(ui,List.of(file(bad,false),value("录像文件错误",JOptionPane.OK_OPTION)),()->click(ui,"replay-file-import"));
            try{assertArrayEquals(before,Files.readAllBytes(slots));assertEquals("[]",Files.readString(bad));}catch(Exception e){throw new AssertionError(e);}
            dialogs(ui,List.of(file(incoming,false),input("录像预览",0),input("选择目标槽位",8),value("确认写入录像",JOptionPane.YES_OPTION)),()->click(ui,"replay-file-import"));
            assertNotNull(manager.getSlot(8));assertEquals(8,manager.getSlot(8).slotIndex);
            ui.replayPage=2;ui.run();screenshot(ui.getJFrame(),"desktop-import-slot8.png");
            dialogs(ui,List.of(input("选择导出录像",1),file(exported,false)),()->click(ui,"replay-file-export"));
        });
            assertEquals(new ObjectMapper().readTree(before).get(0).get("rawRecords"),new ObjectMapper().valueToTree(manager.getSlot(0)).get("rawRecords"));
            var expected=ReplayFileExchange.read(incoming).getFirst().replay();var actual=ReplayFileExchange.read(exported).getFirst().replay();
            assertEquals(new ObjectMapper().valueToTree(expected),new ObjectMapper().valueToTree(actual));
            assertEquals(8,manager.getSlot(8).slotIndex);
        }finally{close(ui);}
    }
}
