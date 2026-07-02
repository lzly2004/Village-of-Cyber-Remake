// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\vocr\src\ReplayBrowserHandler.java
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ReplayBrowserHandler implements SceneHandler {
    private static final int SLOTS_PER_PAGE = 3;
    private static final int SLOT_X = 90;
    private static final int SLOT_Y_START = 80;
    private static final int SLOT_WIDTH = 770;
    private static final int SLOT_HEIGHT = 170;
    private static final int SLOT_GAP = 30;

    private static final int BTN_X = 1050;
    private static final int BTN_W = 194;
    private static final int BTN_H = 126;

    private static final Color[] PEIYI_COLORS = {
            new Color(74, 144, 217),   // jianyi - 蓝
            new Color(93, 174, 90),    // tongchang - 绿
            new Color(217, 197, 74),   // yaoohu - 黄
            new Color(217, 74, 74),    // kuangxin - 红
            new Color(217, 138, 74),   // beide - 橙
            new Color(154, 74, 217),   // maoyou - 紫
            new Color(136, 136, 136)   // daxing - 灰
    };

    private boolean browserBgmSwitched = false;

    @Override
    public void render(UI ui) {
        try {
            doRender(ui);
        } catch (Exception ex) {
            DebugLogger.error("[ReplayBrowserHandler] 渲染异常: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void doRender(UI ui) {
        ui.jPanel.removeAll();

        if (!browserBgmSwitched) {
            DebugLogger.info("[ReplayBrowserHandler] 切换BGM: 纯音乐-优美旋律.wav");
            ui.resources.playBgm("纯音乐-优美旋律.wav");
            browserBgmSwitched = true;
        }

        ReplayManager manager = ui.replayManager;
        int page = ui.replayPage;

        DebugLogger.info("[ReplayBrowserHandler] 渲染存档浏览器: page=" + page +
                ", usedSlots=" + manager.getUsedSlotCount());

        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int slotIndex = page * SLOTS_PER_PAGE + i;
            if (slotIndex >= ReplayManager.MAX_SLOTS) break;

            int y = SLOT_Y_START + i * (SLOT_HEIGHT + SLOT_GAP);
            renderSlot(ui, manager, slotIndex, SLOT_X, y, SLOT_WIDTH, SLOT_HEIGHT);
        }

        JButton btnNext = createButton(ui, "PVBnext.png", BTN_X, 250, BTN_W, BTN_H);
        if (page < 2) {
            UIHelpers.setButtonListener(btnNext, ui, () -> {
                ui.replayPage++;
                ui.run();
            });
        } else {
            UIHelpers.setButtonListener(btnNext, ui, () -> {});
        }
        ui.jPanel.add(btnNext);

        if (page > 0) {
            JButton btnBack = createButton(ui, "PVBreturn.png", BTN_X, 400, BTN_W, BTN_H);
            UIHelpers.setButtonListener(btnBack, ui, () -> {
                ui.replayPage--;
                ui.run();
            });
            ui.jPanel.add(btnBack);
        }

        JButton btnTitle = createButton(ui, "PVBtitile.png", BTN_X, 550, BTN_W, BTN_H);
        UIHelpers.setButtonListener(btnTitle, ui, () -> {
            browserBgmSwitched = false;
            ui.resources.playBgm("start_menu.wav");
            ui.transitionTo(UI.Scene.START_SCENE);
        });
        ui.jPanel.add(btnTitle);

        UIHelpers.addBackground(ui, "PVBG.png");

        UIHelpers.finishRender(ui);
    }

    private void renderSlot(UI ui, ReplayManager manager, int slotIndex,
                            int x, int y, int w, int h) {
        JPanel slotPanel = new JPanel(null);
        slotPanel.setBounds(x, y, w, h);
        slotPanel.setOpaque(false);

        ReplaySave save = manager.getSlot(slotIndex);
        boolean isEmpty = (save == null);

        Color bgColor = isEmpty ? new Color(85, 85, 85) : getPeiyiColor(save.peiyiOrdinal);

        JLabel colorBar = new JLabel();
        colorBar.setBounds(0, 0, 12, h);
        colorBar.setOpaque(true);
        colorBar.setBackground(bgColor);
        slotPanel.add(colorBar);

        JLabel cardBg = LabelSimpleFactory.makeLabel(
                LabelConst.Simple_Label, 12, 0, w - 12, h,
                ui.resources.getImage("avg1_resized(3).png")
        );
        slotPanel.add(cardBg);

        if (!isEmpty) {
            JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 22, "");
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d. %s", slotIndex, save.getPeiyiDisplayName()));
            sb.append(String.format("     %s\n", save.saveTime));
            sb.append(String.format("   %d日目に  %s     %s戦目",
                    save.totalDays, save.getResultDisplayName(),
                    (save.peiyiVillageCount > 0 && save.totalVillageCount > 0) 
                            ? String.format("%d/%d", save.peiyiVillageCount, save.totalVillageCount)
                            : "-/-"));
            infoText.setText(sb.toString());
            infoText.setBounds(30, 15, w - 60, h - 30);
            infoText.setLineWrap(true);
            infoText.setWrapStyleWord(true);
            infoText.setOpaque(false);
            infoText.setEditable(false);  // 禁止编辑
            infoText.setFocusable(false);   // 禁止获取焦点，让鼠标事件穿透
            slotPanel.add(infoText);

            final int finalSlotIndex = slotIndex;
            
            // 将MouseListener绑定到infoText上（因为它覆盖在slotPanel上方）
            infoText.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        ui.resources.playSound("click.wav");
                        ui.currentReplaySave = save;
                        ui.replayDay = 1;
                        DebugLogger.info("[ReplayBrowserHandler] 点击存档进入复盘: slot=" + slotIndex);
                        ui.transitionTo(UI.Scene.REPLAY_PLAYER_SCENE);
                    }
                    else if (SwingUtilities.isRightMouseButton(e)) {
                        showConfirmDialog(ui, finalSlotIndex, save);
                    }
                }
            });
        } else {
            JTextArea emptyText = TextareaSimpleFactory.createBoldTitleTextArea(
                    Color.GRAY, 24, String.format("%d. (空槽位)", slotIndex)
            );
            emptyText.setBounds(30, (h - 40) / 2, w - 60, 40);
            emptyText.setOpaque(false);
            emptyText.setEditable(false);  // 禁止编辑
            emptyText.setFocusable(false);   // 禁止获取焦点
            slotPanel.add(emptyText);

            final int finalSlotIndex = slotIndex;
            
            // 将MouseListener绑定到emptyText上
            emptyText.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        handleEmptySlotRightClick(ui, finalSlotIndex);
                    }
                }
            });
        }

        ui.jPanel.add(slotPanel);
    }

    private boolean checkReplayDataAvailable(UI ui) {
        MainLogic mainLogic = null;
        try {
            mainLogic = (MainLogic) Game.getInstance().getMainLogic();
        } catch (Exception e) {}
        
        if (mainLogic == null || mainLogic.getRecorder() == null || 
            mainLogic.getRecorder().getRecords().isEmpty()) {
            JOptionPane.showMessageDialog(ui.getJFrame(), 
                "没有可保存的对局数据。\n请先完成一局游戏后再保存。",
                "无法保存",
                JOptionPane.INFORMATION_MESSAGE
            );
            return false;
        }
        return true;
    }

    private void saveReplayToSlot(UI ui, int slotIndex) {
        MainLogic mainLogic = null;
        try {
            mainLogic = (MainLogic) Game.getInstance().getMainLogic();
        } catch (Exception e) {}
        
        if (mainLogic == null) return;
        
        GameRecorder recorder = mainLogic.getRecorder();
        ReplaySave save = ReplaySave.fromRecorder(slotIndex, recorder);
        ui.replayManager.saveToSlot(slotIndex, save);
        recorder.endGame(recorder.isActive() ? 0 : 0, 0);
        recorder.setActive(false);
        ui.resources.playSound("click.wav");
        ui.run();
        DebugLogger.info("[ReplayBrowserHandler] 已保存到槽位: " + slotIndex);
    }

    private void handleEmptySlotRightClick(UI ui, int slotIndex) {
        if (!checkReplayDataAvailable(ui)) return;
        
        int choice = JOptionPane.showConfirmDialog(ui.getJFrame(),
            "是否将对局数据保存到槽位 " + slotIndex + "？",
            "保存确认",
            JOptionPane.YES_NO_OPTION
        );
        
        if (choice == JOptionPane.YES_OPTION) {
            saveReplayToSlot(ui, slotIndex);
        }
    }

    private void showConfirmDialog(UI ui, int slotIndex, ReplaySave existingSave) {
        if (!checkReplayDataAvailable(ui)) return;
        
        int choice = JOptionPane.showConfirmDialog(ui.getJFrame(),
                "要将存档保存在存档格 " + slotIndex + " 吗？\n（之前的存档会被覆盖）",
                "保存确认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            saveReplayToSlot(ui, slotIndex);
        }
    }

    private JButton createDialogButton(UI ui, String text, Color color, int x, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(x, y, 80, 40);
        btn.setFont(new Font("Dialog", Font.BOLD, 18));
        btn.setForeground(color);
        btn.setBackground(Color.DARK_GRAY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        return btn;
    }

    private JButton createButton(UI ui, String imageName, int x, int y, int w, int h) {
        ImageIcon img = ui.resources.getImage(imageName);
        return ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, w, h, img);
    }

    private Color getPeiyiColor(int ordinal) {
        if (ordinal >= 0 && ordinal < PEIYI_COLORS.length) {
            return PEIYI_COLORS[ordinal];
        }
        return Color.GRAY;
    }
}