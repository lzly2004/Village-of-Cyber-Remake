// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\vocr\src\ReplayPlayerHandler.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ReplayPlayerHandler implements SceneHandler {
    private static final int AVATAR_SIZE = 70;
    private static final int AVATAR_GAP = 10;
    private static final int AVATAR_START_X = 50;
    private static final int AVATAR_START_Y = 10;
    private boolean isShowingVote = false;
    private int voteCurrentRound = 0;

    @Override
    public void render(UI ui) {
        doRender(ui);
    }

    private void doRender(UI ui) {
        ui.jPanel.removeAll();

        // 确保播放复盘BGM（基于当前状态动态判断，避免从其他界面返回时不切换）
        String currentBgm = ui.resources.getCurrentBgmName();
        if (!"replay_bgm.wav".equals(currentBgm)) {
            DebugLogger.info("[ReplayPlayerHandler] 切换BGM: 之前=" + currentBgm + ", 新=replay_bgm.wav");
            boolean bgmOk = ui.resources.playBgm("replay_bgm.wav");
            if (!bgmOk) DebugLogger.warn("[ReplayPlayerHandler] 复盘BGM播放失败!");
        }

        if (ui.currentReplaySave == null || ui.currentReplaySave.daySnapshots.isEmpty()) {
            renderEmptyState(ui);
            return;
        }

        int day = ui.replayDay;
        if (day < 2) day = 2;
        if (day > ui.currentReplaySave.totalDays) day = ui.currentReplaySave.totalDays;
        ui.replayDay = day;

        DebugLogger.info("[ReplayPlayerHandler] 渲染回放: day=" + day +
                "/" + ui.currentReplaySave.totalDays);

        try {
            renderAvatarBar(ui, day);
            renderSkillTargets(ui, day);
            renderDataPanel(ui, day);
            renderInfoArea(ui, day);
            renderButtons(ui, day);
        } catch (Exception e) {
            DebugLogger.error("[ReplayPlayerHandler] 渲染组件时出错: " + e.getMessage());
            e.printStackTrace();
            showErrorState(ui, "渲染出错: " + e.getMessage());
            return;
        }

        addBackground(ui);
        finishRender(ui);
    }

    private void renderEmptyState(UI ui) {
        String message = "暂无存档数据";
        if (ui.currentReplaySave != null && ui.currentReplaySave.daySnapshots != null) {
            message = String.format("存档数据为空 (rawRecords=%d, daySnapshots=%d)",
                    ui.currentReplaySave.rawRecords != null ? ui.currentReplaySave.rawRecords.size() : 0,
                    ui.currentReplaySave.daySnapshots.size());
        } else if (ui.currentReplaySave == null) {
            message = "未选择存档";
        }
        showErrorState(ui, message);
    }

    private void showErrorState(UI ui, String errorMessage) {
        JTextArea errorText = TextareaSimpleFactory.createBoldTitleTextArea(Color.RED, 22, errorMessage);
        errorText.setBounds(300, 200, 600, 350);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorText.setEditable(false);
        errorText.setOpaque(false);
        ui.jPanel.add(errorText);

        JButton backBtn = createButton(ui, "PVBreturn.png", 530, 580, 194, 126);
        backBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
        });
        ui.jPanel.add(backBtn);

        addBackground(ui);
        finishRender(ui);
    }

    private JLabel addBackground(UI ui) {
        ImageIcon bgImg = ui.resources.getImage("PVBG.png");
        if (bgImg != null) {
            JLabel background = LabelSimpleFactory.makeLabel(
                    LabelConst.Simple_Label, 0, 0,
                    GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                    bgImg
            );
            ui.jPanel.add(background);
            ui.jPanel.setComponentZOrder(background, ui.jPanel.getComponentCount() - 1);
            return background;
        }
        return null;
    }

    private void finishRender(UI ui) {
        ui.resizeComponents();
        ui.jPanel.revalidate();
        ui.jPanel.repaint();
        ui.getJFrame().setVisible(true);
    }

    private String getStatusIcon(int deathReason, int actualRole) {
        switch (deathReason) {
            case 1: return "占"; // 占い師結果関連
            case 2: return "霊"; // 霊能者結果関連
            case 3: return "吊"; // 処刑
            default:
                if (actualRole == 7) return "狼";
                if (actualRole == 10) return "狐";
                return "×";
        }
    }






    private DaySnapshot getSnapshot(UI ui, int day) {
        if (ui.currentReplaySave == null || ui.currentReplaySave.daySnapshots.isEmpty())
            return null;

        for (DaySnapshot snap : ui.currentReplaySave.daySnapshots) {
            if (snap != null && snap.dayNumber == day) return snap;
        }

        int idx = day - 1;
        if (idx >= 0 && idx < ui.currentReplaySave.daySnapshots.size()) {
            return ui.currentReplaySave.daySnapshots.get(idx);
        }

        return null;
    }

    private int getPlayerSumFromSave(UI ui) {
        if (ui.currentReplaySave == null || ui.currentReplaySave.daySnapshots.isEmpty())
            return 0;

        DaySnapshot first = ui.currentReplaySave.daySnapshots.get(0);
        return (first != null && first.players != null) ? first.players.length : 0;
    }

    private String getActualRoleName(int actualRole) {
        switch (actualRole) {
            case 1: return "占卜师";
            case 2: return "灵媒师";
            case 3: return "猎人";
            case 4: return "共有者";
            case 5: return "猫又";
            case 6: return "村人";
            case 7: return "人狼";
            case 8: return "狂人";
            case 9: return "狂信者";
            case 10: return "妖狐";
            case 11: return "背德者";
            default: return "不明";
        }
    }

    private JButton createButton(UI ui, String imageName, int x, int y, int w, int h) {
        ImageIcon img = ui.resources.getImage(imageName);
        if (img == null) {
            DebugLogger.warn("[ReplayPlayerHandler] 按钮图片未找到: " + imageName);
            JButton fallbackBtn = new JButton(imageName);
            fallbackBtn.setBounds(x, y, w, h);
            return fallbackBtn;
        }
        return ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, w, h, img);
    }

    private JButton addButton(UI ui, String text, int x, int y, int w, int h, ActionListener listener) {
        ImageIcon btnImg = ui.resources.getImage("avg_button2.png");
        if (btnImg == null) {
            DebugLogger.warn("[ReplayPlayerHandler] 按钮背景图未找到: avg_button2.png");
            return null;
        }
        JButton btn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, w, h, btnImg);
        btn.setText(text);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setFont(new Font("Dialog", Font.BOLD, 18));
        btn.setForeground(Color.WHITE);
        btn.addActionListener(listener);
        return btn;
    }
    
    private void renderAvatarBar(UI ui, int day) {
        DaySnapshot snapshot = getSnapshot(ui, day + 1);
        if (snapshot == null) snapshot = getSnapshot(ui, day);
        if (snapshot == null || snapshot.players == null) {
            DebugLogger.warn("[ReplayPlayerHandler] 头像栏：无法获取快照数据");
            return;
        }

        int playerSum = snapshot.players.length;
        
        for (int i = 0; i < playerSum; i++) {
            try {
                DaySnapshot.PlayerStatus ps = snapshot.players[i];
                if (ps == null || ps.characterNumber == 0) continue;
                
                int playerIndex = i + 1;
                boolean isFirstRow = (playerIndex <= (playerSum + 1) / 2);
                
                StringBuilder xName = new StringBuilder();
                StringBuilder imageName = new StringBuilder();
                
                if (ps.characterNumber <= 9) imageName.append("0");
                imageName.append(ps.characterNumber);
                boolean isDead = ps.deathDay > 0 && ps.deathDay < day;

                // 复盘模式：根据实际身份显示不同颜色头像（对齐结算界面规则）
                imageName.append(GameStrings.getRoleImageSuffix(ps.actualRole));

                // 死亡图标照常显示（与生死状态无关）
                if (isDead) {
                    xName.append(whyDie.getDeathIconName(ps.deathReason));
                }
                imageName.append(".png");
                String textName = GameStrings.buildCharacterTextName(ps.characterNumber);
                
                // 构建CO职业图标名称
                String claimedRoleIconName = ui.uiComponentFactory.getClaimedRoleIconName(ps.claimedRole, ps.claimedRoleOrder);
                
                // 构建技能目标标记名称
                StringBuilder skillTargetName = new StringBuilder("result");
                if (ps.claimedRole > 0 && ps.claimedRole < 6 && ps.claimedRole <= 3) {
                    skillTargetName.append(ps.claimedRole).append("_").append(ps.claimedRoleOrder);
                    if (ps.skillTarget != 0) {
                        if (ps.claimedRole != 3) { // 非猎人
                            if (ps.skillTarget >= playerSum + 1) {
                                skillTargetName.append("black.png"); // 黑色结果
                            } else {
                                skillTargetName.append("white.png"); // 白色结果
                            }
                        } else { // 猎人
                            skillTargetName.append(".png");
                        }
                    } else {
                        DebugLogger.info(String.format(
                                "[ReplayPlayerHandler] 不显示CO图标: player=%d, role=%d, comingOutDay=%d, currentDay=%d",
                                i+1, ps.claimedRole, ps.comingOutDay, day));
                    }
                }
                
                // === 1. 添加CO职业图标 ===
                if (ps.claimedRole > 0 && ps.claimedRole < 6) {
                    if (ps.comingOutDay > 0) {
                        DebugLogger.info(String.format(
                                "[ReplayPlayerHandler] 显示CO图标: player=%d, role=%d, comingOutDay=%d, currentDay=%d",
                                i+1, ps.claimedRole, ps.comingOutDay, day));
                        ImageIcon claimedRoleIcon = ui.resources.getImage(claimedRoleIconName);
                        if (claimedRoleIcon != null) {
                            JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                            if (isFirstRow) {
                                claimedRoleLabel.setBounds(160 + 64 * playerIndex, 0,
                                        claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                            } else {
                                claimedRoleLabel.setBounds(160 + 64 * (playerIndex - ((playerSum + 1) / 2)), 98,
                                        claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                            }
                            ui.jPanel.add(claimedRoleLabel);
                        }
                    }
                }
                
                // === 2.添加死亡标记 ===
                if (!xName.isEmpty()) {
                    ImageIcon deathImage = ui.resources.getImage(xName.toString());
                    if (deathImage != null) {
                        JLabel deathLabel = new JLabel(deathImage);
                        if (isFirstRow) {
                            deathLabel.setBounds(165 + 64 * playerIndex, 10,
                                    deathImage.getIconWidth(), deathImage.getIconHeight());
                        } else {
                            deathLabel.setBounds(165 + 64 * (playerIndex - ((playerSum + 1) / 2)), 108,
                                    deathImage.getIconWidth(), deathImage.getIconHeight());
                        }
                        ui.jPanel.add(deathLabel);
                    }
                }
                
                // === 3. 添加头像和角色名 ===
                ImageIcon characterImage = ui.resources.getImage(imageName.toString());
                if (characterImage == null) {
                    DebugLogger.warn("[ReplayPlayerHandler] 头像图片未找到: " + imageName);
                    continue;
                }
                
                ImageIcon characterText = ui.resources.getImage(textName);
                JLabel label = new JLabel(characterImage);
                JLabel textLabel = null;
                
                if (isFirstRow) {
                    label.setBounds(160 + characterImage.getIconWidth() * playerIndex, 0,
                            characterImage.getIconWidth(), characterImage.getIconHeight());
                    if (characterText != null) {
                        textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                                175 + characterImage.getIconWidth() * playerIndex,
                                characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                                characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
                    }
                } else {
                    label.setBounds(160 + characterImage.getIconWidth() * (playerIndex - ((playerSum + 1) / 2)),
                            characterImage.getIconHeight(),
                            characterImage.getIconWidth(), characterImage.getIconHeight());
                    if (characterText != null) {
                        textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                                175 + characterImage.getIconWidth() * (playerIndex - ((playerSum + 1) / 2)),
                                2 * characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                                characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
                    }
                }
                
                if (textLabel != null) ui.jPanel.add(textLabel);
                ui.jPanel.add(label);
                
            } catch (Exception e) {
                DebugLogger.warn("[ReplayPlayerHandler] 渲染玩家" + (i+1) + "时出错: " + e.getMessage());
            }
        }
    }

    private void renderSkillTargets(UI ui, int day) {
        // 遍历所有已过的天数，累积显示所有技能结果（与对局中一致，从2日目开始）
        for (int d = 2; d <= day; d++) {
            DaySnapshot snapshot = getSnapshot(ui, d);
            if (snapshot == null || snapshot.players == null) continue;

            int playerSum = snapshot.players.length;
            int half = (playerSum + 1) / 2;

            for (int i = 0; i < playerSum; i++) {
                DaySnapshot.PlayerStatus ps = snapshot.players[i];
                if (ps == null || ps.characterNumber == 0) continue;

                // claimedRole在快照中可能延迟（CO发生在快照录制之后），
                // 回退使用actualRole确保占卜/灵能技能结果及时显示
                int displayRole = ps.claimedRole > 0 ? ps.claimedRole : ps.actualRole;
                if (displayRole != 1 && displayRole != 2) continue;

                // 死亡检查：已死亡的占卜/灵能师不再发球（与对局中GameSceneVoteHandler一致）
                if (ps.deathDay > 0 && ps.deathDay < d) continue;

                if (ps.skillTarget == 0) continue;

                int targetPlayer = ps.skillTarget;
                boolean isBlack = false;
                if (targetPlayer >= playerSum + 1) {
                    isBlack = true;
                    targetPlayer -= playerSum;
                }

                if (targetPlayer <= 0 || targetPlayer > playerSum) continue;

                int displayOrder = ps.claimedRoleOrder > 0 ? ps.claimedRoleOrder : 1;
                String imgName = "result" + displayRole + "_" + displayOrder;
                imgName += isBlack ? "black.png" : "white.png";

                ImageIcon stIcon = ui.resources.getImage(imgName);
                if (stIcon == null) {
                    DebugLogger.warn("[ReplayPlayerHandler] 技能图标未找到: " + imgName);
                    continue;
                }

                JLabel stLabel = new JLabel(stIcon);
                int targetX = 224 + 64 * (targetPlayer <= half ? targetPlayer : targetPlayer - playerSum / 2);
                int targetY = targetPlayer <= half ? 0 : 98;

                stLabel.setBounds(
                        targetX - stIcon.getIconWidth() * displayRole,
                        targetY + (displayOrder - 1) * stIcon.getIconHeight(),
                        stIcon.getIconWidth(),
                        stIcon.getIconHeight()
                );
                ui.jPanel.add(stLabel);
                ui.jPanel.setComponentZOrder(stLabel, 0);

                DebugLogger.info(String.format("[ReplayPlayerHandler] 技能图标: day=%d, src=%d->tgt=%d, img=%s, pos=(%d,%d)",
                        d, i + 1, targetPlayer, imgName, stLabel.getX(), stLabel.getY()));
            }
        }
    }

    private void renderDataPanel(UI ui, int day) {
        DaySnapshot snapshot = getSnapshot(ui, day);
        if (snapshot == null) {
            DebugLogger.warn("[ReplayPlayerHandler] 数据面板：无法获取快照数据");
            return;
        }

        int aliveCount = snapshot.aliveCount;
        int deathCount = getPlayerSumFromSave(ui) - aliveCount;
        int hangCount = (aliveCount > 1) ? (aliveCount - 1) / 2 : 0;

        DebugLogger.info(String.format("[ReplayPlayerHandler] 数据面板: day=%d, alive=%d, dead=%d, rope=%d",
                day, aliveCount, deathCount, hangCount));

        JTextArea dataText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 20,
                "    " + day + "日目\n 生存者:" + aliveCount +
                        "\n 死亡者:" + deathCount +
                        "\n 吊り縄:" + hangCount
        );
        dataText.setBounds(890, 25, 100, 130);
        dataText.setOpaque(false);
        ui.jPanel.add(dataText);
        DebugLogger.info("[ReplayPlayerHandler] 已添加数据文本 (890, 25)");

        ImageIcon dataBgImg = ui.resources.getImage("hiduke.png");
        if (dataBgImg == null) {
            DebugLogger.warn("[ReplayPlayerHandler] 日历背景图未找到: hiduke.png");
        } else {
            JLabel dataBg = LabelSimpleFactory.makeLabel(
                    LabelConst.Simple_Label, 880, 10,
                    dataBgImg
            );
            ui.jPanel.add(dataBg);
            DebugLogger.info("[ReplayPlayerHandler] 已添加日历背景图 (880, 10)");
        }
    }
    
    private void renderInfoArea(UI ui, int day) {
        try {
            int playerSum = getPlayerSumFromSave(ui);
            if (playerSum <= 0) { renderEmptyInfoPanel(ui); return; }

            ImageIcon boardIcon = ui.resources.getImage("frame #19252.png");
            if (boardIcon != null) {
                JLabel board = LabelSimpleFactory.makeLabel(
                        LabelConst.Simple_Label, 0, 198,
                        boardIcon.getIconWidth(), boardIcon.getIconHeight(),
                        boardIcon
                );
                ui.jPanel.add(board);
            }

            int totalDays = ui.currentReplaySave.totalDays;

            if (day >= totalDays) {
                renderRolePanel(ui, playerSum, boardIcon);
            } else if (isShowingVote) {
                renderVoteResultPanel(ui, day, playerSum, boardIcon);
            } else {
                renderNormalInfoPanel(ui, day, playerSum, boardIcon);
            }
        } catch (Exception e) {
            DebugLogger.error("[ReplayPlayerHandler] infoArea 异常: " + e.getMessage());
            e.printStackTrace();
            renderEmptyInfoPanel(ui);
        }
    }

    private void renderNormalInfoPanel(UI ui, int day, int playerSum, ImageIcon boardIcon) {

            StringBuilder zhanbu = new StringBuilder();
            StringBuilder lingneng = new StringBuilder();
            StringBuilder chuxing = new StringBuilder();
            StringBuilder shiti = new StringBuilder();
            StringBuilder lieren = new StringBuilder();

            DebugLogger.info("[ReplayPlayerHandler] infoArea: day=" + day + ", playerSum=" + playerSum);

            DaySnapshot firstSnap = getSnapshot(ui, day + 1);
            if (firstSnap == null) firstSnap = getSnapshot(ui, day);
            DebugLogger.info("[ReplayPlayerHandler] infoArea: firstSnap=" + (firstSnap != null ? "存在(p=" + firstSnap.players.length + ")" : "null"));

            if (firstSnap != null && firstSnap.players != null) {
                StringBuilder mapLog = new StringBuilder("[ReplayPlayerHandler] [CHAR-MAP] 全部映射:");
                for (int mi = 0; mi < firstSnap.players.length; mi++) {
                    DaySnapshot.PlayerStatus mps = firstSnap.players[mi];
                    if (mps != null) mapLog.append(" p").append(mi+1).append("=").append(mps.characterNumber).append("(").append(getShortName(mps.characterNumber)).append(")");
                }
                DebugLogger.info(mapLog.toString());
            }
            if (firstSnap != null && firstSnap.players != null) {
                for (int i = 0; i < playerSum; i++) {
                    DaySnapshot.PlayerStatus ps = firstSnap.players[i];
                    if (ps == null) continue;
                    switch (ps.claimedRole) {
                        case 1: appendSeerResults(ui, zhanbu, ps, i, playerSum, day); break;
                        case 2: appendSeerResults(ui, lingneng, ps, i, playerSum, day); break;
                        case 3: appendHunterResults(ui, lieren, ps, i, playerSum, day); break;
                    }
                }
            }

            for (int d = 1; d < day; d++) {
                DaySnapshot snap = getSnapshot(ui, d + 1);
                if (snap == null || snap.players == null) continue;

                int shitiCnt = 0;
                java.util.List<Integer> shitiNum = new java.util.ArrayList<>();
                boolean hasChuxing = false;

                for (int i = 0; i < playerSum; i++) {
                    DaySnapshot.PlayerStatus ps = snap.players[i];
                    if (ps == null) continue;

                    switch (ps.deathReason) {
                        case 1:
                            if (ps.deathDay == d) {
                                chuxing.append(getShortName(ps.characterNumber));
                                hasChuxing = true;
                            }
                            break;
                        case 2:
                            if (ps.deathDay == d) {
                                String cat = findRoleInSnapshot(snap, 5, playerSum);
                                if (!cat.isEmpty()) chuxing.append(cat).append("+");
                                chuxing.append(getShortName(ps.characterNumber)).append("(猫呪)");
                                hasChuxing = true;
                            }
                            break;
                        case 3:
                            if (ps.deathDay == d) {
                                String fox = findRoleInSnapshot(snap, 10, playerSum);
                                if (!fox.isEmpty()) chuxing.append(fox).append("+");
                                chuxing.append(getShortName(ps.characterNumber)).append("(後追)");
                                hasChuxing = true;
                            }
                            break;
                        default:
                            if (ps.deathReason >= 4 && ps.deathDay == d) {
                                shitiCnt++;
                                shitiNum.add(ps.characterNumber);
                            }
                            break;
                    }

                    if (i == playerSum - 1 && shitiCnt == 0) {
                        shiti.append(GameStrings.PEACE_ARROW);
                    }
                }

                if (hasChuxing) chuxing.append("\u2192");

                if (shitiCnt == 1) {
                    shiti.append(getShortName(shitiNum.get(0))).append("\u2192");
                } else if (shitiCnt > 1) {
                    for (int l = 0; l < shitiNum.size(); l++) {
                        shiti.append(getShortName(shitiNum.get(l))).append("+");
                    }
                    shiti.setLength(shiti.length() - 1);
                    shiti.append("\u2192");
                }
            }

            if (shiti.length() > 2) shiti.setLength(shiti.length() - 1);
            if (chuxing.length() > 2) chuxing.setLength(chuxing.length() - 1);

            DebugLogger.info("[ReplayPlayerHandler] infoArea: z=" + zhanbu.length() + " l=" + lingneng.length() + " c=" + chuxing.length() + " s=" + shiti.length() + " h=" + lieren.length());

            StringBuilder result = new StringBuilder();
            result.append(GameStrings.SECTION_SEER).append(zhanbu)
                  .append(GameStrings.SECTION_MEDIUM).append(lingneng)
                  .append(GameStrings.SECTION_EXECUTION).append(chuxing)
                  .append("\n[死体]\n").append(shiti)
                  .append("\n[護衛先]\n").append(lieren);

            DebugLogger.info("[ReplayPlayerHandler] infoArea: resultLen=" + result.length());

            JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 24, result.toString());
            JScrollPane scrollPane = new JScrollPane(infoText);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            int panelW = boardIcon != null ? 200 + boardIcon.getIconWidth() - 80 : 500;
            int panelH = boardIcon != null ? 50 + boardIcon.getIconHeight() - 60 : 300;
            scrollPane.setBounds(40, 228, panelW, panelH);
            ui.jPanel.add(scrollPane);
            ui.jPanel.setComponentZOrder(scrollPane, 0);
            DebugLogger.info("[ReplayPlayerHandler] infoArea: done");
    }

    private void renderRolePanel(UI ui, int playerSum, ImageIcon boardIcon) {
        DaySnapshot lastSnap = getSnapshot(ui, ui.currentReplaySave.totalDays);
        if (lastSnap == null) {
            lastSnap = getSnapshot(ui, ui.currentReplaySave.totalDays - 1);
        }
        if (lastSnap == null || lastSnap.players == null) {
            renderEmptyInfoPanel(ui);
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("一役職一\n");

        java.util.Map<Integer, java.util.List<String>> roleMap = new java.util.LinkedHashMap<>();
        for (int i = 0; i < playerSum; i++) {
            DaySnapshot.PlayerStatus ps = lastSnap.players[i];
            if (ps == null || ps.characterNumber == 0) continue;
            int role = ps.actualRole;
            String charName = getShortName(ps.characterNumber);
            roleMap.computeIfAbsent(role, k -> new java.util.ArrayList<>()).add(charName);
        }

        int[] roleOrder = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        for (int role : roleOrder) {
            java.util.List<String> chars = roleMap.get(role);
            if (chars == null || chars.isEmpty()) continue;
            result.append("[").append(getActualRoleName(role)).append("]\n");
            for (int j = 0; j < chars.size(); j++) {
                result.append(chars.get(j));
                if (j < chars.size() - 1) result.append("　");
            }
            result.append("\n");
        }

        JTextArea roleText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 22, result.toString());
        JScrollPane scrollPane = new JScrollPane(roleText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        int panelW = boardIcon != null ? 200 + boardIcon.getIconWidth() - 80 : 500;
        int panelH = boardIcon != null ? 50 + boardIcon.getIconHeight() - 60 : 300;
        scrollPane.setBounds(40, 228, panelW, panelH);
        ui.jPanel.add(scrollPane);
        ui.jPanel.setComponentZOrder(scrollPane, 0);

        DebugLogger.info("[ReplayPlayerHandler] rolePanel: 显示角色信息(按职业分组)");
    }

    private void renderVoteResultPanel(UI ui, int day, int playerSum, ImageIcon boardIcon) {
        DaySnapshot snapshot = getSnapshot(ui, day);
        DebugLogger.info("[ReplayPlayerHandler] [VOTE-DEBUG] day=" + day + ", snapshot=" + (snapshot != null ? "非null" : "NULL"));
        if (snapshot != null) {
            DebugLogger.info("[ReplayPlayerHandler] [VOTE-DEBUG] voteRounds.size=" + snapshot.voteRounds.size());
            if (!snapshot.voteRounds.isEmpty()) {
                DaySnapshot.VoteRound vr = snapshot.voteRounds.get(voteCurrentRound);
                DebugLogger.info("[ReplayPlayerHandler] [VOTE-DEBUG] currentRound=" + voteCurrentRound + ", vr.votes=" + (vr.votes != null ? vr.votes.length : "NULL"));
                if (vr.votes != null && vr.votes.length > 0) {
                    StringBuilder sampleData = new StringBuilder();
                    int limit = Math.min(3, vr.votes.length);
                    for (int i = 0; i < limit; i++) {
                        sampleData.append("[").append(vr.votes[i][0]).append("->").append(vr.votes[i][1]).append("] ");
                    }
                    DebugLogger.info("[ReplayPlayerHandler] [VOTE-DEBUG] 抽样数据: " + sampleData);
                }
            }
        }

        if (snapshot == null || snapshot.voteRounds.isEmpty()) {
            DebugLogger.warn("[ReplayPlayerHandler] [VOTE-ERROR] 无投票数据! snapshot=" + (snapshot == null ? "null" : "有但voteRounds空"));
            isShowingVote = false;
            voteCurrentRound = 0;
            renderNormalInfoPanel(ui, day, playerSum, boardIcon);
            return;
        }

        if (voteCurrentRound >= snapshot.voteRounds.size()) {
            voteCurrentRound = snapshot.voteRounds.size() - 1;
        }

        DaySnapshot.VoteRound vr = snapshot.voteRounds.get(voteCurrentRound);

        StringBuilder leftPiao = new StringBuilder();
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;

        StringBuilder extraText = new StringBuilder();
        if (snapshot.dailyVotingRule >= 0) {
            switch (snapshot.dailyVotingRule) {
                case 0: extraText.append(GameStrings.VOTE_FREE); break;
                case 1:
                    extraText.append(GameStrings.VOTE_GREY);
                    for (int playerIdx : snapshot.greyTargetCharNums) {
                        int charNum = getCharNumByTarget(snapshot, playerIdx);
                        DebugLogger.info("[VOTE-GREY] playerIdx=" + playerIdx + " -> charNum=" + charNum + " -> " + getShortName(charNum));
                        extraText.append(getShortName(charNum));
                    }
                    break;
                case 2:
                    extraText.append(GameStrings.VOTE_DESIGNATED);
                    for (int charNum : snapshot.designatedTargetCharNums) {
                        DebugLogger.info("[VOTE-DESIGN] charNum=" + charNum + " -> " + getShortName(charNum));
                        extraText.append(getShortName(charNum)).append(",");
                    }
                    if (!snapshot.designatedTargetCharNums.isEmpty()) extraText.setLength(extraText.length() - 1);
                    break;
            }
        }

        String title = GameStrings.getVoteHistoryTitle(day, vr.round, extraText.toString());
        DebugLogger.info("[ReplayPlayerHandler] [VOTE-TITLE] day=" + day + ", displayRound=" + vr.round + ", dataRound=" + vr.round + ", totalRounds=" + snapshot.voteRounds.size());
        leftPiao.append(title);

        if (vr.votes != null) {
            java.util.LinkedHashMap<Integer, Integer> dedupedVotes = new java.util.LinkedHashMap<>();
            int[] voterTargetMap = new int[playerSum + 1];
            int[] voteCount = new int[playerSum + 1];
            for (int[] vote : vr.votes) {
                if (vote == null || vote.length < 2) continue;
                int voterIdx = vote[0];
                int targetIdx = vote[1];
                if (voterIdx >= 1 && voterIdx <= playerSum && targetIdx >= 1) {
                    voterTargetMap[voterIdx] = targetIdx;
                    voteCount[targetIdx]++;
                }
            }
            int totalValidVotes = 0;
            for (int c : voteCount) totalValidVotes += c;
            DebugLogger.info("[ReplayPlayerHandler] [VOTE-DATA] 投票记录=" + vr.votes.length + ", 有效投票=" + totalValidVotes + ", 存活=" + snapshot.aliveCount);

            for (int i = 1; i <= playerSum; i++) {
                if (snapshot.players == null || i > snapshot.players.length || snapshot.players[i - 1] == null) continue;
                DaySnapshot.PlayerStatus ps = snapshot.players[i - 1];
                boolean isVotingPlayer = (ps.deathDay == 0 || ps.deathDay >= day);
                if (!isVotingPlayer && ps.deathDay != day) continue;

                String voterName = getShortName(ps.characterNumber);
                int targetIdx = voterTargetMap[i];
                String targetName;
                int count;

                if (targetIdx >= 1 && targetIdx <= playerSum) {
                    DaySnapshot.PlayerStatus targetPs = targetIdx <= snapshot.players.length ? snapshot.players[targetIdx - 1] : null;
                    targetName = (targetPs != null) ? getShortName(targetPs.characterNumber) : ("?" + targetIdx);
                    count = voteCount[i];
                } else if (targetIdx == 0) {
                    targetName = "棄権";
                    count = voteCount[i];
                } else {
                    continue;
                }
                String line = String.format(GameStrings.VOTE_LINE_FORMAT, voterName, count, targetName);
                if (leftCnt >= 10) rightPiao.append(line);
                else { leftPiao.append(line); leftCnt++; }
            }
        }

        JTextArea piaoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 20, leftPiao.toString());
        JScrollPane scrollPane = new JScrollPane(piaoText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        int panelW = boardIcon != null ? 200 + boardIcon.getIconWidth() - 80 : 500;
        int panelH = boardIcon != null ? 50 + boardIcon.getIconHeight() - 60 : 300;
        scrollPane.setBounds(40, 228, panelW * 2 / 3, panelH);
        ui.jPanel.add(scrollPane);
        ui.jPanel.setComponentZOrder(scrollPane, 1);

        JTextArea piaoText1 = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 20, rightPiao.toString());
        JScrollPane scrollPane1 = new JScrollPane(piaoText1);
        scrollPane1.setBorder(BorderFactory.createEmptyBorder());
        scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane1.setOpaque(false);
        scrollPane1.getViewport().setOpaque(false);
        scrollPane1.setBounds(40 + panelW * 2 / 3, 228, panelW / 3, panelH);
        ui.jPanel.add(scrollPane1);
        ui.jPanel.setComponentZOrder(scrollPane1, 1);
        ui.jPanel.setComponentZOrder(scrollPane1, 0);

        DebugLogger.info("[ReplayPlayerHandler] voteResultPanel: day=" + day + ", round=" + voteCurrentRound + "/" + snapshot.voteRounds.size());
    }

    private void toggleShowVote(UI ui) {
        isShowingVote = !isShowingVote;
        if (isShowingVote) {
            voteCurrentRound = 0;
        }
        ui.resources.playSound("click.wav");
        ui.run();
    }

    private void showVotePrevRound(UI ui) {
        if (voteCurrentRound > 0) {
            voteCurrentRound--;
            ui.resources.playSound("click.wav");
            ui.run();
        }
    }

    private void showVoteNextRound(UI ui) {
        DaySnapshot snap = null;
        if (ui.currentReplaySave != null && !ui.currentReplaySave.daySnapshots.isEmpty()) {
            int day = ui.replayDay > 0 ? ui.replayDay : 1;
            for (DaySnapshot s : ui.currentReplaySave.daySnapshots) {
                if (s != null && s.dayNumber == day) { snap = s; break; }
            }
        }
        if (snap != null && voteCurrentRound < snap.voteRounds.size() - 1) {
            voteCurrentRound++;
            ui.resources.playSound("click.wav");
            ui.run();
        }
    }



    private void renderEmptyInfoPanel(UI ui) {
        JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 20, "\u60c5\u5831\u3042\u308a\u307e\u3059\u3093");
        infoText.setBounds(60, 228, 500, 300);
        infoText.setOpaque(false);
        ui.jPanel.add(infoText);
    }

    private String safeCharName(int charNum) {
        if (charNum <= 0 || charNum >= CharacterKanjiName.values().length) {
            DebugLogger.warn("[ReplayPlayerHandler] characterNumber越界: " + charNum + ", 范围[1," + (CharacterKanjiName.values().length - 1) + "]");
            return "?" + charNum;
        }
        return CharacterKanjiName.values()[charNum].name();
    }

    private String getShortName(int charNum) {
        if (charNum <= 0 || charNum >= CharacterKanjiName.values().length) {
            return "?" + charNum;
        }
        return CharacterKanjiName.values()[charNum].getShortName();
    }

    private String findRoleInSnapshot(DaySnapshot snap, int targetRole, int playerSum) {
        if (snap == null || snap.players == null) return "";
        for (int i = 0; i < playerSum; i++) {
            DaySnapshot.PlayerStatus ps = snap.players[i];
            if (ps != null && ps.actualRole == targetRole) return getShortName(ps.characterNumber);
        }
        return "";
    }

    private void appendSkillResults(UI ui, StringBuilder sb, DaySnapshot.PlayerStatus ps,
                                   int idx, int playerSum, int startDay, int endDay,
                                   boolean useSafeName, boolean showBallResult, int dayOffset) {
        sb.append(useSafeName ? safeCharName(ps.characterNumber) : getShortName(ps.characterNumber)).append(" : ");
        for (int d = startDay; d < endDay; d++) {
            DaySnapshot snap = getSnapshot(ui, d + dayOffset);
            if (snap == null || snap.players == null || idx >= snap.players.length) continue;
            DaySnapshot.PlayerStatus p = snap.players[idx];
            if (p == null) continue;
            if (p.deathDay > 0 && d >= p.deathDay) break;
            if (p.skillTarget != 0) {
                int target = p.skillTarget;
                int effectiveTarget = target;
                boolean isBlack = false;
                if (target > playerSum) {
                    effectiveTarget = target - playerSum;
                    isBlack = true;
                }
                String targetName = useSafeName ? safeCharName(getCharNumByTarget(snap, effectiveTarget))
                        : getShortName(getCharNumByTarget(snap, effectiveTarget));
                if (showBallResult) {
                    sb.append(targetName).append(isBlack ? "\u25cf\u2192" : "\u25cb\u2192");
                } else {
                    sb.append(targetName).append("\u2192");
                }
            }
        }
        if (ps.nonHumanMarked) sb.append(GameStrings.MARKER_EXPOSED).append("\u2192");
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\u2192') sb.setLength(sb.length() - 1);
        sb.append("\n");
    }

    private void appendSeerResults(UI ui, StringBuilder sb, DaySnapshot.PlayerStatus basePs,
                                   int idx, int playerSum, int currentDay) {
        appendSkillResults(ui, sb, basePs, idx, playerSum, 1, currentDay, false, true, 1);
    }

    private void appendHunterResults(UI ui, StringBuilder sb, DaySnapshot.PlayerStatus ps,
                                     int idx, int playerSum, int currentDay) {
        appendSkillResults(ui, sb, ps, idx, playerSum, 2, currentDay, false, false, 1);
    }

    private int getCharNumByTarget(DaySnapshot snap, int targetPlayerIdx) {
        if (snap != null && snap.players != null && targetPlayerIdx >= 1 && targetPlayerIdx <= snap.players.length) {
            DaySnapshot.PlayerStatus ps = snap.players[targetPlayerIdx - 1];
            if (ps != null && ps.characterNumber != 0) {
                if (ps.characterNumber == targetPlayerIdx) {
                    DebugLogger.warn("[ReplayPlayerHandler] [CHAR-SUSPECT] p" + targetPlayerIdx
                            + ".charNum=" + ps.characterNumber + " == 座位号! name=" + getShortName(ps.characterNumber)
                            + " | 可能是存档记录错误或角色分配巧合");
                }
                DebugLogger.info("[CHAR-RESOLVE] tgtP" + targetPlayerIdx + " -> charNum=" + ps.characterNumber
                        + "(" + getShortName(ps.characterNumber) + ") | caller=" 
                        + Thread.currentThread().getStackTrace()[2].getMethodName());
                return ps.characterNumber;
            }
        }
        DebugLogger.warn("[ReplayPlayerHandler] [CHAR-FALLBACK] tgtP" + targetPlayerIdx
                + " -> 返回座位号(无映射)! snap=" + (snap != null ? "非null" : "null")
                + ", players=" + (snap != null && snap.players != null ? snap.players.length : "null")
                + ", ps=" + ((snap != null && snap.players != null && targetPlayerIdx >= 1 && targetPlayerIdx < snap.players.length)
                ? (snap.players[targetPlayerIdx - 1] != null ? "charNum=" + snap.players[targetPlayerIdx - 1].characterNumber : "null") : "越界"));
        return targetPlayerIdx;
    }
    
    private void renderButtons(UI ui, int day) {
        int totalDays = ui.currentReplaySave.totalDays;
        int btnX = 1050;
        int btnW = 194;
        int btnH = 126;

        if (day == 2) {
            JButton nextBtn = createButton(ui, "PVBnext.png", btnX, 250, btnW, btnH);
            if (nextBtn != null) {
                nextBtn.addActionListener(e -> {
                    ui.resources.playSound("click.wav");
                    if (voteCurrentRound != 0) return;
                    ui.replayDay++;
                    ui.run();
                });
                ui.jPanel.add(nextBtn);
            }


            JButton backBtn = createButton(ui, "PVBreturn.png", btnX, 400, btnW, btnH);
            if (backBtn != null) {
                backBtn.addActionListener(e -> {
                    ui.resources.playSound("click.wav");
                    DebugLogger.info("[ReplayPlayerHandler] 恢复BGM: 纯音乐-优美旋律.wav");
                    ui.resources.playBgm("纯音乐-优美旋律.wav");
                    ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
                });
                ui.jPanel.add(backBtn);
            }

            if (!isShowingVote) {
                DebugLogger.info("[ReplayPlayerHandler] [BTN-DEBUG] day=2, 准备创建PVtohyo按钮, isShowingVote=" + isShowingVote);
                JButton voteBtn = createButton(ui, "PVtohyo.png", btnX, 550, btnW, btnH);
                if (voteBtn != null) {
                    voteBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(voteBtn);
                    DebugLogger.info("[ReplayPlayerHandler] 添加投票入口(day=2): PVtohyo.png");
                } else {
                    DebugLogger.warn("[ReplayPlayerHandler] [BTN-ERROR] day=2, PVtohyo按钮创建失败!");
                }
            } else {
                JButton backInfoBtn = createButton(ui, "PVBreturn.png", btnX, 550, btnW, btnH);
                if (backInfoBtn != null) {
                    backInfoBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(backInfoBtn);
                }

                DaySnapshot snap2 = getSnapshot(ui, 2);
                int maxRound2 = snap2 != null ? snap2.voteRounds.size() - 1 : 0;

                if (voteCurrentRound > 0) {
                    JButton prevVoteBtn2 = createButton(ui, "PVRirekiBack.png", 1060, 10, btnW / 2, btnH / 2);
                    if (prevVoteBtn2 != null) {
                        prevVoteBtn2.addActionListener(e -> showVotePrevRound(ui));
                        ui.jPanel.add(prevVoteBtn2);
                    }
                }

                if (voteCurrentRound < maxRound2) {
                    JButton nextVoteBtn2 = createButton(ui, "PVRirekiNext.png", 1060 + btnW / 2 + 5, 10, btnW / 2, btnH / 2);
                    if (nextVoteBtn2 != null) {
                        nextVoteBtn2.addActionListener(e -> showVoteNextRound(ui));
                        ui.jPanel.add(nextVoteBtn2);
                    }
                }
            }
        } else if (day >= totalDays) {
            JButton prevBtn = createButton(ui, "PVBreturn.png", btnX, 250, btnW, btnH);
            if (prevBtn != null) {
                prevBtn.addActionListener(e -> {
                    ui.resources.playSound("click.wav");
                    if (voteCurrentRound != 0) return;
                    ui.replayDay--;
                    ui.run();
                });
                ui.jPanel.add(prevBtn);
            }

            JButton titleBtn = createButton(ui, "PVBtitile.png", btnX, 550, btnW, btnH);
            if (titleBtn != null) {
                titleBtn.addActionListener(e -> {
                        ui.resources.playSound("click.wav");
                        DebugLogger.info("[ReplayPlayerHandler] 恢复BGM: 纯音乐-优美旋律.wav");
                        ui.resources.playBgm("纯音乐-优美旋律.wav");
                        ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
                });
                ui.jPanel.add(titleBtn);
            }
        } else {
            JButton nextBtn = createButton(ui, "PVBnext.png", btnX, 250, btnW, btnH);
            if (nextBtn != null) {
                nextBtn.addActionListener(e -> {
                    ui.resources.playSound("click.wav");
                    if (voteCurrentRound != 0) return;
                    ui.replayDay++;
                    ui.run();
                });
                ui.jPanel.add(nextBtn);
            }

            JButton prevBtn = createButton(ui, "PVBreturn.png", btnX, 400, btnW, btnH);
            if (prevBtn != null) {
                prevBtn.addActionListener(e -> {
                    ui.resources.playSound("click.wav");
                    if (voteCurrentRound != 0) return;
                    ui.replayDay--;
                    ui.run();
                });
                ui.jPanel.add(prevBtn);
            }

            if (!isShowingVote) {
                DebugLogger.info("[ReplayPlayerHandler] [BTN-DEBUG] day>2, 准备创建PVtohyo按钮, isShowingVote=" + isShowingVote);
                JButton voteBtn = createButton(ui, "PVtohyo.png", btnX, 550, btnW, btnH);
                if (voteBtn != null) {
                    voteBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(voteBtn);
                    DebugLogger.info("[ReplayPlayerHandler] 添加投票入口按钮: PVtohyo.png");
                } else {
                    DebugLogger.warn("[ReplayPlayerHandler] [BTN-ERROR] day>2, PVtohyo按钮创建失败!");
                }
            } else {
                JButton backInfoBtn = createButton(ui, "PVBreturn.png", btnX, 550, btnW, btnH);
                if (backInfoBtn != null) {
                    backInfoBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(backInfoBtn);
                }

                DaySnapshot snap = getSnapshot(ui, day);
                int maxRound = snap != null ? snap.voteRounds.size() - 1 : 0;
                DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] voteCurrentRound=" + voteCurrentRound + ", maxRound=" + maxRound + ", totalRounds=" + (snap != null ? snap.voteRounds.size() : 0));

                if (voteCurrentRound > 0) {
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] 显示前结果按钮");
                    JButton prevVoteBtn = createButton(ui, "PVRirekiBack.png", 1060, 10, btnW / 2, btnH / 2);
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-CREATE] prevVoteBtn=" + (prevVoteBtn != null ? "非null" : "NULL") + ", pos=(1160,10," + (btnW/2) + "," + (btnH/2) + ")");
                    if (prevVoteBtn != null) {
                        prevVoteBtn.addActionListener(e -> showVotePrevRound(ui));
                        ui.jPanel.add(prevVoteBtn);
                        DebugLogger.info("[ReplayPlayerHandler] [BTN-ADDED] 前结果按钮已添加到jPanel");
                    } else {
                        DebugLogger.error("[ReplayPlayerHandler] [BTN-ERROR] 前结果按钮创建失败(返回null)!");
                    }
                } else {
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] 隐藏前结果按钮(voteCurrentRound=0)");
                }

                if (voteCurrentRound < maxRound) {
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] 显示次结果按钮");
                    int nextBtnX = 1060 + btnW / 2 + 5;
                    int nextBtnY = 10;
                    int nextBtnW = btnW / 2;
                    int nextBtnH = btnH / 2;
                    JButton nextVoteBtn = createButton(ui, "PVRirekiNext.png", nextBtnX, nextBtnY, nextBtnW, nextBtnH);
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-CREATE] nextVoteBtn=" + (nextVoteBtn != null ? "非null" : "NULL") + ", pos=(" + nextBtnX + "," + nextBtnY + "," + nextBtnW + "," + nextBtnH + ")");
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-CREATE] screenSize=" + ui.getJFrame().getWidth() + "x" + ui.getJFrame().getHeight());
                    if (nextVoteBtn != null) {
                        nextVoteBtn.addActionListener(e -> showVoteNextRound(ui));
                        ui.jPanel.add(nextVoteBtn);
                        DebugLogger.info("[ReplayPlayerHandler] [BTN-ADDED] 次结果按钮已添加到jPanel, 组件数=" + ui.jPanel.getComponentCount());
                    } else {
                        DebugLogger.error("[ReplayPlayerHandler] [BTN-ERROR] 次结果按钮创建失败(返回null)! 可能原因: PVRirekiNext.png不存在");
                    }
                } else {
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] 隐藏次结果按钮(voteCurrentRound=" + voteCurrentRound + ">= maxRound=" + maxRound + ")");
                }
            }
        }
    }
}