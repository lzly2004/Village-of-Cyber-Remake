package com.vocr.ui;

import com.vocr.resources.SoundEffect;
import com.vocr.resources.ImageNames;
import com.vocr.resources.UiImage;

import com.vocr.config.GameConstants;

import javax.swing.*;
import java.awt.*;

/**
 * 投票履历浏览器 —— 从 GameSceneVoteHandler.wireAllEventHandlers() 提取。
 * 负责渲染投票历史浏览面板（各日投票结果翻页）。
 */
class VoteHistoryBrowser {

    static void render(UI ui, VoteButtonPanel vp, VoteInfoRenderer vi,
                       JPanel hisPanel, JLabel levellb, GameSceneVoteHandler handler) {
        ui.resources.playSound(SoundEffect.CLICK);
        vp.doubtBtn.setVisible(false); vp.votehisBtn.setVisible(false); vi.infoText.setVisible(false);
        hisPanel.setVisible(true); hisPanel.removeAll();
        ui.jPanel.setComponentZOrder(levellb, 0); levellb.setVisible(true);
        int displayedDay = 0;
        for (int dayCursor = 2; dayCursor <= ui.ctx.getEffectiveGameDay(); ++dayCursor) {
            int gameday = dayCursor;
            int roundMax = ui.ctx.getVoteRoundCount(gameday);
            if (roundMax == 0) continue;
            int dayPosition = displayedDay++;
            JButton backResult = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 3 - 20, ui.resources.getImage(UiImage.HISTORY_BACK));
            backResult.setVisible(false); ui.jPanel.add(backResult);
            ui.jPanel.setComponentZOrder(backResult, 0);
            JButton nextResult = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 2 - 10, ui.resources.getImage(UiImage.HISTORY_NEXT));
            nextResult.setVisible(false); ui.jPanel.add(nextResult);
            ui.jPanel.setComponentZOrder(nextResult, 0);
            JButton backResult1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 3 - 20, ui.resources.getImage(UiImage.HISTORY_BACK));
            backResult1.setVisible(false); ui.jPanel.add(backResult1);
            ui.jPanel.setComponentZOrder(backResult1, 0);
            JButton nextResult1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 2 - 10, ui.resources.getImage(UiImage.HISTORY_NEXT));
            nextResult1.setVisible(false); ui.jPanel.add(nextResult1);
            ui.jPanel.setComponentZOrder(nextResult1, 0);
            nextResult.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 2, gameday, handler);
                backResult.setVisible(true); nextResult.setVisible(false);
                if (roundMax == GameConstants.MAX_VOTE_ROUNDS) nextResult1.setVisible(true);
            });
            backResult.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 1, gameday, handler);
                backResult.setVisible(false); nextResult.setVisible(true);
                nextResult1.setVisible(false);
            });
            nextResult1.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 3, gameday, handler);
                backResult1.setVisible(true); nextResult1.setVisible(false);
                backResult.setVisible(false);
            });
            backResult1.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 2, gameday, handler);
                backResult1.setVisible(false); nextResult1.setVisible(true);
                backResult.setVisible(true);
            });
            ImageIcon dayIcon = ui.resources.getImage(ImageNames.voteDay(gameday));
            JButton dayBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, dayIcon);
            dayBtn.setSize(dayIcon.getIconWidth(), dayIcon.getIconHeight());
            dayBtn.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 1, gameday, handler);
                hisPanel.setVisible(false);
                if (roundMax != 1) nextResult.setVisible(true);
            });
            if (dayPosition < 5) dayBtn.setLocation((10 + dayIcon.getIconWidth()) * dayPosition, 10);
            else if (dayPosition < 10) dayBtn.setLocation((10 + dayIcon.getIconWidth()) * (dayPosition - 5), dayIcon.getIconHeight() + 20);
            else dayBtn.setLocation((10 + dayIcon.getIconWidth()) * (dayPosition - 10), dayIcon.getIconHeight() * 2 + 30);
            hisPanel.add(dayBtn);
        }
        hisPanel.setVisible(true);
        ui.jPanel.setComponentZOrder(hisPanel, 0);
        ui.resizeComponents();
    }
}
