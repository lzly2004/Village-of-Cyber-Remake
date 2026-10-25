package com.vocr.ui;

import com.vocr.replay.ReplayFileExchange;
import com.vocr.replay.ReplayManager;
import com.vocr.replay.ReplaySave;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** User-selected files are parsed in isolation; only the final confirmation can reach the manager. */
final class ReplayFileDialogs {
    private ReplayFileDialogs() {}
    static void addButtons(UI ui) {
        button(ui,"导入录像文件","replay-file-import",90,()->importFile(ui));
        button(ui,"导出录像文件","replay-file-export",300,()->exportFile(ui));
    }
    private static void button(UI ui,String title,String name,int x,Runnable action) {
        JButton button=new JButton(title);button.setName(name);button.setBounds(x,20,190,40);
        button.addActionListener(event->{
            try {action.run();}
            catch(Exception error) {
                String code=error instanceof FileFailure f?f.code:"FILE_OPERATION_FAILED";
                JOptionPane.showMessageDialog(ui.getJFrame(),"录像文件操作未完成（"+code+"）。\n未自动修复或重建存档，请检查文件或存储后重试。","录像文件错误",JOptionPane.ERROR_MESSAGE);
            }
        });
        ui.jPanel.add(button);ui.jPanel.setComponentZOrder(button,0);
    }
    private static final class FileFailure extends RuntimeException {
        final String code;
        FileFailure(Exception cause){super(cause);code=cause instanceof ReplayFileExchange.Problem p?p.code():"FILE_IO_FAILED";}
    }
    private static JFileChooser chooser(String title) {
        var chooser=new JFileChooser();chooser.setDialogTitle(title);chooser.setFileFilter(new FileNameExtensionFilter("Replay JSON (*.json)","json"));return chooser;
    }
    private static void importFile(UI ui) {
        var chooser=chooser("选择要导入的录像");
        if(chooser.showOpenDialog(ui.getJFrame())!=JFileChooser.APPROVE_OPTION)return;
        try {
            var candidates=ReplayFileExchange.read(chooser.getSelectedFile().toPath());
            var labels=new ArrayList<String>();
            for(var c:candidates){var r=c.replay();labels.add("文件位置 "+c.fileSlot()+" · "+r.getPeiyiDisplayName()+" · "+r.totalDays+"日 · "+r.getResultDisplayName()+" · v"+c.sourceVersion()+(c.readOnly()?"（未完，只读资料）":""));}
            Object selected=JOptionPane.showInputDialog(ui.getJFrame(),"已完整校验文件。选择一局；文件位置不会决定目标槽位。","录像预览",JOptionPane.QUESTION_MESSAGE,null,labels.toArray(),labels.getFirst());
            if(selected==null)return;
            var candidate=candidates.get(labels.indexOf(selected));
            if(candidate.readOnly()) {
                JOptionPane.showMessageDialog(ui.getJFrame(),"此录像未完，仅作只读资料。不会写入槽位、恢复对局或累计战绩。","未完录像",JOptionPane.INFORMATION_MESSAGE);return;
            }
            String[] targets=new String[ReplayManager.MAX_SLOTS];
            for(int i=0;i<targets.length;i++)targets[i]="槽位 "+i+ (ui.replayManager.getSlot(i)==null?"（空）":"（已有录像，确认后覆盖）");
            Object target=JOptionPane.showInputDialog(ui.getJFrame(),"请选择本地目标槽位。取消不写入。","选择目标槽位",JOptionPane.QUESTION_MESSAGE,null,targets,null);
            if(target==null)return;
            int slot=List.of(targets).indexOf(target);
            if(JOptionPane.showConfirmDialog(ui.getJFrame(),"将文件中的第 "+candidate.fileSlot()+" 局写入本地槽位 "+slot+"？\n"+(ui.replayManager.getSlot(slot)==null?"写入空槽位。":"原槽位录像将被覆盖。"),"确认写入录像",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            ui.replayManager.saveToSlotChecked(slot,candidate.replay());ui.run();
        }catch(Exception error){throw new FileFailure(error);}
    }
    private static void exportFile(UI ui) {
        try {
            var saves=new ArrayList<ReplaySave>();var labels=new ArrayList<String>();
            for(int i=0;i<ReplayManager.MAX_SLOTS;i++){var r=ui.replayManager.getSlot(i);if(r!=null){saves.add(r);labels.add("槽位 "+i+" · "+r.getPeiyiDisplayName()+" · "+r.saveTime);}}
            if(saves.isEmpty()){JOptionPane.showMessageDialog(ui.getJFrame(),"没有可导出的录像。","导出录像",JOptionPane.INFORMATION_MESSAGE);return;}
            Object selected=JOptionPane.showInputDialog(ui.getJFrame(),"选择一局导出为九槽文件的第0格。原槽位不变。","选择导出录像",JOptionPane.QUESTION_MESSAGE,null,labels.toArray(),labels.getFirst());
            if(selected==null)return;
            byte[] data=ReplayFileExchange.encode(List.of(saves.get(labels.indexOf(selected))));
            var chooser=chooser("保存导出的录像");
            chooser.setSelectedFile(new java.io.File("vocr-replay-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+".json"));
            if(chooser.showSaveDialog(ui.getJFrame())!=JFileChooser.APPROVE_OPTION)return;
            Path path=chooser.getSelectedFile().toPath();boolean exists=Files.exists(path,LinkOption.NOFOLLOW_LINKS);
            if(exists && JOptionPane.showConfirmDialog(ui.getJFrame(),"目标文件已存在，是否覆盖？","确认覆盖导出文件",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            ReplayFileExchange.write(path,data,exists);
        }catch(Exception error){throw new FileFailure(error);}
    }
}
