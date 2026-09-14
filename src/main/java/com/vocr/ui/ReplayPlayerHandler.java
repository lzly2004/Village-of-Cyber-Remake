package com.vocr.ui;

import com.vocr.resources.BgmTrack;
import com.vocr.resources.ImageNames;
import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;

import com.vocr.logging.DebugLogger;

import com.vocr.replay.DaySnapshot;

// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\src\main\java\com\vocr\ReplayPlayerHandler.java
import javax.swing.*;
import java.awt.*;

public class ReplayPlayerHandler implements SceneHandler {
    private boolean isShowingVote = false;
    private int voteCurrentRound = 0;

    @Override
    public void render(UI ui) {
        doRender(ui);
    }

    private void doRender(UI ui) {
        ui.jPanel.removeAll();

        // 确保播放复盘BGM（基于当前状态动态判断，避免从其他界面返回时不切换）
        BgmTrack currentBgm = ui.resources.getCurrentBgm();
        if (currentBgm != BgmTrack.REPLAY_PLAYER) {
            DebugLogger.info("[ReplayPlayerHandler] 切换到复盘播放BGM，之前=" + currentBgm);
            boolean bgmOk = ui.resources.playBgm(BgmTrack.REPLAY_PLAYER);
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
            DebugLogger.error("[ReplayPlayerHandler] 渲染组件时出错", e);
            showErrorState(ui, "渲染出错: " + e.getMessage());
            return;
        }

        addBackground(ui);
        UIHelpers.finishRender(ui);
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

        JButton backBtn = createButton(ui, UiImage.NAVIGATION_RETURN, 530, 580, 194, 126);
        backBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
        });
        ui.jPanel.add(backBtn);

        addBackground(ui);
        UIHelpers.finishRender(ui);
    }

    private JLabel addBackground(UI ui) {
        ImageIcon bgImg = ui.resources.getImage(UiImage.REPLAY_BACKGROUND);
        if (bgImg != null) {
            JLabel background = LabelSimpleFactory.makeLabel(
                    LabelConst.Simple_Label, 0, 0,
                    UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT,
                    bgImg
            );
            ui.jPanel.add(background);
            ui.jPanel.setComponentZOrder(background, ui.jPanel.getComponentCount() - 1);
            return background;
        }
        return null;
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

    private JButton createButton(UI ui, UiImage image, int x, int y, int w, int h) {
        ImageIcon img = ui.resources.getImage(image);
        if (img == null) {
            DebugLogger.warn("[ReplayPlayerHandler] 按钮图片未找到: " + image);
            JButton fallbackBtn = new JButton(image.name());
            fallbackBtn.setBounds(x, y, w, h);
            return fallbackBtn;
        }
        return ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, w, h, img);
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
                
                boolean isDead = ps.deathDay > 0 && ps.deathDay < day;

                // 复盘模式：根据实际身份显示不同颜色头像（对齐结算界面规则）
                String imageName = ImageNames.characterRolePortrait(
                        ps.characterNumber, ps.actualRole);
                
                // === 1. 添加CO职业图标 ===
                if (ps.claimedRole > 0 && ps.claimedRole < 6) {
                    if (ps.comingOutDay > 0) {
                        DebugLogger.info(String.format(
                                "[ReplayPlayerHandler] 显示CO图标: player=%d, role=%d, comingOutDay=%d, currentDay=%d",
                                i+1, ps.claimedRole, ps.comingOutDay, day));
                        int charIndex = UIHelpers.calculateRowIndex(playerIndex, playerSum);
                        int roleX = 160 + 64 * charIndex;
                        int roleY = isFirstRow ? 0 : 98;
                        JLabel claimedRoleLabel = UIHelpers.createClaimedRoleIcon(ui, ps.claimedRole,
                                ps.claimedRoleOrder, roleX, roleY);
                        if (claimedRoleLabel != null) ui.jPanel.add(claimedRoleLabel);
                    }
                }
                
                // === 2.添加死亡标记 ===
                if (isDead) {
                    int charIndex = UIHelpers.calculateRowIndex(playerIndex, playerSum);
                    int deathX = 165 + 64 * charIndex;
                    int deathY = isFirstRow ? 10 : 108;
                    JLabel deathLabel = UIHelpers.createDeathMarker(ui, ps.deathReason, deathX, deathY);
                    if (deathLabel != null) ui.jPanel.add(deathLabel);
                }
                
                // === 3. 添加头像和角色名 ===
                ImageIcon characterImage = ui.resources.getImage(imageName);
                if (characterImage == null) {
                    DebugLogger.warn("[ReplayPlayerHandler] 头像图片未找到: " + imageName);
                    continue;
                }
                
                JLabel label = new JLabel(characterImage);
                int charWidth = characterImage.getIconWidth();
                int charHeight = characterImage.getIconHeight();
                int charIndex = UIHelpers.calculateRowIndex(playerIndex, playerSum);
                
                JLabel textLabel = UIHelpers.createCharacterText(ui, ps.characterNumber,
                        175 + charWidth * charIndex,
                        isFirstRow ? charHeight : 2 * charHeight, false);
                if (textLabel != null) ui.jPanel.add(textLabel);
                
                if (isFirstRow) {
                    label.setBounds(160 + charWidth * charIndex, 0, charWidth, charHeight);
                } else {
                    label.setBounds(160 + charWidth * charIndex, charHeight, charWidth, charHeight);
                }
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
                if (!ps.isAlive(d)) continue;

                if (ps.skillTarget == 0) continue;

                int targetPlayer = ps.skillTarget;
                boolean isBlack = false;
                if (targetPlayer >= playerSum + 1) {
                    isBlack = true;
                    targetPlayer -= playerSum;
                }

                if (targetPlayer <= 0 || targetPlayer > playerSum) continue;

                int displayOrder = ps.claimedRoleOrder > 0 ? ps.claimedRoleOrder : 1;
                String imgName = ImageNames.skillResultIcon(displayRole, displayOrder, isBlack);

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

        ImageIcon dataBgImg = ui.resources.getImage(UiImage.CALENDAR);
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

            ImageIcon boardIcon = ui.resources.getImage(UiImage.REVIEW_BOARD);
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
            DebugLogger.warn("[ReplayPlayerHandler] infoArea 异常，显示空信息面板", e);
            renderEmptyInfoPanel(ui);
        }
    }

    private void renderNormalInfoPanel(UI ui, int day, int playerSum, ImageIcon boardIcon) {
        String result = ReplayTextFormatter.buildNormalInfo(ui.currentReplaySave, day, playerSum);
        DebugLogger.info("[ReplayPlayerHandler] infoArea: day=" + day
                + ", playerSum=" + playerSum + ", resultLen=" + result.length());

        JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 24, result);
        JScrollPane scrollPane = new JScrollPane(infoText);
        UIHelpers.configureScrollPane(scrollPane);
        int panelW = boardIcon != null ? 200 + boardIcon.getIconWidth() - 80 : 500;
        int panelH = boardIcon != null ? 50 + boardIcon.getIconHeight() - 60 : 300;
        scrollPane.setBounds(40, 228, panelW, panelH);
        ui.jPanel.add(scrollPane);
        ui.jPanel.setComponentZOrder(scrollPane, 0);
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

        String result = ReplayTextFormatter.buildRoleInfo(lastSnap, playerSum);

        JTextArea roleText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 22, result);
        JScrollPane scrollPane = new JScrollPane(roleText);
        UIHelpers.configureScrollPane(scrollPane);
        int panelW = boardIcon != null ? 200 + boardIcon.getIconWidth() - 80 : 500;
        int panelH = boardIcon != null ? 50 + boardIcon.getIconHeight() - 60 : 300;
        scrollPane.setBounds(40, 228, panelW, panelH);
        ui.jPanel.add(scrollPane);
        ui.jPanel.setComponentZOrder(scrollPane, 0);

        DebugLogger.info("[ReplayPlayerHandler] rolePanel: 显示角色信息(按职业分组)");
    }

    private void renderVoteResultPanel(UI ui, int day, int playerSum, ImageIcon boardIcon) {
        DaySnapshot snapshot = getSnapshot(ui, day);
        if (snapshot == null || snapshot.voteRounds.isEmpty()) {
            DebugLogger.warn("[ReplayPlayerHandler] [VOTE-ERROR] 无投票数据! snapshot=" + (snapshot == null ? "null" : "有但voteRounds空"));
            isShowingVote = false;
            voteCurrentRound = 0;
            renderNormalInfoPanel(ui, day, playerSum, boardIcon);
            return;
        }

        ReplayTextFormatter.VoteText voteText = ReplayTextFormatter.buildVoteInfo(
                snapshot, day, voteCurrentRound, playerSum);
        voteCurrentRound = voteText.roundIndex();

        JTextArea piaoText = TextareaSimpleFactory.createBoldTitleTextArea(
                Color.BLACK, 20, voteText.leftColumn());
        JScrollPane scrollPane = new JScrollPane(piaoText);
        UIHelpers.configureScrollPane(scrollPane);
        int panelW = boardIcon != null ? 200 + boardIcon.getIconWidth() - 80 : 500;
        int panelH = boardIcon != null ? 50 + boardIcon.getIconHeight() - 60 : 300;
        scrollPane.setBounds(40, 228, panelW * 2 / 3, panelH);
        ui.jPanel.add(scrollPane);
        ui.jPanel.setComponentZOrder(scrollPane, 1);

        JTextArea piaoText1 = TextareaSimpleFactory.createBoldTitleTextArea(
                Color.BLACK, 20, voteText.rightColumn());
        JScrollPane scrollPane1 = new JScrollPane(piaoText1);
        UIHelpers.configureScrollPane(scrollPane1);
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
        ui.resources.playSound(SoundEffect.CLICK);
        ui.run();
    }

    private void showVotePrevRound(UI ui) {
        if (voteCurrentRound > 0) {
            voteCurrentRound--;
            ui.resources.playSound(SoundEffect.CLICK);
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
            ui.resources.playSound(SoundEffect.CLICK);
            ui.run();
        }
    }

    private void renderEmptyInfoPanel(UI ui) {
        JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 20, "\u60c5\u5831\u3042\u308a\u307e\u3059\u3093");
        infoText.setBounds(60, 228, 500, 300);
        infoText.setOpaque(false);
        ui.jPanel.add(infoText);
    }

    private void renderButtons(UI ui, int day) {
        int totalDays = ui.currentReplaySave.totalDays;
        int btnX = 1050;
        int btnW = 194;
        int btnH = 126;

        if (day == 2) {
            JButton nextBtn = createButton(ui, UiImage.NAVIGATION_NEXT, btnX, 250, btnW, btnH);
            if (nextBtn != null) {
                nextBtn.addActionListener(e -> {
                    ui.resources.playSound(SoundEffect.CLICK);
                    if (voteCurrentRound != 0) return;
                    ui.replayDay++;
                    ui.run();
                });
                ui.jPanel.add(nextBtn);
            }

            JButton backBtn = createButton(ui, UiImage.NAVIGATION_RETURN, btnX, 400, btnW, btnH);
            if (backBtn != null) {
                backBtn.addActionListener(e -> {
                    ui.resources.playSound(SoundEffect.CLICK);
                    DebugLogger.info("[ReplayPlayerHandler] 恢复复盘浏览BGM");
                    ui.resources.playBgm(BgmTrack.REPLAY_BROWSER);
                    ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
                });
                ui.jPanel.add(backBtn);
            }

            if (!isShowingVote) {
                DebugLogger.info("[ReplayPlayerHandler] [BTN-DEBUG] day=2, 准备创建PVtohyo按钮, isShowingVote=" + isShowingVote);
                JButton voteBtn = createButton(ui, UiImage.REPLAY_VOTE, btnX, 550, btnW, btnH);
                if (voteBtn != null) {
                    voteBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(voteBtn);
                    DebugLogger.info("[ReplayPlayerHandler] 添加投票入口(day=2): PVtohyo.png");
                } else {
                    DebugLogger.warn("[ReplayPlayerHandler] [BTN-ERROR] day=2, PVtohyo按钮创建失败!");
                }
            } else {
                JButton backInfoBtn = createButton(
                        ui, UiImage.NAVIGATION_RETURN, btnX, 550, btnW, btnH);
                if (backInfoBtn != null) {
                    backInfoBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(backInfoBtn);
                }

                DaySnapshot snap2 = getSnapshot(ui, 2);
                int maxRound2 = snap2 != null ? snap2.voteRounds.size() - 1 : 0;

                if (voteCurrentRound > 0) {
                    JButton prevVoteBtn2 = createButton(
                            ui, UiImage.REPLAY_HISTORY_BACK, 1060, 10, btnW / 2, btnH / 2);
                    if (prevVoteBtn2 != null) {
                        prevVoteBtn2.addActionListener(e -> showVotePrevRound(ui));
                        ui.jPanel.add(prevVoteBtn2);
                    }
                }

                if (voteCurrentRound < maxRound2) {
                    JButton nextVoteBtn2 = createButton(ui, UiImage.REPLAY_HISTORY_NEXT,
                            1060 + btnW / 2 + 5, 10, btnW / 2, btnH / 2);
                    if (nextVoteBtn2 != null) {
                        nextVoteBtn2.addActionListener(e -> showVoteNextRound(ui));
                        ui.jPanel.add(nextVoteBtn2);
                    }
                }
            }
        } else if (day >= totalDays) {
            JButton prevBtn = createButton(ui, UiImage.NAVIGATION_RETURN, btnX, 250, btnW, btnH);
            if (prevBtn != null) {
                prevBtn.addActionListener(e -> {
                    ui.resources.playSound(SoundEffect.CLICK);
                    if (voteCurrentRound != 0) return;
                    ui.replayDay--;
                    ui.run();
                });
                ui.jPanel.add(prevBtn);
            }

            JButton titleBtn = createButton(ui, UiImage.TITLE_RETURN, btnX, 550, btnW, btnH);
            if (titleBtn != null) {
                titleBtn.addActionListener(e -> {
                        ui.resources.playSound(SoundEffect.CLICK);
                        DebugLogger.info("[ReplayPlayerHandler] 恢复复盘浏览BGM");
                        ui.resources.playBgm(BgmTrack.REPLAY_BROWSER);
                        ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
                });
                ui.jPanel.add(titleBtn);
            }
        } else {
            JButton nextBtn = createButton(ui, UiImage.NAVIGATION_NEXT, btnX, 250, btnW, btnH);
            if (nextBtn != null) {
                nextBtn.addActionListener(e -> {
                    ui.resources.playSound(SoundEffect.CLICK);
                    if (voteCurrentRound != 0) return;
                    ui.replayDay++;
                    ui.run();
                });
                ui.jPanel.add(nextBtn);
            }

            JButton prevBtn = createButton(ui, UiImage.NAVIGATION_RETURN, btnX, 400, btnW, btnH);
            if (prevBtn != null) {
                prevBtn.addActionListener(e -> {
                    ui.resources.playSound(SoundEffect.CLICK);
                    if (voteCurrentRound != 0) return;
                    ui.replayDay--;
                    ui.run();
                });
                ui.jPanel.add(prevBtn);
            }

            if (!isShowingVote) {
                DebugLogger.info("[ReplayPlayerHandler] [BTN-DEBUG] day>2, 准备创建PVtohyo按钮, isShowingVote=" + isShowingVote);
                JButton voteBtn = createButton(ui, UiImage.REPLAY_VOTE, btnX, 550, btnW, btnH);
                if (voteBtn != null) {
                    voteBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(voteBtn);
                    DebugLogger.info("[ReplayPlayerHandler] 添加投票入口按钮: PVtohyo.png");
                } else {
                    DebugLogger.warn("[ReplayPlayerHandler] [BTN-ERROR] day>2, PVtohyo按钮创建失败!");
                }
            } else {
                JButton backInfoBtn = createButton(
                        ui, UiImage.NAVIGATION_RETURN, btnX, 550, btnW, btnH);
                if (backInfoBtn != null) {
                    backInfoBtn.addActionListener(e -> toggleShowVote(ui));
                    ui.jPanel.add(backInfoBtn);
                }

                DaySnapshot snap = getSnapshot(ui, day);
                int maxRound = snap != null ? snap.voteRounds.size() - 1 : 0;
                DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] voteCurrentRound=" + voteCurrentRound + ", maxRound=" + maxRound + ", totalRounds=" + (snap != null ? snap.voteRounds.size() : 0));

                if (voteCurrentRound > 0) {
                    DebugLogger.info("[ReplayPlayerHandler] [BTN-NAV] 显示前结果按钮");
                    JButton prevVoteBtn = createButton(
                            ui, UiImage.REPLAY_HISTORY_BACK, 1060, 10, btnW / 2, btnH / 2);
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
                    JButton nextVoteBtn = createButton(
                            ui, UiImage.REPLAY_HISTORY_NEXT, nextBtnX, nextBtnY, nextBtnW, nextBtnH);
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
