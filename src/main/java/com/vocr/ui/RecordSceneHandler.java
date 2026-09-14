package com.vocr.ui;

import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;

import com.vocr.logging.DebugLogger;

import com.vocr.history.GameRecord;

// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\src\main\java\com\vocr\RecordSceneHandler.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RecordSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        try {
            doRender(ui);
        } catch (Exception ex) {
            DebugLogger.error("[RecordSceneHandler] 渲染异常", ex);
            ui.jPanel.removeAll();
            JTextArea errorMsg = TextareaSimpleFactory.createBoldTitleTextArea(Color.RED, 24, "战绩加载失败，请返回主菜单");
            errorMsg.setBounds(400, 300, 500, 40);
            ui.jPanel.add(errorMsg);
            JButton fallbackBtn = ButtonSimpleFactory.makeButton(
                    ButtonConst.Simple_Button, 530, 450, 194, 126,
                    ui.resources.getImage(UiImage.TITLE_RETURN)
            );
            fallbackBtn.addActionListener(ev -> ui.transitionTo(UI.Scene.START_SCENE));
            ui.jPanel.add(fallbackBtn);
            // 背景图片（放在最后，确保在最底层）
            ImageIcon fallbackBg = ui.resources.getImage(UiImage.TITLE_BACKGROUND);
            if (fallbackBg != null) {
                JLabel errorBg = LabelSimpleFactory.makeLabel(
                        LabelConst.Simple_Label, 0, 0,
                        UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT,
                        fallbackBg
                );
                ui.jPanel.add(errorBg);
            }
            ui.resizeComponents();
            ui.jPanel.revalidate();
            ui.jPanel.repaint();
            ui.getJFrame().setVisible(true);
        }
    }

    private void doRender(UI ui) {
        ui.jPanel.removeAll();
        DebugLogger.info("[RecordSceneHandler] 开始渲染战绩页面, page=" + ui.recordPage);
        GameRecord record = ui.gameRecordManager.getRecord();
        int page = ui.recordPage; // 0=総合, 1~7=各配役村

        // ====== 左上角：标题 ======
        String titleImageName = (page == 0) ? "gameTitle0.png" : "gameTitle" + page + ".png";
        addImageLabel(ui, titleImageName, 30, 20, 200, 60);

        // ====== 左侧：数据面板 ======
        int dataX = 35;
        int labelY = 120;
        int valueY = 175;
        int rowGap = 115;

        if (page == 0) {
            // 総合页
            renderDataRow(ui, dataX, labelY, valueY, UiImage.RECORD_PLAY_COUNT, record.totalPlayCnt + "回");
            renderDataRow(ui, dataX, labelY + rowGap, valueY + rowGap, UiImage.RECORD_WIN_RATE,
                    String.format("%.5f%%", record.totalWinRate));
            renderStreakRow(ui, dataX, labelY + rowGap * 2, valueY + rowGap * 2,
                    UiImage.RECORD_MAX_STREAK, record.totalMaxStreak);
            renderFactionRows(ui, dataX, labelY + rowGap * 3, valueY + rowGap * 3,
                    record.totalVillageWin, record.totalWolfWin, record.totalFoxWin);
        } else {
            // 各村页
            renderDataRow(ui, dataX, labelY, valueY, UiImage.RECORD_PLAY_COUNT, record.playcnt[page] + "回");
            renderDataRow(ui, dataX, labelY + rowGap, valueY + rowGap, UiImage.RECORD_WIN_RATE,
                    String.format("%.5f%%", record.winrate[page]));
            renderStreakRow(ui, dataX, labelY + rowGap * 2, valueY + rowGap * 2,
                    UiImage.RECORD_MAX_STREAK, record.maxStreak[page], record.currentStreak[page]);
            renderFactionRows(ui, dataX, labelY + rowGap * 3, valueY + rowGap * 3,
                    record.villageWincnt[page], record.wolfWincnt[page], record.foxWincnt[page]);
        }

        // ====== 右侧：按钮 ======
        int btnX = 1050;
        int btnW = 194;
        int btnH = 126;

        // 次へ
        addButton(ui, UiImage.NAVIGATION_NEXT, btnX, 250, btnW, btnH, ev -> {
            ui.resources.playSound(SoundEffect.CLICK);
            ui.recordPage = (ui.recordPage + 1) % 8;
            ui.run();
        });

        // 戻る（总览页隐藏）
        if (page > 0) {
            addButton(ui, UiImage.NAVIGATION_RETURN, btnX, 400, btnW, btnH, ev -> {
                ui.resources.playSound(SoundEffect.CLICK);
                ui.recordPage = 0;
                ui.run();
            });
        }

        // タイトル
        addButton(ui, UiImage.TITLE_RETURN, btnX, 550, btnW, btnH,
                ev -> ui.transitionTo(UI.Scene.START_SCENE));

        // 背景图片（放在最后，确保在最底层）
        ImageIcon bgImg = ui.resources.getImage(UiImage.TITLE_BACKGROUND);
        if (bgImg != null) {
            JLabel background = LabelSimpleFactory.makeLabel(
                    LabelConst.Simple_Label, 0, 0,
                    UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT,
                    bgImg
            );
            ui.jPanel.add(background);
        }

        ui.resizeComponents();
        ui.jPanel.revalidate();
        ui.jPanel.repaint();
        ui.getJFrame().setVisible(true);
    }

    private void addImageLabel(UI ui, String imageName, int x, int y, int w, int h) {
        addImageLabel(ui, ui.resources.getImage(imageName), x, y, w, h);
    }

    private void addImageLabel(UI ui, UiImage image, int x, int y, int w, int h) {
        addImageLabel(ui, ui.resources.getImage(image), x, y, w, h);
    }

    private void addImageLabel(UI ui, ImageIcon img, int x, int y, int w, int h) {
        if (img != null) {
            JLabel label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, x, y, w, h, img);
            ui.jPanel.add(label);
        }
    }

    private void addButton(UI ui, UiImage image, int x, int y, int w, int h, ActionListener listener) {
        ImageIcon img = ui.resources.getImage(image);
        if (img != null) {
            JButton btn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, w, h, img);
            btn.addActionListener(listener);
            ui.jPanel.add(btn);
        }
    }

    private void renderDataRow(UI ui, int x, int labelY, int valueY, UiImage labelImg, String valueText) {
        addImageLabel(ui, labelImg, x, labelY, 185, 38);
        JTextArea valueArea = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 34, valueText);
        valueArea.setBounds(x + 8, valueY, 240, 50);
        ui.jPanel.add(valueArea);
    }

    private void renderStreakRow(UI ui, int x, int labelY, int valueY, UiImage labelImg, int maxStreak) {
        renderStreakRow(ui, x, labelY, valueY, labelImg, maxStreak, -1);
    }

    private void renderStreakRow(UI ui, int x, int labelY, int valueY, UiImage labelImg,
                                 int maxStreak, int currentStreak) {
        addImageLabel(ui, labelImg, x, labelY, 185, 38);
        String text = (currentStreak >= 0) ? maxStreak + "/Now:" + currentStreak : maxStreak + "";
        JTextArea valueArea = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 34, text);
        valueArea.setBounds(x + 8, valueY, 260, 50);
        ui.jPanel.add(valueArea);
    }

    private void renderFactionRows(UI ui, int x, int labelY, int valueY, int village, int wolf, int fox) {
        addImageLabel(ui, UiImage.RECORD_WIN_COUNT, x, labelY, 185, 38);

        int factionGap = 42;
        int factionLabelW = 70;
        int factionLabelH = 28;

        // 村人
        addImageLabel(ui, UiImage.RECORD_VILLAGE_WINS, x + 15, valueY, factionLabelW, factionLabelH);
        JTextArea villageArea = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 24, village + "回");
        villageArea.setBounds(x + 92, valueY + 2, 75, 32);
        ui.jPanel.add(villageArea);

        // 人狼
        addImageLabel(ui, UiImage.RECORD_WOLF_WINS,
                x + 15, valueY + factionGap, factionLabelW, factionLabelH);
        JTextArea wolfArea = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 24, wolf + "回");
        wolfArea.setBounds(x + 92, valueY + factionGap + 2, 75, 32);
        ui.jPanel.add(wolfArea);

        // 妖狐
        addImageLabel(ui, UiImage.RECORD_FOX_WINS,
                x + 15, valueY + factionGap * 2, factionLabelW, factionLabelH);
        JTextArea foxArea = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 24, fox + "回");
        foxArea.setBounds(x + 92, valueY + factionGap * 2 + 2, 75, 32);
        ui.jPanel.add(foxArea);
    }
}
