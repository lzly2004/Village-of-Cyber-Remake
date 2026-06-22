import javax.swing.*;
import java.awt.*;

/**
 * 投票履历浏览器 —— 从 GameSceneVoteHandler.wireAllEventHandlers() 提取。
 * 负责渲染投票历史浏览面板（各日投票结果翻页）。
 */
class VoteHistoryBrowser {

    static void render(UI ui, VoteButtonPanel vp, VoteInfoRenderer vi,
                       JPanel hisPanel, JLabel levellb, boolean[] isVotehis,
                       GameSceneVoteHandler handler) {
        isVotehis[0] = true;
        ui.resources.playSound("click.wav");
        vp.doubtBtn.setVisible(false); vp.votehisBtn.setVisible(false); vi.infoText.setVisible(false);
        hisPanel.setVisible(true); hisPanel.removeAll();
        ui.jPanel.setComponentZOrder(levellb, 0); levellb.setVisible(true);
        for (int i = 0; i < ui.voteRounds.size(); ++i) {
            int gameday = i + 2;
            int roundMax = ui.voteRounds.get(gameday - 2);
            JButton backResult = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 3 - 20, ui.resources.getImage("rirekiBack.png"));
            backResult.setVisible(false); ui.jPanel.add(backResult);
            ui.jPanel.setComponentZOrder(backResult, 0);
            JButton nextResult = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 2 - 10, ui.resources.getImage("rirekiNext.png"));
            nextResult.setVisible(false); ui.jPanel.add(nextResult);
            ui.jPanel.setComponentZOrder(nextResult, 0);
            JButton backResult1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 3 - 20, ui.resources.getImage("rirekiBack.png"));
            backResult1.setVisible(false); ui.jPanel.add(backResult1);
            ui.jPanel.setComponentZOrder(backResult1, 0);
            JButton nextResult1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                    720 - 40 - 126 * 2 - 10, ui.resources.getImage("rirekiNext.png"));
            nextResult1.setVisible(false); ui.jPanel.add(nextResult1);
            ui.jPanel.setComponentZOrder(nextResult1, 0);
            nextResult.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 2, gameday, ui.voteMethods.get(gameday - 2), handler);
                backResult.setVisible(true); nextResult.setVisible(false);
                if (roundMax == 3) nextResult1.setVisible(true);
            });
            backResult.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 1, gameday, ui.voteMethods.get(gameday - 2), handler);
                backResult.setVisible(false); nextResult.setVisible(true);
                nextResult1.setVisible(false);
            });
            nextResult1.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 3, gameday, ui.voteMethods.get(gameday - 2), handler);
                backResult1.setVisible(true); nextResult1.setVisible(false);
                backResult.setVisible(false);
            });
            backResult1.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 2, gameday, ui.voteMethods.get(gameday - 2), handler);
                backResult1.setVisible(false); nextResult1.setVisible(true);
                backResult.setVisible(true);
            });
            ImageIcon dayIcon = ui.resources.getImage(gameday + "day.png");
            JButton dayBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, dayIcon);
            dayBtn.setSize(dayIcon.getIconWidth(), dayIcon.getIconHeight());
            dayBtn.addActionListener(e1 -> {
                VoteResultRenderer.renderDayPiao(ui, 1, gameday, ui.voteMethods.get(gameday - 2), handler);
                hisPanel.setVisible(false);
                if (roundMax != 1) nextResult.setVisible(true);
            });
            if (i < 5) dayBtn.setLocation((10 + dayIcon.getIconWidth()) * i, 10);
            else if (i < 10) dayBtn.setLocation((10 + dayIcon.getIconWidth()) * (i - 5), dayIcon.getIconHeight() + 20);
            else dayBtn.setLocation((10 + dayIcon.getIconWidth()) * (i - 10), dayIcon.getIconHeight() * 2 + 30);
            hisPanel.add(dayBtn);
        }
        hisPanel.setVisible(true);
        ui.jPanel.setComponentZOrder(hisPanel, 0);
        ui.resizeComponents();
    }
}