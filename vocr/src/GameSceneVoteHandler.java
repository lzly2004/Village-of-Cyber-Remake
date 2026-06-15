// GameSceneVoteHandler.java - 投票主界面场景处理器
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GameSceneVoteHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.jPanel.removeAll();
        if (DebugLogger.getInstance().isEnabled()) {
            ui.testBtn();
        }
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("komorebi002yuu.png"));
        if ((ui.gs.aliveCounter - 1) / 2 == 1) {
            ImageIcon musicBtnIcon = ui.resources.getImage("musicBtn.png");
            JButton btnMusic = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 15, 35,
                    GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT, musicBtnIcon);
            btnMusic.addActionListener(e -> {
                int cur = ui.count % 4;
                switch (cur) {
                    case 0: ui.name = "真甜呢丨先来一斤吧 - ラストボス02.wav"; break;
                    case 1: ui.name = "西江紫堂 - 黒き明日、清き闇.wav"; break;
                    case 2: ui.name = "西江紫堂 - BLOOD.wav"; break;
                    case 3: ui.name = "西江紫堂 - 灯り無き眼光.wav"; break;
                }
                ui.count++;
                ui.resources.playBgm(ui.name);
            });
            ui.jPanel.add(btnMusic);
        } else {
            ImageIcon musicBtnIcon = ui.resources.getImage("musicBtn.png");
            JButton btnMusic = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 15, 35,
                    GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT, musicBtnIcon);
            btnMusic.addActionListener(e -> {
                int cur = ui.count % 13;
                switch (cur) {
                    case 0:  ui.name = "Death Impact.wav"; ui.text = "Death Impact.txt"; break;
                    case 1:  ui.name = "永久色五色.wav"; ui.text = "永久色五色.txt"; break;
                    case 2:  ui.name = "Emotionally Unstable.wav"; ui.text = "Emotionally Unstable.txt"; break;
                    case 3:  ui.name = "FELT LOVE.wav"; ui.text = "FELT LOVE.txt"; break;
                    case 4:  ui.name = "Just Complex.wav"; ui.text = "Just Complex.txt"; break;
                    case 5:  ui.name = "Peace of lost puzzle.wav"; ui.text = "Peace of lost puzzle.txt"; break;
                    case 6:  ui.name = "TEEK TEEK TEEK.wav"; ui.text = "TEEK TEEK TEEK.txt"; break;
                    case 7:  ui.name = "ダンジョン09.wav"; ui.text = "ダンジョン09.txt"; break;
                    case 8:  ui.name = "ライアービジネス.wav"; ui.text = "ライアービジネス.txt"; break;
                    case 9:  ui.name = "五月雨Vivaride.wav"; ui.text = "五月雨Vivaride.txt"; break;
                    case 10: ui.name = "村08.wav"; ui.text = "村08.txt"; break;
                    case 11: ui.name = "真甜呢丨先来一斤吧 - ダンジョン14.wav"; ui.text = "真甜呢丨先来一斤吧 - ダンジョン14.txt"; break;
                    case 12: ui.name = "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.wav"; ui.text = "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.txt"; break;
                }
                ui.count++;
                ui.resources.playBgm(ui.name);
            });
            ui.jPanel.add(btnMusic);
        }
        for (int i = 1; i <= ui.gs.gc.length - 1; i++) {
            StringBuilder xName = new StringBuilder();
            StringBuilder imageName = new StringBuilder();
            if (ui.gs.gc[i].number <= 9) imageName.append("0");
            imageName.append(ui.gs.gc[i].number);
            switch (ui.gs.gc[i].whyDie) {
                case NONE: break;
                case chuxing: imageName.append("g"); xName.append("turi.png"); break;
                case daymaozhou: imageName.append("g"); xName.append("noroi.png"); break;
                case dayhouzhui: imageName.append("g"); xName.append("atooi.png"); break;
                default: imageName.append("g"); xName.append("kami.png"); break;
            }
            imageName.append("s.png");
            String textName = ui.gs.gc[i].number + "job.png";
            StringBuilder claimedRoleName = new StringBuilder("yaku");
            StringBuilder skillTargetName = new StringBuilder("result");
            if (ui.gs.gc[i].claimedRole > 0 && ui.gs.gc[i].claimedRole < 6) {
                if (ui.gs.gc[i].claimedRole <= 3) {
                    claimedRoleName.append(ui.gs.gc[i].claimedRole).append("_")
                            .append(ui.gs.gc[i].claimedRoleorder).append(".png");
                    skillTargetName.append(ui.gs.gc[i].claimedRole).append("_")
                            .append(ui.gs.gc[i].claimedRoleorder);
                    int a = 0;
                    for (int day = 1; day < ui.gs.gameDay; day++) {
                        a = ui.gs.gc[i].skillTarget[day];
                    }
                    if (a != 0) {
                        ui.claimedRolenum[i][ui.gs.gameDay] = ui.gs.gc[i].claimedRole;
                        ui.skillTargetOrder[i][ui.gs.gameDay] = ui.gs.gc[i].claimedRoleorder;
                        if (ui.gs.gc[i].claimedRole != 3) {
                            if (a >= ui.gs.gc.length) {
                                skillTargetName.append("black.png");
                                ui.skillTargetPeople[i][ui.gs.gameDay] = (a + 1 - ui.gs.gc.length);
                            } else {
                                skillTargetName.append("white.png");
                                ui.skillTargetPeople[i][ui.gs.gameDay] = (a);
                            }
                        } else {
                            if (a >= ui.gs.gc.length) {
                                skillTargetName.append(".png");
                                ui.skillTargetPeople[i][ui.gs.gameDay] = (a + 1 - ui.gs.gc.length);
                            } else {
                                skillTargetName.append(".png");
                                ui.skillTargetPeople[i][ui.gs.gameDay] = (a);
                            }
                        }
                        ui.skillTargetNames[i][ui.gs.gameDay] = (skillTargetName.toString());
                    }
                } else {
                    claimedRoleName.append(ui.gs.gc[i].claimedRole).append(".png");
                }
                ImageIcon claimedRoleIcon = ui.resources.getImage(claimedRoleName.toString());
                JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                if (i <= ui.gs.gc.length / 2)
                    claimedRoleLabel.setBounds(160 + 64 * i, 0,
                            claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                else
                    claimedRoleLabel.setBounds(160 + 64 * (i - ((ui.gs.gc.length - 1 + 1) / 2)), 98,
                            claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                ui.jPanel.add(claimedRoleLabel);
            }
            if (!xName.isEmpty()) {
                ImageIcon deathImage = ui.resources.getImage(xName.toString());
                JLabel deathLabel = new JLabel(deathImage);
                if (i <= (ui.gs.gc.length - 1 + 1) / 2)
                    deathLabel.setBounds(165 + 64 * i, 10,
                            deathImage.getIconWidth(), deathImage.getIconHeight());
                else
                    deathLabel.setBounds(165 + 64 * (i - (ui.gs.gc.length / 2)), 108,
                            deathImage.getIconWidth(), deathImage.getIconHeight());
                ui.jPanel.add(deathLabel);
            }
            ImageIcon characterImage = ui.resources.getImage(imageName.toString());
            ImageIcon characterText = ui.resources.getImage(textName);
            JLabel label = new JLabel(characterImage);
            JLabel textLabel;
            if (i <= (ui.gs.gc.length - 1 + 1) / 2) {
                label.setBounds(160 + characterImage.getIconWidth() * i, 0,
                        characterImage.getIconWidth(), characterImage.getIconHeight());
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        175 + characterImage.getIconWidth() * i,
                        characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
            } else {
                label.setBounds(160 + characterImage.getIconWidth() * (i - ui.gs.gc.length / 2),
                        characterImage.getIconHeight(),
                        characterImage.getIconWidth(), characterImage.getIconHeight());
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        175 + characterImage.getIconWidth() * (i - ui.gs.gc.length / 2),
                        2 * characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
            }
            ui.jPanel.add(textLabel);
            ui.jPanel.add(label);
        }
        for (int k = 2; k <= ui.gs.gameDay; ++k) {
            for (int j = 1; j < ui.gs.gc.length; ++j) {
                if (ui.skillTargetPeople[j][k] == 0) continue;
                int i = ui.skillTargetPeople[j][k];
                int zynum = ui.claimedRolenum[j][k];
                String name = ui.skillTargetNames[j][k];
                int order = ui.skillTargetOrder[j][k];
                if (zynum == 3) continue;
                if (zynum == 1 && ui.gs.gc[j].dieDay != 0 && ui.gs.gc[j].dieDay < k) continue;
                if (zynum == 2 && ui.gs.gc[j].dieDay != 0 && ui.gs.gc[j].dieDay < k) continue;
                ImageIcon skillTargetIcon = ui.resources.getImage(name);
                JLabel skillTargetLabel;
                if (i <= (ui.gs.gc.length - 1 + 1) / 2) {
                    skillTargetLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            160 + 64 * (i + 1) - skillTargetIcon.getIconWidth() * zynum,
                            (order - 1) * skillTargetIcon.getIconHeight(),
                            skillTargetIcon.getIconWidth(), skillTargetIcon.getIconHeight(), skillTargetIcon);
                } else {
                    skillTargetLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            160 + 64 * (i + 1 - ((ui.gs.gc.length - 1 + 1) / 2))
                                    - skillTargetIcon.getIconWidth() * zynum,
                            98 + (order - 1) * skillTargetIcon.getIconHeight(),
                            skillTargetIcon.getIconWidth(), skillTargetIcon.getIconHeight(), skillTargetIcon);
                }
                ui.jPanel.add(skillTargetLabel);
                ui.jPanel.setComponentZOrder(skillTargetLabel, 0);
            }
        }
        JLabel data = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 880, 10,
                ui.resources.getImage("hiduke.png"));
        JTextArea dataText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 20,
                "    " + ui.gs.gameDay + "日目\n 生存者:" + ui.gs.aliveCounter
                        + "\n 死亡者:" + ui.gs.deathCounter
                        + "\n 吊り縄:" + (ui.gs.aliveCounter - 1) / 2, true);
        dataText.setBounds(890, 25, 100, 130);
        ui.jPanel.add(dataText);
        ui.jPanel.add(data);
        StringBuilder zhanbu = new StringBuilder();
        StringBuilder lingneng = new StringBuilder();
        StringBuilder chuxing = new StringBuilder();
        StringBuilder shiti = new StringBuilder();
        StringBuilder lieren = new StringBuilder();
        List<Boolean> isPeace = new ArrayList<>();
        for (int i = 1; i <= ui.gs.gc.length - 1; i++) {
            for (int j = 1; j < ui.gs.gameDay; ++j) {
                if (ui.gs.gc[i].dieDay == j) isPeace.add(false);
            }
        }
        List<Integer> peacePos = new ArrayList<>();
        for (int j = 1; j < ui.gs.gameDay; ++j) {
            int pos = 0;
            if (!isPeace.isEmpty() && isPeace.get(j - 1)) {
                for (int i = 1; i <= ui.gs.gc.length - 1; i++) {
                    if (ui.gs.gc[i].dieDay != 0 && ui.gs.gc[i].dieDay < j) pos++;
                }
                peacePos.add(pos);
            }
        }
        for (int k = 1; k < ui.gs.gameDay; ++k) {
            int shitiCnt = 0;
            ArrayList<Integer> shitiNum = new ArrayList<>();
            for (int i = 1; i <= ui.gs.gc.length - 1; i++) {
                if (k == 1) switch (ui.gs.gc[i].claimedRole) {
                    case 1:
                        zhanbu.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append(" : ");
                        for (int j = 1; j < ui.gs.gameDay; ++j) {
                            if (ui.gs.gc[i].dieDay != 0 && j >= ui.gs.gc[i].dieDay) break;
                            else {
                                if (ui.gs.gc[i].skillTarget[j] > (ui.gs.gc.length - 1)) {
                                    zhanbu.append(ui.uiComponentFactory.getJobText(
                                            ui.gs.gc[ui.gs.gc[i].skillTarget[j] - (ui.gs.gc.length - 1)].number)).append("●");
                                    zhanbu.append("→");
                                } else if ((ui.gs.gc[i].skillTarget[j] > 0)) {
                                    zhanbu.append(ui.uiComponentFactory.getJobText(
                                            ui.gs.gc[ui.gs.gc[i].skillTarget[j]].number)).append("○");
                                    zhanbu.append("→");
                                }
                            }
                        }
                        if (ui.gs.gc[i].nonHumanMarker) {
                            zhanbu.append(GameStrings.MARKER_EXPOSED); zhanbu.append("→");
                        }
                        zhanbu.setLength(zhanbu.length() - 1); zhanbu.append("\n");
                        break;
                    case 2:
                        lingneng.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append(" : ");
                        for (int j = 2; j < ui.gs.gameDay; ++j) {
                            if (ui.gs.gc[i].dieDay != 0 && j >= ui.gs.gc[i].dieDay) break;
                            else {
                                if (ui.gs.gc[i].skillTarget[j] > (ui.gs.gc.length - 1)) {
                                    lingneng.append(ui.uiComponentFactory.getJobText(
                                            ui.gs.gc[ui.gs.gc[i].skillTarget[j] - (ui.gs.gc.length - 1)].number)).append("●");
                                    lingneng.append("→");
                                } else if ((ui.gs.gc[i].skillTarget[j] > 0)) {
                                    lingneng.append(ui.uiComponentFactory.getJobText(
                                            ui.gs.gc[ui.gs.gc[i].skillTarget[j]].number)).append("○");
                                    lingneng.append("→");
                                }
                            }
                        }
                        if (ui.gs.gc[i].nonHumanMarker) {
                            lingneng.append(GameStrings.MARKER_EXPOSED); lingneng.append("→");
                        }
                        lingneng.setLength(lingneng.length() - 1); lingneng.append("\n");
                        break;
                    case 3:
                        lieren.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append(" : ");
                        for (int j = 2; j < ui.gs.gameDay; ++j) {
                            if (ui.gs.gc[i].dieDay != 0 && j >= ui.gs.gc[i].dieDay) break;
                            if (ui.gs.gc[i].skillTarget[j] != 0) {
                                lieren.append(ui.uiComponentFactory.getJobText(
                                        ui.gs.gc[ui.gs.gc[i].skillTarget[j]].number));
                                lieren.append("→");
                            }
                        }
                        if (ui.gs.gc[i].nonHumanMarker) {
                            lieren.append(GameStrings.MARKER_EXPOSED); lieren.append("→");
                        }
                        lieren.setLength(lieren.length() - 1); lieren.append("\n");
                        break;
                }
                switch (ui.gs.gc[i].whyDie) {
                    case whyDie.chuxing:
                        if (ui.gs.gc[i].actualRole == 10) {
                            for (int j = 1; j <= ui.gs.gc.length - 1; j++) {
                                if (ui.gs.gc[j].actualRole == 11 && ui.gs.gc[j].whyDie != whyDie.NONE
                                        && ui.gs.gc[i].dieDay == k && ui.gs.gc[j].dieDay < ui.gs.gc[i].dieDay) {
                                    chuxing.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append("→");
                                    break;
                                }
                            }
                        } else if (ui.gs.gc[i].actualRole == 5) {
                        } else {
                            if (ui.gs.gc[i].dieDay == k)
                                chuxing.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append("→");
                        }
                        break;
                    case whyDie.daymaozhou:
                        if (ui.gs.gc[i].dieDay == k) {
                            for (int j = 1; j <= ui.gs.gc.length - 1; j++) {
                                if (ui.gs.gc[j].actualRole == 5) {
                                    chuxing.append(ui.uiComponentFactory.getJobText(ui.gs.gc[j].number)).append("+");
                                    break;
                                }
                            }
                            chuxing.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append("(猫呪)").append("→");
                        }
                        break;
                    case whyDie.dayhouzhui:
                        if (ui.gs.gc[i].dieDay == k) {
                            for (int j = 1; j <= ui.gs.gc.length - 1; j++) {
                                if (ui.gs.gc[j].actualRole == 10) {
                                    chuxing.append(ui.uiComponentFactory.getJobText(ui.gs.gc[j].number)).append("+");
                                    break;
                                }
                            }
                            chuxing.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)).append("(後追)").append("→");
                        }
                        break;
                    default:
                        if (ui.gs.gc[i].dieDay == k) {
                            shitiCnt++; shitiNum.add(ui.gs.gc[i].number);
                        }
                        break;
                }
                if (i == ui.gs.gc.length - 1 && shitiCnt == 0) {
                    shiti.append(GameStrings.PEACE_ARROW);
                }
            }
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
        ImageIcon boardIcon = ui.resources.getImage("frame #19252.png");
        JLabel board = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 198,
                200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight(), boardIcon);
        JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 24, result.toString(), false);
        JScrollPane scrollPane = new JScrollPane(infoText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBounds(40, 228, 200 + boardIcon.getIconWidth() - 80, 50 + boardIcon.getIconHeight() - 60);
        JButton voteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("goTohyo.png"));
        ui.jPanel.add(voteBtn);
        JButton recordBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("check.png"));
        ui.jPanel.add(recordBtn);
        JButton pointBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("shiji.png"));
        ui.jPanel.add(pointBtn);
        JButton avoidBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("关闭回避.png"));
        ui.jPanel.add(avoidBtn);
        JButton avoidBtn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("开启回避.png"));
        avoidBtn1.setVisible(false);
        ui.jPanel.add(avoidBtn1);
        JButton menuBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 5 - 40, ui.resources.getImage("IntroTitle.png"));
        menuBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = UI.Scene.START_SCENE;
            ui.run();
        });
        ui.jPanel.add(menuBtn);
        JButton greyBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoGrey.png"));
        greyBtn.setVisible(false); ui.jPanel.add(greyBtn);
        JButton freeBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("tohyoFree.png"));
        freeBtn.setVisible(false); ui.jPanel.add(freeBtn);
        JButton doubtBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("checkUtagai.png"));
        doubtBtn.setVisible(false); ui.jPanel.add(doubtBtn);
        JButton votehisBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("checkTohyo.png"));
        votehisBtn.setVisible(false); ui.jPanel.add(votehisBtn);
        JButton coBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("doCO.png"));
        coBtn.setVisible(false); ui.jPanel.add(coBtn);
        JButton ppBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("doShitei.png"));
        ppBtn.setVisible(false); ui.jPanel.add(ppBtn);
        JButton returnBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("return.png"));
        returnBtn.setVisible(false); ui.jPanel.add(returnBtn);
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("nextDay.png"));
        nextBtn.setVisible(false); ui.jPanel.add(nextBtn);
        JButton againBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("goTohyo.png"));
        againBtn.setVisible(false); ui.jPanel.add(againBtn);
        JButton readyVoteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoShitei.png"));
        readyVoteBtn.setVisible(false); ui.jPanel.add(readyVoteBtn);
        JButton askCoBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("询问CO.png"));
        askCoBtn.setVisible(false); ui.jPanel.add(askCoBtn);
        JButton reiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("reiCO.png"));
        reiBtn.setVisible(false); ui.jPanel.add(reiBtn);
        JButton kariBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("kariCO.png"));
        kariBtn.setVisible(false); ui.jPanel.add(kariBtn);
        JButton uranaiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("uranaiCO.png"));
        uranaiBtn.setVisible(false); ui.jPanel.add(uranaiBtn);
        JButton kyouyuBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("kyouyuCO.png"));
        kyouyuBtn.setVisible(false); ui.jPanel.add(kyouyuBtn);
        JButton catBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126, ui.resources.getImage("catCO.png"));
        catBtn.setVisible(false); ui.jPanel.add(catBtn);
        JButton fixedVoteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoShitei.png"));
        fixedVoteBtn.setVisible(false); ui.jPanel.add(fixedVoteBtn);
        JButton fixedUranaiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("shiteiUranai.png"));
        fixedUranaiBtn.setVisible(false); ui.jPanel.add(fixedUranaiBtn);
        JButton protectBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("shiteiGoei.png"));
        protectBtn.setVisible(false); ui.jPanel.add(protectBtn);

        if (ui.isAvoid) { avoidBtn.setVisible(true); avoidBtn1.setVisible(false); }
        else { avoidBtn1.setVisible(true); avoidBtn.setVisible(false); }

        JScrollPane scrollPane1 = new JScrollPane();
        if (!scrollPane1.isAncestorOf(ui.jPanel)) ui.jPanel.add(scrollPane1);

        List<Integer> cxList = new ArrayList<>();
        List<Integer> beiZhan1 = new ArrayList<>();
        for (int j = 1; j < ui.gs.gameDay; ++j) {
            for (int k = 1; k < ui.gs.gc.length; ++k) {
                if (ui.gs.gc[k].claimedRole == 1) {
                    int num = ui.gs.gc[k].skillTarget[j];
                    if (num > ui.gs.gc.length - 1) num -= ui.gs.gc.length - 1;
                    if (!beiZhan1.contains(num)) { beiZhan1.add(num); DebugLogger.log(num); }
                }
            }
        }
        for (int i = 1; i < ui.gs.gc.length; ++i) {
            if (DebugLogger.getInstance().isEnabled())
                DebugLogger.log("已进入灰循环" + ui.uiComponentFactory.getJobText(ui.gs.gc[i].number)
                        + " " + ui.gs.gc[i].whyDie + " " + ui.gs.gc[i].claimedRole);
            if (ui.gs.gc[i].whyDie == whyDie.NONE
                    && (ui.gs.gc[i].claimedRole == 0 || ui.gs.gc[i].claimedRole == 6)) {
                if (beiZhan1.contains(i)) {
                    if (DebugLogger.getInstance().isEnabled())
                        DebugLogger.log(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number) + "被占卜过了，不是灰");
                    continue;
                }
                if (DebugLogger.getInstance().isEnabled())
                    DebugLogger.log(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number) + "是灰");
                cxList.add(i);
            }
        }
        doubtBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            doubtBtn.setVisible(false); votehisBtn.setVisible(false); infoText.setVisible(false);
            ui.createDoubt();
        });

        JPanel hisPanel = PanelSimpleFactory.createSimplePanel(0, 0, false, false);
        hisPanel.setBounds(0, 198, 200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight());
        ui.jPanel.add(hisPanel);
        hisPanel.setVisible(false);
        ImageIcon levelIcon = ui.resources.getImage(ui.levelName);
        JLabel levellb = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 700, 600, levelIcon);
        levellb.setVisible(false); ui.jPanel.add(levellb);
        boolean[] isVotehis = {false};
        votehisBtn.addActionListener(e -> {
            isVotehis[0] = true;
            ui.resources.playSound("click.wav");
            doubtBtn.setVisible(false); votehisBtn.setVisible(false); infoText.setVisible(false);
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
                    ui.createDayPiao(2, gameday, ui.voteMethods.get(gameday - 2));
                    backResult.setVisible(true); nextResult.setVisible(false);
                    if (roundMax == 3) nextResult1.setVisible(true);
                });
                backResult.addActionListener(e1 -> {
                    ui.createDayPiao(1, gameday, ui.voteMethods.get(gameday - 2));
                    backResult.setVisible(false); nextResult.setVisible(true);
                    nextResult1.setVisible(false);
                });
                nextResult1.addActionListener(e1 -> {
                    ui.createDayPiao(3, gameday, ui.voteMethods.get(gameday - 2));
                    backResult1.setVisible(true); nextResult1.setVisible(false);
                    backResult.setVisible(false);
                });
                backResult1.addActionListener(e1 -> {
                    ui.createDayPiao(2, gameday, ui.voteMethods.get(gameday - 2));
                    backResult1.setVisible(false); nextResult1.setVisible(true);
                    backResult.setVisible(true);
                });
                ImageIcon dayIcon = ui.resources.getImage(gameday + "day.png");
                JButton dayBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, dayIcon);
                dayBtn.setSize(dayIcon.getIconWidth(), dayIcon.getIconHeight());
                dayBtn.addActionListener(e1 -> {
                    ui.createDayPiao(1, gameday, ui.voteMethods.get(gameday - 2));
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
        });

        voteBtn.addActionListener(e -> {
            infoText.setVisible(false);
            ui.resources.playSound("click.wav");
            voteBtn.setVisible(false); pointBtn.setVisible(false); avoidBtn.setVisible(false);
            recordBtn.setVisible(false); avoidBtn1.setVisible(false);
            if (ui.isVote[0]) {
                readyVoteBtn.setVisible(true); greyBtn.setVisible(false); freeBtn.setVisible(false);
            } else {
                if (!cxList.isEmpty()) greyBtn.setVisible(true);
                freeBtn.setVisible(true); readyVoteBtn.setVisible(false);
            }
            if (cxList.isEmpty()) greyBtn.setVisible(false);
            returnBtn.setVisible(true); scrollPane1.setVisible(true);

            JTextArea isSelectedVoteTargetText = new JTextArea();
            StringBuilder isSelectedVoteTargetResult = new StringBuilder("-指定内容-\n");
            if (ui.isVote[0]) {
                isSelectedVoteTargetResult.append(GameStrings.SPECIFY_VOTE);
                for (int i = 1; i < ui.gs.gc.length; ++i) {
                    if (ui.gs.gc[i].isSelectedVoteTarget[ui.gs.gameDay])
                        isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number) + " ");
                }
                isSelectedVoteTargetResult.append("\n");
            }
            if (ui.isZhan[0]) {
                isSelectedVoteTargetResult.append(GameStrings.SPECIFY_DIVINATION);
                for (int i = 1; i < ui.gs.gc.length; ++i) {
                    if (ui.gs.gc[i].claimedRole == 1 && ui.gs.gc[i].whyDie == whyDie.NONE) {
                        isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number) + "→");
                        for (int j = 1; j < ui.gs.gc.length; ++j) {
                            if (ui.gs.gc[i].claimedRoleScheduledSkillTargets[j][ui.gs.gameDay])
                                isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[j].number) + ",");
                        }
                        isSelectedVoteTargetResult.append("\n");
                    }
                }
                int cc = 0;
                for (int j = 1; j < ui.gs.gc.length; ++j) {
                    if (ui.gs.hiddenSeerScheduledSkillTargets[j][ui.gs.gameDay]) {
                        if (cc++ == 0) isSelectedVoteTargetResult.append(GameStrings.HIDDEN_ARROW);
                        isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[j].number) + ",");
                    }
                }
                isSelectedVoteTargetResult.append("\n");
            }
            if (DebugLogger.getInstance().isEnabled()) {
                DebugLogger.log("是否护卫" + ui.isHu[0]);
                for (int i = 1; i < ui.gs.gc.length; ++i) {
                    for (int j = 1; j < ui.gs.gc.length; ++j) {
                        if (ui.gs.gc[i].claimedRoleScheduledSkillTargets[j][ui.gs.gameDay])
                            DebugLogger.log(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number) + "护卫了"
                                    + ui.gs.gc[i].claimedRoleScheduledSkillTargets[j][ui.gs.gameDay]);
                    }
                }
            }
            if (ui.isHu[0]) {
                isSelectedVoteTargetResult.append(GameStrings.SPECIFY_GUARD);
                for (int i = 1; i < ui.gs.gc.length; ++i) {
                    if (ui.gs.gc[i].claimedRole == 3 && ui.gs.gc[i].whyDie == whyDie.NONE) {
                        isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[i].number) + "→");
                        for (int j = 1; j < ui.gs.gc.length; ++j) {
                            if (ui.gs.gc[i].claimedRoleScheduledSkillTargets[j][ui.gs.gameDay])
                                isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[j].number) + ",");
                        }
                        isSelectedVoteTargetResult.append("\n");
                    }
                }
                int vv = 0;
                for (int j = 1; j < ui.gs.gc.length; ++j) {
                    if (ui.gs.hiddenHunterScheduledSkillTargets[j][ui.gs.gameDay]) {
                        if (vv++ == 0) isSelectedVoteTargetResult.append(GameStrings.HIDDEN_ARROW);
                        isSelectedVoteTargetResult.append(ui.uiComponentFactory.getJobText(ui.gs.gc[j].number) + ",");
                    }
                }
                isSelectedVoteTargetResult.append("\n");
            }
            isSelectedVoteTargetText.setText(isSelectedVoteTargetResult.toString());
            isSelectedVoteTargetText.setForeground(Color.BLACK);
            isSelectedVoteTargetText.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_VOTE));
            isSelectedVoteTargetText.setLineWrap(true);
            isSelectedVoteTargetText.setWrapStyleWord(true);
            isSelectedVoteTargetText.setEditable(false);
            isSelectedVoteTargetText.setOpaque(false);
            isSelectedVoteTargetText.setBackground(GameConstants.COLOR_TRANSPARENT);
            isSelectedVoteTargetText.setBorder(BorderFactory.createEmptyBorder());
            scrollPane1.getViewport().setView(isSelectedVoteTargetText);
            scrollPane1.setBorder(BorderFactory.createEmptyBorder());
            scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane1.setOpaque(false);
            scrollPane1.getViewport().setOpaque(false);
            scrollPane1.setBounds(40, 228, 200 + boardIcon.getIconWidth() - 80,
                    50 + boardIcon.getIconHeight() - 60);
            isSelectedVoteTargetText.setVisible(true);
            ui.jPanel.setComponentZOrder(scrollPane1, 0);
            ui.resizeComponents();
        });

        int[] round = {1};

        greyBtn.addActionListener(e -> {
            freeBtn.setVisible(false); greyBtn.setVisible(false); returnBtn.setVisible(false);
            scrollPane1.setVisible(false);
            StringBuilder greyText = new StringBuilder();
            for (int i = 0; i < cxList.size(); ++i)
                greyText.append(ui.uiComponentFactory.getJobText(ui.gs.gc[cxList.get(i)].number));
            List<Integer> beiZhan = new ArrayList<>();
            for (int j = 1; j < ui.gs.gameDay; ++j) {
                for (int k = 1; k < ui.gs.gc.length; ++k) {
                    if (ui.gs.gc[k].claimedRole == 1) {
                        int num = ui.gs.gc[k].skillTarget[j];
                        if (num > ui.gs.gc.length - 1) num -= ui.gs.gc.length - 1;
                        if (!beiZhan.contains(num)) { beiZhan.add(num); DebugLogger.log(num); }
                    }
                }
            }
            boolean[] isReVote = {false};
            if (DebugLogger.getInstance().isEnabled()) {
                if (!cxList.isEmpty()) DebugLogger.log("cxList不为空，且具体为" + cxList);
            }
            if (ui.mainLogic.shokei(1, cxList, ui.isAvoid)) {
                int trueDay = (ui.gs.end == 0) ? ui.gs.gameDay - 1 : ui.gs.gameDay;
                for (int f = 0; f < cxList.size(); ++f)
                    ui.greyCharas[f][trueDay] = cxList.get(f);
                ui.voteMethods.add(1); cxList.clear();
                ui.gs = ui.mainLogic.getGameStatus();
                ui.createPiao("-投票結果/" + (trueDay) + "日目-グレラン：\n" + greyText + "\n", round[0], isReVote);
                if (isReVote[0]) { againBtn.setVisible(true); nextBtn.setVisible(false); round[0]++; }
                else { nextBtn.setVisible(true); againBtn.setVisible(false); }
            } else {
                cxList.clear();
                freeBtn.setVisible(true); greyBtn.setVisible(true); returnBtn.setVisible(true);
                scrollPane1.setVisible(true);
                votehisBtn.setVisible(false); doubtBtn.setVisible(false); recordBtn.setVisible(false);
                coBtn.setVisible(false); ppBtn.setVisible(false); returnBtn.setVisible(false);
                reiBtn.setVisible(false); kariBtn.setVisible(false); uranaiBtn.setVisible(false);
                kyouyuBtn.setVisible(false); catBtn.setVisible(false); askCoBtn.setVisible(false);
                greyBtn.setVisible(false); freeBtn.setVisible(false); readyVoteBtn.setVisible(false);
                nextBtn.setVisible(false); againBtn.setVisible(false);
                ui.currentScene = UI.Scene.DIALOGUE_AFTERNOON;
                ui.run();
            }
        });

        recordBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            if (ui.gs.gameDay == 2) {
                ui.createTishi("まだ特に疑い先もなく、\n投票の履歴もないようだ。");
            } else {
                voteBtn.setVisible(false); pointBtn.setVisible(false); avoidBtn.setVisible(false);
                recordBtn.setVisible(false); avoidBtn1.setVisible(false);
                votehisBtn.setVisible(true); doubtBtn.setVisible(true); returnBtn.setVisible(true);
                ui.resizeComponents();
            }
        });

        avoidBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            avoidBtn1.setVisible(true); avoidBtn.setVisible(false);
            ui.isAvoid = false;
            ui.resizeComponents();
        });
        avoidBtn1.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            avoidBtn1.setVisible(false); avoidBtn.setVisible(true);
            ui.isAvoid = true;
            ui.resizeComponents();
        });

        pointBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            voteBtn.setVisible(false); pointBtn.setVisible(false); avoidBtn.setVisible(false);
            recordBtn.setVisible(false); avoidBtn1.setVisible(false);
            coBtn.setVisible(true); ppBtn.setVisible(true); returnBtn.setVisible(true);
            ui.resizeComponents();
        });

        coBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            coBtn.setVisible(false); ppBtn.setVisible(false);
            reiBtn.setVisible(true); kariBtn.setVisible(true); askCoBtn.setVisible(true);
            uranaiBtn.setVisible(true); kyouyuBtn.setVisible(true); catBtn.setVisible(true);
            ui.resizeComponents();
        });

        ppBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            coBtn.setVisible(false); ppBtn.setVisible(false); returnBtn.setVisible(false);
            fixedVoteBtn.setVisible(true); fixedUranaiBtn.setVisible(true); protectBtn.setVisible(true);
            ui.resizeComponents();
        });

        JPanel infoPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);
        JPanel infoZhanPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);
        JPanel infoHuPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);
        JPanel infoCoPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);

        List<Integer> askList = new ArrayList<>();
        boolean[] isCo = {false};
        returnBtn.addActionListener(e -> {
            if (isVotehis[0]) { ui.run(); }
            ui.resources.playSound("click.wav");
            voteBtn.setVisible(true); pointBtn.setVisible(true); infoText.setVisible(true);
            ui.piaoText.setVisible(false); ui.piaoText1.setVisible(false);
            if (ui.isAvoid) { avoidBtn.setVisible(true); avoidBtn1.setVisible(false); }
            else { avoidBtn1.setVisible(true); avoidBtn.setVisible(false); }
            levellb.setVisible(false);
            infoPanel.setVisible(false); infoZhanPanel.setVisible(false); infoHuPanel.setVisible(false);
            scrollPane1.setVisible(false); hisPanel.setVisible(false); infoCoPanel.setVisible(false);
            votehisBtn.setVisible(false); doubtBtn.setVisible(false); recordBtn.setVisible(true);
            coBtn.setVisible(false); ppBtn.setVisible(false); returnBtn.setVisible(false);
            reiBtn.setVisible(false); kariBtn.setVisible(false); uranaiBtn.setVisible(false);
            kyouyuBtn.setVisible(false); catBtn.setVisible(false); askCoBtn.setVisible(false);
            greyBtn.setVisible(false); freeBtn.setVisible(false); readyVoteBtn.setVisible(false);
            nextBtn.setVisible(false); againBtn.setVisible(false);
            if (!askList.isEmpty() && isCo[0]) {
                isCo[0] = false;
                DebugLogger.log(askList);
                ui.mainLogic.askCo(askList);
                if (!ui.events.isEmpty()) {
                    ui.gs = ui.mainLogic.getGameStatus();
                    ui.currentScene = UI.Scene.DIALOGUE_DAY;
                    ui.run();
                } else {
                    ui.createTishi(GameStrings.MSG_NO_CO);
                }
            }
            ui.resizeComponents();
        });

        freeBtn.addActionListener(e -> {
            freeBtn.setVisible(false); greyBtn.setVisible(false); returnBtn.setVisible(false);
            ui.resources.playSound("click.wav"); scrollPane1.setVisible(false);
            List<Integer> chuxingList = new ArrayList<>();
            for (int i = 1; i < ui.gs.gc.length; ++i)
                if (ui.gs.gc[i].whyDie == whyDie.NONE) chuxingList.add(i);
            if (DebugLogger.getInstance().isEnabled()) DebugLogger.log("自由投票" + chuxingList);
            boolean[] isReVote = {false};
            if (ui.mainLogic.shokei(0, chuxingList, ui.isAvoid)) {
                ui.voteMethods.add(0); chuxingList.clear();
                ui.gs = ui.mainLogic.getGameStatus();
                if (ui.gs.end == 0)
                    ui.createPiao("-投票結果/" + (ui.gs.gameDay - 1) + "日目-自由投票\n\n", round[0], isReVote);
                else
                    ui.createPiao("-投票結果/" + (ui.gs.gameDay) + "日目-自由投票\n\n", round[0], isReVote);
                if (isReVote[0]) { againBtn.setVisible(true); nextBtn.setVisible(false); round[0]++; }
                else { nextBtn.setVisible(true); againBtn.setVisible(false); }
            } else {
                chuxingList.clear();
                freeBtn.setVisible(true); greyBtn.setVisible(true); returnBtn.setVisible(true);
                scrollPane1.setVisible(true);
                votehisBtn.setVisible(false); doubtBtn.setVisible(false); recordBtn.setVisible(false);
                coBtn.setVisible(false); ppBtn.setVisible(false); returnBtn.setVisible(false);
                reiBtn.setVisible(false); kariBtn.setVisible(false); uranaiBtn.setVisible(false);
                kyouyuBtn.setVisible(false); catBtn.setVisible(false); askCoBtn.setVisible(false);
                greyBtn.setVisible(false); freeBtn.setVisible(false); readyVoteBtn.setVisible(false);
                nextBtn.setVisible(false); againBtn.setVisible(false);
                ui.currentScene = UI.Scene.DIALOGUE_AFTERNOON;
                ui.run();
            }
        });

        againBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            boolean[] isReVote = {false};
            if (ui.gs.end == 0)
                ui.createPiao("-投票結果/" + (ui.gs.gameDay - 1) + "日目-重新投票\n\n", round[0], isReVote);
            else
                ui.createPiao("-投票結果/" + (ui.gs.gameDay) + "日目-重新投票\n\n", round[0], isReVote);
            if (isReVote[0]) { againBtn.setVisible(true); nextBtn.setVisible(false); if (round[0] < 3) round[0]++; }
            else { nextBtn.setVisible(true); againBtn.setVisible(false); }
        });

        nextBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.voteRounds.add(round[0]);
            ui.currentScene = UI.Scene.DIALOGUE_CHUXING;
            ui.run();
        });

        readyVoteBtn.addActionListener(e -> {
            readyVoteBtn.setVisible(false); returnBtn.setVisible(false); scrollPane1.setVisible(false);
            List<Integer> chuxingList = new ArrayList<>();
            int p = 0;
            for (int i = 1; i < ui.gs.gc.length; ++i) {
                if (ui.gs.gc[i].isSelectedVoteTarget[ui.gs.gameDay] && ui.gs.gc[i].whyDie == whyDie.NONE) {
                    chuxingList.add(i);
                    ui.isSelectedVoteTargetCharas[p++][ui.gs.gameDay] = i;
                    DebugLogger.log("指定了" + i);
                }
            }
            if (DebugLogger.getInstance().isEnabled()) DebugLogger.log("指定投票" + chuxingList);
            boolean[] isReVote = {false};
            if (ui.mainLogic.shokei(2, chuxingList, ui.isAvoid)) {
                ui.voteMethods.add(2);
                ui.gs = ui.mainLogic.getGameStatus();
                int trueDay = (ui.gs.end == 0) ? ui.gs.gameDay - 1 : ui.gs.gameDay;
                StringBuilder isSelectedVoteTargetText = new StringBuilder();
                for (int i = 0; i < chuxingList.size(); ++i)
                    isSelectedVoteTargetText.append(ui.uiComponentFactory.getJobText(ui.gs.gc[chuxingList.get(i)].number)).append(",");
                DebugLogger.log(isSelectedVoteTargetText);
                ui.createPiao("-投票結果/" + trueDay + "日目-指定投票\n" + isSelectedVoteTargetText + "\n", round[0], isReVote);
                chuxingList.clear();
                if (isReVote[0]) { againBtn.setVisible(true); nextBtn.setVisible(false); round[0]++; }
                else { nextBtn.setVisible(true); againBtn.setVisible(false); }
            } else {
                chuxingList.clear();
                freeBtn.setVisible(true); greyBtn.setVisible(true); returnBtn.setVisible(true);
                scrollPane1.setVisible(true);
                votehisBtn.setVisible(false); doubtBtn.setVisible(false); recordBtn.setVisible(false);
                coBtn.setVisible(false); ppBtn.setVisible(false); returnBtn.setVisible(false);
                reiBtn.setVisible(false); kariBtn.setVisible(false); uranaiBtn.setVisible(false);
                kyouyuBtn.setVisible(false); catBtn.setVisible(false); askCoBtn.setVisible(false);
                greyBtn.setVisible(false); freeBtn.setVisible(false); readyVoteBtn.setVisible(false);
                nextBtn.setVisible(false); againBtn.setVisible(false);
                ui.currentScene = UI.Scene.DIALOGUE_AFTERNOON;
                ui.run();
            }
        });

        askCoBtn.addActionListener(e -> {
            isCo[0] = true;
            reiBtn.setVisible(false); kariBtn.setVisible(false); uranaiBtn.setVisible(false);
            kyouyuBtn.setVisible(false); catBtn.setVisible(false); askCoBtn.setVisible(false);
            infoCoPanel.setVisible(true);
            ui.resources.playSound("click.wav");
            infoText.setVisible(false);
            fixedVoteBtn.setVisible(false); fixedUranaiBtn.setVisible(false); protectBtn.setVisible(false);
            returnBtn.setVisible(true);

            infoCoPanel.removeAll();

            List<Integer> zhanbuNum = new ArrayList<>();
            List<Integer> zhanbuOrder = new ArrayList<>();
            for (int i = 1; i <= ui.gs.gc.length - 1; i++) {
                if (ui.gs.gc[i].claimedRole == 1 && ui.gs.gc[i].whyDie == whyDie.NONE) {
                    zhanbuNum.add(i);
                    zhanbuOrder.add(ui.gs.gc[i].claimedRoleorder);
                }
            }
            List<JLabel> targetLabels = new ArrayList<>();
            List<JLabel> frameLabels = new ArrayList<>();
            List<JLabel> resultLabels = new ArrayList<>();
            List<JLabel> zbLabels = new ArrayList<>();
            for (int i = 1; i <= ui.gs.gc.length - 1; i++) {
                StringBuilder imageName = new StringBuilder();
                if (ui.gs.gc[i].number <= 9) imageName.append("0");
                imageName.append(ui.gs.gc[i].number);
                switch (ui.gs.gc[i].whyDie) {
                    case NONE: break;
                    default: imageName.append("g"); break;
                }
                imageName.append("s.png");
                StringBuilder claimedRoleName = new StringBuilder("yaku");
                if (ui.gs.gc[i].claimedRole > 0 && ui.gs.gc[i].claimedRole < 6) {
                    if (ui.gs.gc[i].claimedRole <= 3) {
                        claimedRoleName.append(ui.gs.gc[i].claimedRole).append("_")
                                .append(ui.gs.gc[i].claimedRoleorder).append(".png");
                    } else {
                        claimedRoleName.append(ui.gs.gc[i].claimedRole).append(".png");
                    }
                    ImageIcon claimedRoleIcon = ui.resources.getImage(claimedRoleName.toString());
                    JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                    if (i <= (ui.gs.gc.length - 1 + 1) / 2)
                        claimedRoleLabel.setBounds(60 + 74 * i, 20, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    else
                        claimedRoleLabel.setBounds(60 + 74 * (i - ((ui.gs.gc.length - 1 + 1) / 2)), 128,
                                claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    infoCoPanel.add(claimedRoleLabel);
                }
                ImageIcon chooseIcon = ui.resources.getImage("frameSRed.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                if (i <= ui.gs.gc.length / 2)
                    chooseLabel.setBounds(60 + 74 * i, 20, chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
                else
                    chooseLabel.setBounds(60 + 74 * (i - ((ui.gs.gc.length - 1 + 1) / 2)), 128,
                            chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
                infoCoPanel.add(chooseLabel);
                chooseLabel.setVisible(false);

                ImageIcon voteIcon = ui.resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);
                if (i <= ui.gs.gc.length / 2)
                    voteLabel.setBounds(60 + 5 + 74 * i, 20, voteIcon.getIconWidth(), voteIcon.getIconHeight());
                else
                    voteLabel.setBounds(60 + 5 + 74 * (i - ui.gs.gc.length / 2), 128,
                            voteIcon.getIconWidth(), voteIcon.getIconHeight());
                infoCoPanel.add(voteLabel);
                infoCoPanel.setComponentZOrder(voteLabel, 0);
                voteLabel.setVisible(false);
                if (ui.gs.gc[i].isSelectedVoteTarget[ui.gs.gameDay]) voteLabel.setVisible(true);

                ImageIcon voteAllIcon = ui.resources.getImage("result1_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                if (i <= (ui.gs.gc.length - 1 + 1) / 2)
                    voteAllLabel.setBounds(60 + 5 + 74 * i, 40, voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
                else
                    voteAllLabel.setBounds(60 + 5 + 74 * (i - ui.gs.gc.length / 2), 148,
                            voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
                infoCoPanel.add(voteAllLabel);
                resultLabels.add(voteAllLabel);
                voteAllLabel.setVisible(false);

                if (i < zhanbuNum.size() + 1) {
                    for (int i2 = 1; i2 < ui.gs.gc.length; ++i2) {
                        ImageIcon zbIcon = ui.resources.getImage("result1_" + zhanbuOrder.get(i - 1) + "white.png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        if (i2 <= (ui.gs.gc.length - 1 + 1) / 2)
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * i2,
                                    20 + zbIcon.getIconHeight() * zhanbuOrder.get(i - 1),
                                    zbIcon.getIconWidth(), zbIcon.getIconHeight());
                        else
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - ui.gs.gc.length / 2),
                                    128 + zbIcon.getIconHeight() * zhanbuOrder.get(i - 1),
                                    zbIcon.getIconWidth(), zbIcon.getIconHeight());
                        infoCoPanel.add(zbLabel);
                        zbLabel.setVisible(false);
                    }
                }
                ImageIcon characterImage = ui.resources.getImage(imageName.toString());
                JLabel label = new JLabel(characterImage);
                targetLabels.add(label);
                if (i <= ui.gs.gc.length / 2)
                    label.setBounds(60 + (characterImage.getIconWidth() + 10) * i, 20,
                            characterImage.getIconWidth(), characterImage.getIconHeight());
                else
                    label.setBounds((60 + (characterImage.getIconWidth() + 10) * (i - ui.gs.gc.length / 2)),
                            30 + characterImage.getIconHeight(), characterImage.getIconWidth(), characterImage.getIconHeight());
                infoCoPanel.add(label);
            }
            for (int k = 2; k <= ui.gs.gameDay; ++k) {
                for (int j = 1; j < ui.gs.gc.length; ++j) {
                    if (ui.skillTargetPeople[j][k] == 0) continue;
                    int i1 = ui.skillTargetPeople[j][k];
                    int zynum = ui.claimedRolenum[j][k];
                    String name = ui.skillTargetNames[j][k];
                    int order = ui.skillTargetOrder[j][k];
                    if (zynum == 3) continue;
                    if (zynum == 1 && ui.gs.gc[j].dieDay != 0 && ui.gs.gc[j].dieDay < k) continue;
                    if (zynum == 2 && ui.gs.gc[j].dieDay != 0 && ui.gs.gc[j].dieDay < k) continue;
                    ImageIcon skillTargetIcon = ui.resources.getImage(name);
                    JLabel skillTargetLabel = new JLabel(skillTargetIcon);
                    if (i1 <= ui.gs.gc.length / 2)
                        skillTargetLabel.setBounds(((50 + 74 * (i1 + 1)) - skillTargetIcon.getIconWidth() * zynum),
                                20 + (order - 1) * skillTargetIcon.getIconHeight(),
                                skillTargetIcon.getIconWidth(), skillTargetIcon.getIconHeight());
                    else
                        skillTargetLabel.setBounds(((50 + 74 * (i1 + 1 - ui.gs.gc.length / 2)) - skillTargetIcon.getIconWidth() * zynum),
                                (128 + (order - 1) * skillTargetIcon.getIconHeight()),
                                skillTargetIcon.getIconWidth(), skillTargetIcon.getIconHeight());
                    infoCoPanel.add(skillTargetLabel);
                    infoCoPanel.setComponentZOrder(skillTargetLabel, 0);
                }
            }
            infoCoPanel.setBounds(0, 198, 200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight());

            JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                    200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight(), boardIcon);

            ImageIcon dragIcon = ui.resources.getImage("uranaiAll.png");
            JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 250, 350,
                    dragIcon.getIconWidth() / 2, dragIcon.getIconHeight() / 2, dragIcon);
            dragBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn.getParent();
                    if (parent == null || targetLabels.isEmpty()) return;
                    Rectangle btnRect = dragBtn.getBounds();
                    int btnCenterX = btnRect.x + btnRect.width / 2;
                    int btnCenterY = btnRect.y + btnRect.height / 2;
                    for (JLabel label : targetLabels) {
                        if (label.getParent() != parent || !label.isVisible()) continue;
                        Rectangle labelRect = label.getBounds();
                        if (labelRect.contains(btnCenterX, btnCenterY)) {
                            int index = targetLabels.indexOf(label);
                            if (ui.gs.gc[index + 1].whyDie != whyDie.NONE) break;
                            ui.resources.playSound("click.wav");
                            if (!askList.contains(index + 1)) askList.add(index + 1);
                            resultLabels.get(index).setVisible(true);
                            frameLabels.get(index).setVisible(true);
                            frameLabels.get(index).repaint();
                            ui.jPanel.repaint(label.getBounds());
                            break;
                        }
                        dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
            });
            infoCoPanel.add(dragBtn);

            dragIcon = ui.resources.getImage("delete.png");
            JButton dragBtn_delete = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 800, 350,
                    dragIcon.getIconWidth() / 2, dragIcon.getIconHeight() / 2, dragIcon);
            dragBtn_delete.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn_delete.getParent();
                    if (parent == null || targetLabels.isEmpty()) return;
                    Rectangle btnRect = dragBtn_delete.getBounds();
                    int btnCenterX = btnRect.x + btnRect.width / 2;
                    int btnCenterY = btnRect.y + btnRect.height / 2;
                    for (JLabel label : targetLabels) {
                        if (label.getParent() != parent || !label.isVisible()) continue;
                        Rectangle labelRect = label.getBounds();
                        if (labelRect.contains(btnCenterX, btnCenterY)) {
                            int index = targetLabels.indexOf(label);
                            if (ui.gs.gc[index + 1].whyDie != whyDie.NONE) break;
                            ui.resources.playSound("click.wav");
                            if (askList.contains(index + 1)) {
                                int uu = askList.indexOf(index + 1);
                                askList.remove(uu);
                            }
                            for (int u = 0; u < zhanbuNum.size(); ++u)
                                zbLabels.get(index + u * (ui.gs.gc.length - 1)).setVisible(false);
                            resultLabels.get(index).setVisible(false);
                            frameLabels.get(index).setVisible(false);
                            frameLabels.get(index).repaint();
                            ui.jPanel.repaint(label.getBounds());
                            break;
                        }
                        dragBtn_delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
            });
            infoCoPanel.add(dragBtn_delete);
            infoCoPanel.add(infoBoard);
            ui.jPanel.add(infoCoPanel);
            ui.jPanel.setComponentZOrder(infoCoPanel, 0);
            ui.resizeComponents();
        });

        reiBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.mainLogic.askCo(Role.ling);
            if (ui.events.isEmpty()) {
                ui.createTishi(GameStrings.MSG_NO_MEDIUM);
            } else {
                ui.gs = ui.mainLogic.getGameStatus();
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            }
        });
        kariBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.mainLogic.askCo(Role.lie);
            if (ui.events.isEmpty()) {
                ui.createTishi(GameStrings.MSG_NO_HUNTER);
            } else {
                ui.gs = ui.mainLogic.getGameStatus();
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            }
        });
        uranaiBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.mainLogic.askCo(Role.zhan);
            if (ui.events.isEmpty()) {
                ui.createTishi(GameStrings.MSG_NO_SEER);
            } else {
                ui.gs = ui.mainLogic.getGameStatus();
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            }
        });
        kyouyuBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.mainLogic.askCo(Role.gong);
            if (ui.events.isEmpty()) {
                ui.createTishi(GameStrings.MSG_NO_SHARED);
            } else {
                ui.gs = ui.mainLogic.getGameStatus();
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            }
        });
        catBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.mainLogic.askCo(Role.mao);
            if (ui.events.isEmpty()) {
                ui.createTishi(GameStrings.MSG_NO_CAT);
            } else {
                ui.gs = ui.mainLogic.getGameStatus();
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            }
        });

        fixedVoteBtn.addActionListener(e -> {
            ui.hideButtons(fixedVoteBtn, fixedUranaiBtn, protectBtn);
            infoText.setVisible(false); returnBtn.setVisible(true);
            ui.createCharacterSelectionPanel(UI.SelectionType.VOTE, infoPanel, ui.voteChosen, ui.isVote, boardIcon);
        });
        fixedUranaiBtn.addActionListener(e -> {
            ui.hideButtons(fixedVoteBtn, fixedUranaiBtn, protectBtn);
            infoText.setVisible(false); returnBtn.setVisible(true);
            ui.createCharacterSelectionPanel(UI.SelectionType.DIVINATION, infoZhanPanel, ui.zhanChosen, ui.isZhan, boardIcon);
        });
        protectBtn.addActionListener(e -> {
            ui.hideButtons(fixedVoteBtn, fixedUranaiBtn, protectBtn);
            infoText.setVisible(false); returnBtn.setVisible(true);
            ui.createCharacterSelectionPanel(UI.SelectionType.GUARD, infoHuPanel, ui.huChosen, ui.isHu, boardIcon);
        });

        ui.jPanel.add(scrollPane);
        ui.jPanel.add(board);
        ui.jPanel.add(background);
        ui.jFrame.setVisible(true);
        ui.resizeComponents();
    }
}