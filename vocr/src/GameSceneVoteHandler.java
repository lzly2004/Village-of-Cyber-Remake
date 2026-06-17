// GameSceneVoteHandler.java - 投票主界面场景处理器
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GameSceneVoteHandler implements SceneHandler {
    private UI ui;

    @Override
    public void render(UI ui) {
        this.ui = ui;
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
            createDoubt();
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
                    createDayPiao(2, gameday, ui.voteMethods.get(gameday - 2));
                    backResult.setVisible(true); nextResult.setVisible(false);
                    if (roundMax == 3) nextResult1.setVisible(true);
                });
                backResult.addActionListener(e1 -> {
                    createDayPiao(1, gameday, ui.voteMethods.get(gameday - 2));
                    backResult.setVisible(false); nextResult.setVisible(true);
                    nextResult1.setVisible(false);
                });
                nextResult1.addActionListener(e1 -> {
                    createDayPiao(3, gameday, ui.voteMethods.get(gameday - 2));
                    backResult1.setVisible(true); nextResult1.setVisible(false);
                    backResult.setVisible(false);
                });
                backResult1.addActionListener(e1 -> {
                    createDayPiao(2, gameday, ui.voteMethods.get(gameday - 2));
                    backResult1.setVisible(false); nextResult1.setVisible(true);
                    backResult.setVisible(true);
                });
                ImageIcon dayIcon = ui.resources.getImage(gameday + "day.png");
                JButton dayBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, dayIcon);
                dayBtn.setSize(dayIcon.getIconWidth(), dayIcon.getIconHeight());
                dayBtn.addActionListener(e1 -> {
                    createDayPiao(1, gameday, ui.voteMethods.get(gameday - 2));
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
                createPiao("-投票結果/" + (trueDay) + "日目-グレラン：\n" + greyText + "\n", round[0], isReVote);
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
                createTishi("まだ特に疑い先もなく、\n投票の履歴もないようだ。");
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
                    createTishi(GameStrings.MSG_NO_CO);
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
                if (ui.gs.end == 0) {
                    createPiao("-投票結果/" + (ui.gs.gameDay - 1) + "日目-自由投票\n\n", round[0], isReVote);
                } else {
                    createPiao("-投票結果/" + (ui.gs.gameDay) + "日目-自由投票\n\n", round[0], isReVote);
                }
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
            if (ui.gs.end == 0) {
                createPiao("-投票結果/" + (ui.gs.gameDay - 1) + "日目-重新投票\n\n", round[0], isReVote);
            } else {
                createPiao("-投票結果/" + (ui.gs.gameDay) + "日目-重新投票\n\n", round[0], isReVote);
            }
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
                createPiao("-投票結果/" + trueDay + "日目-指定投票\n" + isSelectedVoteTargetText + "\n", round[0], isReVote);
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
                createTishi(GameStrings.MSG_NO_MEDIUM);
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
                createTishi(GameStrings.MSG_NO_HUNTER);
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
                createTishi(GameStrings.MSG_NO_SEER);
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
                createTishi(GameStrings.MSG_NO_SHARED);
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
                createTishi(GameStrings.MSG_NO_CAT);
            } else {
                ui.gs = ui.mainLogic.getGameStatus();
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            }
        });

        fixedVoteBtn.addActionListener(e -> {
            UIHelpers.hideButtons(fixedVoteBtn, fixedUranaiBtn, protectBtn);
            infoText.setVisible(false); returnBtn.setVisible(true);
            createCharacterSelectionPanel(SelectionType.VOTE, infoPanel, ui.voteChosen, ui.isVote, boardIcon);
        });
        fixedUranaiBtn.addActionListener(e -> {
            UIHelpers.hideButtons(fixedVoteBtn, fixedUranaiBtn, protectBtn);
            infoText.setVisible(false); returnBtn.setVisible(true);
            createCharacterSelectionPanel(SelectionType.DIVINATION, infoZhanPanel, ui.zhanChosen, ui.isZhan, boardIcon);
        });
        protectBtn.addActionListener(e -> {
            UIHelpers.hideButtons(fixedVoteBtn, fixedUranaiBtn, protectBtn);
            infoText.setVisible(false); returnBtn.setVisible(true);
            createCharacterSelectionPanel(SelectionType.GUARD, infoHuPanel, ui.huChosen, ui.isHu, boardIcon);
        });

        ui.jPanel.add(scrollPane);
        ui.jPanel.add(board);
        ui.jPanel.add(background);
        ui.jFrame.setVisible(true);
        ui.resizeComponents();
    }

    private void stylePiaoTextArea(JTextArea area, String text, int x, int y, int w, int h) {
        area.setText(text);
        area.setForeground(Color.black);
        area.setFont(new Font(GameConstants.FONT_FAMILY, Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBackground(GameConstants.COLOR_TRANSPARENT);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setBounds(x, y, w, h);
        this.ui.jPanel.add(area);
        this.ui.jPanel.setComponentZOrder(area, 0);
    }

    private void createTishi(String str) {
        JTextArea tishiText = TextareaSimpleFactory.createTranslucentTipTextArea(str);
        tishiText.setBounds(300, 300, 500, 200);
        this.ui.jPanel.add(tishiText);
        this.ui.jPanel.setComponentZOrder(tishiText, 0);
        Timer timer = new Timer(GameConstants.TRANSITION_SHORT_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tishiText.setVisible(false);
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
        this.ui.resizeComponents();
    }

    private void createDayPiao(int round, int gameDay, int dailyVotingRule) {
        this.ui.piaoText.setVisible(true);
        this.ui.piaoText1.setVisible(true);
        int[][] voteTotal = new int[this.ui.gs.gc.length][4];
        for (int i = 1; i < this.ui.gs.gc.length; ++i) {
            voteTotal[this.ui.gs.gc[i].voteTarget[gameDay][round]][round]++;
        }
        StringBuilder extraText = new StringBuilder();
        switch (dailyVotingRule) {
            case 0:
                extraText.append(GameStrings.VOTE_FREE);
                break;
            case 1:
                extraText.append(GameStrings.VOTE_GREY);
                for (int i = 0; i < this.ui.gs.gc.length; ++i) {
                    if (this.ui.greyCharas[i][gameDay] != 0) {
                        extraText.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.greyCharas[i][gameDay]].number));
                    }
                }
                break;
            case 2:
                extraText.append(GameStrings.VOTE_DESIGNATED);
                for (int i = 0; i < this.ui.gs.gc.length; ++i) {
                    if (this.ui.isSelectedVoteTargetCharas[i][gameDay] != 0) {
                        extraText.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.isSelectedVoteTargetCharas[i][gameDay]].number)).append(",");
                    }
                }
                break;
        }
        StringBuilder leftPiao = new StringBuilder("-投票結果/" + gameDay + "日目-第" + round + "轮" + extraText + "\n");
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        for (int i = 1; i < this.ui.gs.gc.length; ++i) {
            if (this.ui.gs.gc[i].whyDie == whyDie.NONE || this.ui.gs.gc[i].dieDay >= gameDay) {
                if (leftCnt >= 10) {
                    rightPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                } else {
                    leftPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    leftCnt++;
                }
            }
        }
        if (DebugLogger.getInstance().isEnabled()) {
            DebugLogger.log(leftPiao);
            DebugLogger.log(rightPiao);
        }
        stylePiaoTextArea(this.ui.piaoText, leftPiao.toString(), 40, 228, 1000, 430);
        stylePiaoTextArea(this.ui.piaoText1, rightPiao.toString(), 400, 228, 450, 430);
        this.ui.resizeComponents();
    }

    private void createPiao(String str, int round, boolean[] isReVote) {
        if (DebugLogger.getInstance().isEnabled()) {
            DebugLogger.log("***当前gameDay等于" + this.ui.gs.gameDay + "***");
        }
        this.ui.piaoText.setVisible(true);
        this.ui.piaoText1.setVisible(true);
        int gameDay;
        if (this.ui.gs.end == 0) {
            gameDay = this.ui.gs.gameDay - 1;
        } else {
            gameDay = this.ui.gs.gameDay;
        }
        int[][] voteTotal = new int[this.ui.gs.gc.length][4];
        for (int i = 1; i < this.ui.gs.gc.length; ++i) {
            if (this.ui.gs.gc[i].whyDie == whyDie.NONE || this.ui.gs.gc[i].dieDay == gameDay) {
                voteTotal[this.ui.gs.gc[i].voteTarget[gameDay][round]][round]++;
            }
        }
        StringBuilder leftPiao = new StringBuilder(str);
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        int max = voteTotal[1][round];
        for (int i = 2; i < this.ui.gs.gc.length; ++i) {
            if (voteTotal[i][round] > max) max = voteTotal[i][round];
        }
        int maxCnt = 0;
        List<Integer> maxPos = new ArrayList<>();
        for (int i = 1; i < this.ui.gs.gc.length; ++i) {
            if (voteTotal[i][round] == max) { maxCnt++; maxPos.add(i); }
        }
        for (int i = 1; i < this.ui.gs.gc.length; ++i) {
            if (this.ui.gs.gc[i].whyDie == whyDie.NONE || this.ui.gs.gc[i].dieDay == gameDay) {
                if (leftCnt >= 10) {
                    rightPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                } else {
                    leftPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    leftCnt++;
                }
            }
            if (i == this.ui.gs.gc.length - 1) {
                for (int i1 = 0; i1 < 10 - leftCnt; ++i1) leftPiao.append("\n");
                if (maxCnt == 1) {
                    leftPiao.append(max).append("票で").append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[maxPos.get(0)].number)).append("さんが処刑されました。");
                    isReVote[0] = false;
                    this.ui.chuxingWho = maxPos.get(0);
                    maxPos.clear();
                } else {
                    leftPiao.append("投票が同点となりました。再投票を行います。");
                    isReVote[0] = true;
                    maxPos.clear();
                }
            }
        }
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);
        stylePiaoTextArea(this.ui.piaoText, leftPiao.toString(), 40, 228, 900, 430);
        stylePiaoTextArea(this.ui.piaoText1, rightPiao.toString(), 530, 228, 450, 430);
        this.ui.resizeComponents();
    }

    private void createDoubt() {
        this.ui.piaoText.setVisible(true);
        this.ui.piaoText1.setVisible(true);
        StringBuilder leftPiao = new StringBuilder("- 疑い先 - 全体表示\n\n");
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        for (int i = 1; i < this.ui.gs.gc.length; ++i) {
            if (this.ui.gs.gc[i].whyDie == whyDie.NONE) {
                ArrayList<Integer> charas = new ArrayList<>();
                for (int t = 1; t <= 3; ++t) {
                    if (t < this.ui.gs.gc[i].top3SuspectedPlayers.length && this.ui.gs.gc[i].top3SuspectedPlayers[t][this.ui.gs.gameDay] != 0) {
                        charas.add(this.ui.gs.gc[i].top3SuspectedPlayers[t][this.ui.gs.gameDay]);
                    }
                }
                ArrayList<String> cmps = new ArrayList<>();
                for (int t = 0; t <= charas.size() - 2; ++t) {
                    int temp = this.ui.gs.gc[i].suspicionValue[charas.get(t)] - this.ui.gs.gc[i].suspicionValue[charas.get(t + 1)];
                    if (temp <= 2) cmps.add("=");
                    else if (temp <= 5) cmps.add("≧");
                    else if (temp <= 10) cmps.add(">");
                    else cmps.add("≫");
                }
                if (leftCnt >= 10) {
                    for (int u = 1; u < 4; ++u) {
                        if (u < this.ui.gs.gc[i].top3SuspectedPlayers.length && this.ui.gs.gc[i].top3SuspectedPlayers[u][this.ui.gs.gameDay] != 0) {
                            if (u == 1) rightPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[i].number)).append("：");
                            rightPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.gs.gc[i].top3SuspectedPlayers[u][this.ui.gs.gameDay]].number));
                            if (!cmps.isEmpty()) { rightPiao.append(cmps.getFirst()); cmps.removeFirst(); }
                        }
                    }
                    rightPiao.append("\n");
                } else {
                    for (int u = 1; u < 4; ++u) {
                        if (u < this.ui.gs.gc[i].top3SuspectedPlayers.length && this.ui.gs.gc[i].top3SuspectedPlayers[u][this.ui.gs.gameDay] != 0) {
                            if (u == 1) leftPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[i].number)).append("：");
                            leftPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.gs.gc[this.ui.gs.gc[i].top3SuspectedPlayers[u][this.ui.gs.gameDay]].number));
                            if (!cmps.isEmpty()) { leftPiao.append(cmps.getFirst()); cmps.removeFirst(); }
                        }
                    }
                    leftPiao.append("\n");
                    leftCnt++;
                }
            }
        }
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);
        stylePiaoTextArea(this.ui.piaoText, leftPiao.toString(), 40, 228, 900, 430);
        stylePiaoTextArea(this.ui.piaoText1, rightPiao.toString(), 530, 228, 450, 430);
        this.ui.resizeComponents();
    }

    private int charGridX(int i, int baseX, int spacing) {
        int half = (this.ui.gs.gc.length) / 2;
        return baseX + spacing * (i <= half ? i : i - (this.ui.gs.gc.length - 1) / 2);
    }

    private int charGridY(int i, int yTop, int yBottom) {
        return i <= (this.ui.gs.gc.length) / 2 ? yTop : yBottom;
    }

    private void updateFlagFromVoteTargets(boolean[] flag) {
        for (int y = 1; y < this.ui.gs.gc.length; ++y) {
            if (this.ui.gs.gc[y].isSelectedVoteTarget[this.ui.gs.gameDay]) { flag[0] = true; return; }
        }
        flag[0] = false;
    }

    private void updateFlagFromScheduledTargets(java.util.List<Integer> trueNums, boolean[] flag, boolean isSeer) {
        for (int b = 0; b < trueNums.size(); ++b) {
            for (int j = 1; j < this.ui.gs.gc.length; ++j) {
                if (this.ui.gs.gc[trueNums.get(b)].claimedRoleScheduledSkillTargets[j][this.ui.gs.gameDay]) {
                    flag[0] = true; return;
                }
            }
        }
        for (int y = 1; y < this.ui.gs.gc.length; ++y) {
            if (isSeer && this.ui.gs.hiddenSeerScheduledSkillTargets[y][this.ui.gs.gameDay]) { flag[0] = true; return; }
            if (!isSeer && this.ui.gs.hiddenHunterScheduledSkillTargets[y][this.ui.gs.gameDay]) { flag[0] = true; return; }
        }
        flag[0] = false;
    }

    private void createCharacterSelectionPanel(SelectionType type, JPanel panel,
                                                java.util.List<Integer> chosenList, boolean[] flag,
                                                ImageIcon boardIcon) {
        panel.setVisible(false);
        panel.removeAll();
        this.ui.jPanel.add(panel);
        this.ui.jPanel.setComponentZOrder(panel, 0);
        int n = this.ui.gs.gc.length;

        String frameIconName = switch (type) {
            case VOTE -> "frameSBlue.png";
            case DIVINATION -> "frameSRed.png";
            case GUARD -> "frameOrange.png";
        };
        String resultAllIconName = switch (type) {
            case VOTE -> "result2_all.png";
            case DIVINATION -> "result1_all.png";
            case GUARD -> "result3_all.png";
        };
        String dragAllIconName = switch (type) {
            case VOTE -> "touhyou.png";
            case DIVINATION -> "uranaiAll.png";
            case GUARD -> "goeiAll.png";
        };
        int claimedRoleFilter = switch (type) {
            case VOTE -> 0;
            case DIVINATION -> 1;
            case GUARD -> 3;
        };

        java.util.List<Integer> roleNums = new java.util.ArrayList<>();
        java.util.List<Integer> roleOrders = new java.util.ArrayList<>();
        java.util.List<Integer> trueNums = new java.util.ArrayList<>();
        for (int i = 1; i <= n - 1; i++) {
            if (this.ui.gs.gc[i].claimedRole == claimedRoleFilter && this.ui.gs.gc[i].whyDie == whyDie.NONE) {
                roleNums.add(i);
                roleOrders.add(this.ui.gs.gc[i].claimedRoleorder);
            }
            if ((this.ui.gs.gc[i].actualRole == claimedRoleFilter || this.ui.gs.gc[i].claimedRole == claimedRoleFilter)
                    && this.ui.gs.gc[i].whyDie == whyDie.NONE) {
                trueNums.add(i);
            }
        }

        java.util.List<JLabel> targetLabels = new java.util.ArrayList<>();
        java.util.List<JLabel> frameLabels = new java.util.ArrayList<>();
        java.util.List<JLabel> resultLabels = new java.util.ArrayList<>();
        java.util.List<JLabel> zbLabels = new java.util.ArrayList<>();

        for (int i = 1; i <= n - 1; i++) {
            StringBuilder imageName = new StringBuilder();
            if (this.ui.gs.gc[i].number <= 9) imageName.append("0");
            imageName.append(this.ui.gs.gc[i].number);
            if (this.ui.gs.gc[i].whyDie != whyDie.NONE) imageName.append("g");
            imageName.append("s.png");

            if (this.ui.gs.gc[i].claimedRole > 0 && this.ui.gs.gc[i].claimedRole < 6) {
                StringBuilder crName = new StringBuilder("yaku");
                if (this.ui.gs.gc[i].claimedRole <= 3)
                    crName.append(this.ui.gs.gc[i].claimedRole).append("_").append(this.ui.gs.gc[i].claimedRoleorder).append(".png");
                else
                    crName.append(this.ui.gs.gc[i].claimedRole).append(".png");
                JLabel crLabel = new JLabel(this.ui.resources.getImage(crName.toString()));
                crLabel.setBounds(charGridX(i, 60, 74), charGridY(i, 20, 128),
                        crLabel.getIcon().getIconWidth(), crLabel.getIcon().getIconHeight());
                panel.add(crLabel);
            }

            JLabel chooseLabel = new JLabel(this.ui.resources.getImage(frameIconName));
            frameLabels.add(chooseLabel);
            chooseLabel.setBounds(charGridX(i, 60, 74), charGridY(i, 20, 128), 64, 98);
            panel.add(chooseLabel);
            chooseLabel.setVisible(chosenList.contains(i));

            if (type == SelectionType.VOTE) {
                JLabel voteLabel = new JLabel(this.ui.resources.getImage("result2_all.png"));
                voteLabel.setBounds(charGridX(i, 65, 74), charGridY(i, 20, 128),
                        voteLabel.getIcon().getIconWidth(), voteLabel.getIcon().getIconHeight());
                panel.add(voteLabel);
                panel.setComponentZOrder(voteLabel, 0);
                voteLabel.setVisible(this.ui.gs.gc[i].isSelectedVoteTarget[this.ui.gs.gameDay]);
            }

            JLabel resultLabel = new JLabel(this.ui.resources.getImage(resultAllIconName));
            resultLabels.add(resultLabel);
            resultLabel.setBounds(charGridX(i, 65, 74), charGridY(i, 40, 148),
                    resultLabel.getIcon().getIconWidth(), resultLabel.getIcon().getIconHeight());
            panel.add(resultLabel);
            resultLabel.setVisible(chosenList.contains(i));

            if (type != SelectionType.VOTE) {
                String zbIconPrefix = (type == SelectionType.DIVINATION) ? "result1_" : "result3_";
                String zbSuffix = (type == SelectionType.DIVINATION) ? "white.png" : ".png";
                for (int r = 0; r < roleNums.size(); r++) {
                    JLabel zbLabel = new JLabel(this.ui.resources.getImage(zbIconPrefix + roleOrders.get(r) + zbSuffix));
                    zbLabels.add(zbLabel);
                    zbLabel.setBounds(charGridX(i, 65 + zbLabel.getIcon().getIconWidth(), 74),
                            charGridY(i, 20 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r),
                                    128 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r)),
                            zbLabel.getIcon().getIconWidth(), zbLabel.getIcon().getIconHeight());
                    panel.add(zbLabel);
                    zbLabel.setVisible(chosenList.contains(i));
                }
            }

            ImageIcon charImg = this.ui.resources.getImage(imageName.toString());
            JLabel label = new JLabel(charImg);
            targetLabels.add(label);
            label.setBounds(charGridX(i, 60, charImg.getIconWidth() + 10),
                    charGridY(i, 20, 30 + charImg.getIconHeight()),
                    charImg.getIconWidth(), charImg.getIconHeight());
            panel.add(label);
        }

        for (int k = 2; k <= this.ui.gs.gameDay; ++k) {
            for (int j = 1; j < this.ui.gs.gc.length; ++j) {
                if (this.ui.skillTargetPeople[j][k] == 0) continue;
                int i1 = this.ui.skillTargetPeople[j][k];
                int zynum = this.ui.claimedRolenum[j][k];
                if (zynum == 3) continue;
                if (zynum == 1 && this.ui.gs.gc[j].dieDay != 0 && this.ui.gs.gc[j].dieDay < k) continue;
                if (zynum == 2 && this.ui.gs.gc[j].dieDay != 0 && this.ui.gs.gc[j].dieDay < k) continue;
                JLabel stLabel = new JLabel(this.ui.resources.getImage(this.ui.skillTargetNames[j][k]));
                stLabel.setBounds(charGridX(i1, 50 + 74, 74) - stLabel.getIcon().getIconWidth() * zynum,
                        charGridY(i1, 20 + (this.ui.skillTargetOrder[j][k] - 1) * stLabel.getIcon().getIconHeight(),
                                128 + (this.ui.skillTargetOrder[j][k] - 1) * stLabel.getIcon().getIconHeight()),
                        stLabel.getIcon().getIconWidth(), stLabel.getIcon().getIconHeight());
                panel.add(stLabel);
                panel.setComponentZOrder(stLabel, 0);
            }
        }

        panel.setBounds(GameConstants.INFO_PANEL_X, GameConstants.INFO_PANEL_Y,
                200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight());
        panel.add(LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight(), boardIcon));

        // === "全部"拖拽按钮 ===
        JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 250, 350,
                this.ui.resources.getImage(dragAllIconName).getIconWidth() / 2,
                this.ui.resources.getImage(dragAllIconName).getIconHeight() / 2,
                this.ui.resources.getImage(dragAllIconName));
        dragBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                java.awt.Container parent = dragBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                java.awt.Rectangle btnRect = dragBtn.getBounds();
                int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    if (!label.getBounds().contains(cx, cy)) continue;
                    int idx = targetLabels.indexOf(label);
                    if (ui.gs.gc[idx + 1].whyDie != whyDie.NONE) break;
                    ui.resources.playSound("click.wav");
                    switch (type) {
                        case VOTE -> {
                            ui.gs.gc[idx + 1].isSelectedVoteTarget[ui.gs.gameDay] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            updateFlagFromVoteTargets(flag);
                        }
                        case DIVINATION -> {
                            ui.gs.hiddenSeerScheduledSkillTargets[idx + 1][ui.gs.gameDay] = true;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][ui.gs.gameDay] = true;
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                        }
                        case GUARD -> {
                            ui.gs.hiddenHunterScheduledSkillTargets[idx + 1][ui.gs.gameDay] = true;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][ui.gs.gameDay] = true;
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                        }
                    }
                    resultLabels.get(idx).setVisible(true);
                    frameLabels.get(idx).setVisible(true);
                    frameLabels.get(idx).repaint();
                    ui.jPanel.repaint(label.getBounds());
                    break;
                }
                dragBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            }
        });
        panel.add(dragBtn);
        panel.setComponentZOrder(dragBtn, 0);

        // === 逐职业拖拽按钮 ===
        if (type != SelectionType.VOTE) {
            String perIconPrefix = (type == SelectionType.DIVINATION) ? "uranai" : "goei";
            for (int r = 0; r < roleNums.size(); r++) {
                final int cur = r;
                JButton perBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                        roleOrders.get(r) * (type == SelectionType.DIVINATION ? 100 : 150)
                                + (type == SelectionType.DIVINATION ? 150 : 250), 350,
                        this.ui.resources.getImage(perIconPrefix + roleOrders.get(r) + ".png").getIconWidth() / 2,
                        this.ui.resources.getImage(perIconPrefix + roleOrders.get(r) + ".png").getIconHeight() / 2,
                        this.ui.resources.getImage(perIconPrefix + roleOrders.get(r) + ".png"));
                perBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        java.awt.Container parent = perBtn.getParent();
                        if (parent == null || targetLabels.isEmpty()) return;
                        java.awt.Rectangle btnRect = perBtn.getBounds();
                        int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                        for (JLabel label : targetLabels) {
                            if (label.getParent() != parent || !label.isVisible()) continue;
                            if (!label.getBounds().contains(cx, cy)) continue;
                            int idx = targetLabels.indexOf(label);
                            if (ui.gs.gc[idx + 1].whyDie != whyDie.NONE) break;
                            ui.resources.playSound("click.wav");
                            ui.gs.gc[roleNums.get(cur)].claimedRoleScheduledSkillTargets[idx + 1][ui.gs.gameDay] = true;
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            zbLabels.get(idx + cur * (ui.gs.gc.length - 1)).setVisible(true);
                            frameLabels.get(idx).setVisible(true);
                            frameLabels.get(idx).repaint();
                            ui.jPanel.repaint(label.getBounds());
                            break;
                        }
                        perBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    }
                });
                panel.add(perBtn);
                panel.setComponentZOrder(perBtn, 0);
            }
        }

        // === 删除按钮 ===
        JButton delBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                (type == SelectionType.VOTE) ? 500 : 800, 350,
                this.ui.resources.getImage("delete.png").getIconWidth() / 2,
                this.ui.resources.getImage("delete.png").getIconHeight() / 2,
                this.ui.resources.getImage("delete.png"));
        delBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                java.awt.Container parent = delBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                java.awt.Rectangle btnRect = delBtn.getBounds();
                int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    if (!label.getBounds().contains(cx, cy)) continue;
                    int idx = targetLabels.indexOf(label);
                    if (ui.gs.gc[idx + 1].whyDie != whyDie.NONE) break;
                    ui.resources.playSound("click.wav");
                    chosenList.remove(Integer.valueOf(idx + 1));
                    switch (type) {
                        case VOTE -> {
                            ui.gs.gc[idx + 1].isSelectedVoteTarget[ui.gs.gameDay] = false;
                            updateFlagFromVoteTargets(flag);
                        }
                        case DIVINATION -> {
                            ui.gs.hiddenSeerScheduledSkillTargets[idx + 1][ui.gs.gameDay] = false;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][ui.gs.gameDay] = false;
                            updateFlagFromScheduledTargets(trueNums, flag, true);
                        }
                        case GUARD -> {
                            ui.gs.hiddenHunterScheduledSkillTargets[idx + 1][ui.gs.gameDay] = false;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][ui.gs.gameDay] = false;
                            updateFlagFromScheduledTargets(trueNums, flag, false);
                        }
                    }
                    if (type != SelectionType.VOTE) {
                        for (int u = 0; u < roleNums.size(); u++)
                            zbLabels.get(idx + u * (ui.gs.gc.length - 1)).setVisible(false);
                    }
                    resultLabels.get(idx).setVisible(false);
                    frameLabels.get(idx).setVisible(false);
                    frameLabels.get(idx).repaint();
                    ui.jPanel.repaint(label.getBounds());
                    break;
                }
                delBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            }
        });
        panel.add(delBtn);
        panel.setComponentZOrder(delBtn, 0);

        panel.setVisible(true);
        panel.revalidate();
        panel.repaint();
        this.ui.resizeComponents();
    }
}