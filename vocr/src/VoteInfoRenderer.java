import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * 投票信息面板渲染器 —— 从 GameSceneVoteHandler 提取。
 * 负责渲染占卜/灵能/处刑/死体/护卫等技能结果信息。
 */
class VoteInfoRenderer {

    JTextArea infoText;
    JScrollPane scrollPane;
    JLabel board;
    ImageIcon boardIcon;

    void render(UI ui) {
        JLabel data = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 880, 10,
                ui.resources.getImage("hiduke.png"));
        JTextArea dataText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 20,
                "    " + ui.ctx.getGameDay() + "日目\n 生存者:" + ui.ctx.getAliveCounter()
                        + "\n 死亡者:" + ui.ctx.getDeathCounter()
                        + "\n 吊り縄:" + (ui.ctx.getAliveCounter() - 1) / 2);
        dataText.setBounds(890, 25, 100, 130);
        ui.jPanel.add(dataText);
        ui.jPanel.add(data);
        StringBuilder zhanbu = new StringBuilder();
        StringBuilder lingneng = new StringBuilder();
        StringBuilder chuxing = new StringBuilder();
        StringBuilder shiti = new StringBuilder();
        StringBuilder lieren = new StringBuilder();
        for (int k = 1; k < ui.ctx.getGameDay(); ++k) {
            int shitiCnt = 0;
            ArrayList<Integer> shitiNum = new ArrayList<>();
            boolean hasChuxing = false;
            for (int i = 1; i <= ui.ctx.getPlayerSum(); i++) {
                if (k == 1) switch (ui.ctx.getClaimedRole(i)) {
                    case 1:
                        appendSkillResults(ui, zhanbu, i, 1);
                        break;
                    case 2:
                        appendSkillResults(ui, lingneng, i, 2);
                        break;
                    case 3:
                        GameLogicUtils.appendSkillResultLog(ui, lieren, i, 2, false);
                        break;
                }
                switch (ui.ctx.getDeathReason(i)) {
                    case whyDie.chuxing:
                        if (ui.ctx.getActualRole(i) == 10) {
                            int deviant = ui.ctx.getDeviant();
                            if (deviant > 0 && !ui.ctx.isAlive(deviant)
                                    && ui.ctx.getDeathDay(i) == k && ui.ctx.getDeathDay(deviant) < ui.ctx.getDeathDay(i)) {
                                chuxing.append(ui.getJobText(i));
                                hasChuxing = true;
                            }
                        } else if (ui.ctx.getActualRole(i) == 5) {
                        } else {
                            if (ui.ctx.getDeathDay(i) == k) {
                                chuxing.append(ui.getJobText(i));
                                hasChuxing = true;
                            }
                        }
                        break;
                    case whyDie.daymaozhou:
                        if (ui.ctx.getDeathDay(i) == k) {
                            chuxing.append(ui.getJobText(ui.ctx.getCat())).append("+");
                            chuxing.append(ui.getJobText(i)).append("(猫呪)");
                            hasChuxing = true;
                        }
                        break;
                    case whyDie.dayhouzhui:
                        if (ui.ctx.getDeathDay(i) == k) {
                            chuxing.append(ui.getJobText(ui.ctx.getFox())).append("+");
                            chuxing.append(ui.getJobText(i)).append("(後追)");
                            hasChuxing = true;
                        }
                        break;
                    default:
                        if (ui.ctx.getDeathDay(i) == k) {
                            shitiCnt++; shitiNum.add(ui.ctx.getCharacterNumber(i));
                        }
                        break;
                }
                if (i == ui.ctx.getPlayerSum() && shitiCnt == 0) {
                    shiti.append(GameStrings.PEACE_ARROW);
                }
            }
            if (hasChuxing) chuxing.append("→");
            if (shitiCnt == 1) shiti.append(ui.uiComponentFactory.getJobText(shitiNum.get(0))).append("→");
            else {
                for (int l = 0; l < shitiNum.size(); ++l)
                    shiti.append(ui.uiComponentFactory.getJobText(shitiNum.get(l))).append("+");
                shiti.setLength(shiti.length() - 1); shiti.append("→");
            }
        }
        if (shiti.length() > 2) shiti.setLength(shiti.length() - 1);
        if (chuxing.length() > 2) chuxing.setLength(chuxing.length() - 1);
        StringBuilder result = new StringBuilder();
        result.append(GameStrings.SECTION_SEER).append(zhanbu)
                .append(GameStrings.SECTION_MEDIUM).append(lingneng)
                .append(GameStrings.SECTION_EXECUTION).append(chuxing)
                .append("\n[死体]\n").append(shiti).append("\n[護衛先]\n").append(lieren);
        boardIcon = ui.resources.getImage("frame #19252.png");
        board = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 198,
                200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight(), boardIcon);
        infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 24, result.toString());
        scrollPane = new JScrollPane(infoText);
        UIHelpers.configureScrollPane(scrollPane);
        scrollPane.setBounds(40, 228, 200 + boardIcon.getIconWidth() - 80, 50 + boardIcon.getIconHeight() - 60);
    }

    private void appendSkillResults(UI ui, StringBuilder sb, int i, int startDay) {
        GameLogicUtils.appendSkillResultLog(ui, sb, i, startDay);
    }
}