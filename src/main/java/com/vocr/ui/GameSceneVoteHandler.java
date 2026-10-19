package com.vocr.ui;

import com.vocr.application.AskClaimByPlayersCommand;
import com.vocr.application.AskClaimByRoleCommand;
import com.vocr.application.ClaimedActorTargets;
import com.vocr.application.ExecuteVoteCommand;
import com.vocr.application.GameUpdate;
import com.vocr.application.SelectionStateSnapshot;
import com.vocr.application.SetAvoidanceCommand;
import com.vocr.application.SkillTargetSelections;
import com.vocr.application.VoteExecutionResult;
import com.vocr.application.VotingMethod;
import com.vocr.resources.BgmTrack;
import com.vocr.resources.ImageNames;
import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;

import com.vocr.config.GameConstants;

// GameSceneVoteHandler.java - 投票主界面场景处理器
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GameSceneVoteHandler implements SceneHandler {
    private UI ui;
    // === 提取的类（阶段二：Extract Class）===
    VoteButtonPanel votePanel = new VoteButtonPanel();
    VoteInfoRenderer voteInfo = new VoteInfoRenderer();
    private List<Integer> freeVoteCandidates = List.of();
    private List<Integer> greyVoteCandidates = List.of();
    private List<Integer> designatedVoteCandidates = List.of();
    private SelectionStateSnapshot selections;
    // === 保留在Handler中的UI组件 ===
    private JPanel hisPanel, infoPanel, infoZhanPanel, infoHuPanel, infoCoPanel;
    private JLabel levellb, background;
    // === 共享状态（实例字段）===
    private List<Integer> askList;
    private int currentRound;
    private boolean isViewingVoteHistory;
    private boolean isSelectingCo;
    private Runnable resetToAfternoon;

    @Override
    public void render(UI ui) {
        this.ui = ui;
        ui.jPanel.removeAll();
        if (DebugLogger.getInstance().isEnabled()) { ui.testBtn(); }

        // ========== 阶段1：背景与BGM ==========
        createBackground();
        createMusicButton();

        // ========== 阶段2：玩家状态渲染 ==========
        renderPlayerStatuses();

        // ========== 阶段3：技能目标渲染 ==========
        renderSkillTargets(ui.jPanel, 160, 64, 0, 98);

        // ========== 阶段4：投票信息面板 ==========
        createVoteInfoPanel();

        // ========== 阶段5：读取应用层权威状态 ==========
        refreshApplicationState();

        // ========== 阶段6：按钮创建 ==========
        createAllButtons();

        // ========== 阶段7：重置回调 ==========
        createResetToAfternoon();

        // ========== 阶段8：事件处理绑定 ==========
        wireAllEventHandlers();

        // ========== 阶段9：最终布局 ==========
        ui.jPanel.add(voteInfo.scrollPane);
        ui.jPanel.add(voteInfo.board);
        ui.jPanel.add(background);
        ui.getJFrame().setVisible(true);
        ui.resizeComponents();
    }

    // ===================================================================
    // 阶段1提取：背景渲染
    // ===================================================================
    private void createBackground()
    {
        background = UIHelpers.createBackgroundLabel(ui, UiImage.DAY_BACKGROUND);
    }

    // ===================================================================
    // 阶段1提取：BGM按钮
    // ===================================================================
    private void createMusicButton()
    {
        List<BgmTrack> playlist = (ui.ctx.getAliveCounter() - 1) / 2 == 1
                ? BgmTrack.lateGamePlaylist()
                : BgmTrack.normalGamePlaylist();
        ImageIcon musicBtnIcon = ui.resources.getImage(UiImage.MUSIC_BUTTON);
        JButton btnMusic = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 15, 35,
                UIConstants.RETURN_WIDTH, UIConstants.RETURN_HEIGHT, musicBtnIcon);
        btnMusic.addActionListener(e -> {
            BgmTrack selectedTrack = playlist.get(ui.count % playlist.size());
            ui.count++;
            ui.resources.playBgm(selectedTrack);
        });
        ui.jPanel.add(btnMusic);
    }

    // ===================================================================
    // 阶段1提取：玩家状态渲染
    // ===================================================================
    private void renderPlayerStatuses() {
        PlayerStatusRenderer.render(ui);
    }

    // ===================================================================
    // 阶段4提取：投票信息面板（日期+占卜/灵能/处刑/尸体/猎人结果）
    // ===================================================================
    private void createVoteInfoPanel()
    {
        voteInfo.render(ui);
    }

    // ===================================================================
    // 阶段5提取：所有按钮创建
    // ===================================================================
    private void createAllButtons()
    {
        votePanel.create(ui, selections.avoidanceEnabled());
    }

    // ===================================================================
    // 阶段5：应用层权威投票候选与当前选择
    // ===================================================================
    private void refreshApplicationState()
    {
        GameUpdate update = ui.currentGameUpdate();
        selections = update.selections();
        freeVoteCandidates = update.legalVote(VotingMethod.FREE)
                .map(action -> action.candidates()).orElseGet(List::of);
        greyVoteCandidates = update.legalVote(VotingMethod.GREY)
                .map(action -> action.candidates()).orElseGet(List::of);
        designatedVoteCandidates = update.legalVote(VotingMethod.DESIGNATED)
                .map(action -> action.candidates()).orElseGet(List::of);
    }

    // ===================================================================
    // 阶段7提取：重置回调
    // ===================================================================
    private void createResetToAfternoon()
    {
        resetToAfternoon = () -> {
            votePanel.votehisBtn.setVisible(false); votePanel.doubtBtn.setVisible(false); votePanel.recordBtn.setVisible(false);
            votePanel.coBtn.setVisible(false); votePanel.ppBtn.setVisible(false); votePanel.returnBtn.setVisible(false);
            votePanel.reiBtn.setVisible(false); votePanel.kariBtn.setVisible(false); votePanel.uranaiBtn.setVisible(false);
            votePanel.kyouyuBtn.setVisible(false); votePanel.catBtn.setVisible(false); votePanel.askCoBtn.setVisible(false);
            votePanel.greyBtn.setVisible(false); votePanel.freeBtn.setVisible(false); votePanel.readyVoteBtn.setVisible(false);
            votePanel.nextBtn.setVisible(false); votePanel.againBtn.setVisible(false);
            ui.currentScene = UI.Scene.DIALOGUE_AFTERNOON;
            ui.run();
        };
    }

    // ===================================================================
    // 阶段8提取：所有事件处理器绑定
    // ===================================================================
    private void wireAllEventHandlers()
    {
        // --- 疑いボタン ---
        votePanel.doubtBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            votePanel.doubtBtn.setVisible(false); votePanel.votehisBtn.setVisible(false); voteInfo.infoText.setVisible(false);
            createDoubt();
        });

        hisPanel = PanelSimpleFactory.createSimplePanel(0, 0, false, false);
        hisPanel.setBounds(0, 198, 200 + voteInfo.boardIcon.getIconWidth(), 50 + voteInfo.boardIcon.getIconHeight());
        ui.jPanel.add(hisPanel);
        hisPanel.setVisible(false);
        ImageIcon levelIcon = ui.resources.getImage(ui.levelName);
        levellb = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 700, 600, levelIcon);
        levellb.setVisible(false); ui.jPanel.add(levellb);
        isViewingVoteHistory = false;
        votePanel.votehisBtn.addActionListener(e -> {
            isViewingVoteHistory = true;
            VoteHistoryBrowser.render(ui, votePanel, voteInfo, hisPanel, levellb, this);
        });

        // --- 投票ボタン ---
        votePanel.voteBtn.addActionListener(e -> handleVoteButton());

        currentRound = 1;

        // --- 灰投票 ---
        votePanel.greyBtn.addActionListener(e -> handleGreyVoteButton());

        // --- 履歴チェック ---
        votePanel.recordBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            if (ui.ctx.getGameDay() == 2) {
                createTishi(GameStrings.MSG_NO_DOUBT_YET);
            } else {
                votePanel.voteBtn.setVisible(false); votePanel.pointBtn.setVisible(false); votePanel.avoidBtn.setVisible(false);
                votePanel.recordBtn.setVisible(false); votePanel.avoidBtn1.setVisible(false);
                votePanel.votehisBtn.setVisible(true); votePanel.doubtBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
                ui.resizeComponents();
            }
        });

        // --- 回避切替 ---
        votePanel.avoidBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            setAvoidance(false);
            votePanel.avoidBtn1.setVisible(true); votePanel.avoidBtn.setVisible(false);
            ui.resizeComponents();
        });
        votePanel.avoidBtn1.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            setAvoidance(true);
            votePanel.avoidBtn1.setVisible(false); votePanel.avoidBtn.setVisible(true);
            ui.resizeComponents();
        });

        // --- 指定/CO ---
        votePanel.pointBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            votePanel.voteBtn.setVisible(false); votePanel.pointBtn.setVisible(false); votePanel.avoidBtn.setVisible(false);
            votePanel.recordBtn.setVisible(false); votePanel.avoidBtn1.setVisible(false);
            votePanel.coBtn.setVisible(true); votePanel.ppBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            ui.resizeComponents();
        });

        votePanel.coBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            votePanel.coBtn.setVisible(false); votePanel.ppBtn.setVisible(false);
            votePanel.reiBtn.setVisible(true); votePanel.kariBtn.setVisible(true); votePanel.askCoBtn.setVisible(true);
            votePanel.uranaiBtn.setVisible(true); votePanel.kyouyuBtn.setVisible(true); votePanel.catBtn.setVisible(true);
            ui.resizeComponents();
        });

        votePanel.ppBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            votePanel.coBtn.setVisible(false); votePanel.ppBtn.setVisible(false); votePanel.returnBtn.setVisible(false);
            votePanel.fixedVoteBtn.setVisible(true); votePanel.fixedUranaiBtn.setVisible(true); votePanel.protectBtn.setVisible(true);
            ui.resizeComponents();
        });

        // --- 情報パネル ---
        infoPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);
        infoZhanPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);
        infoHuPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);
        infoCoPanel = PanelSimpleFactory.createSimplePanel(0, 0, true, false);

        askList = new ArrayList<>();
        isSelectingCo = false;

        // --- 戻るボタン ---
        votePanel.returnBtn.addActionListener(e -> handleReturnButton());

        // --- 自由投票 ---
        votePanel.freeBtn.addActionListener(e -> handleFreeVoteButton());

        // --- 再投票 ---
        votePanel.againBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            boolean isReVote;
            if (ui.ctx.getEndResult() == GameResult.NONE) {
                isReVote = VoteResultRenderer.renderPiao(
                        ui, GameStrings.getVoteTitleRedo(ui.ctx.getGameDay() - 1), currentRound, this);
            } else {
                isReVote = VoteResultRenderer.renderPiao(
                        ui, GameStrings.getVoteTitleRedo(ui.ctx.getGameDay()), currentRound, this);
            }
            handleReVoteResultWithLimit(isReVote);
        });

        // --- 次へ ---
        votePanel.nextBtn.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            ui.currentScene = UI.Scene.DIALOGUE_CHUXING;
            ui.run();
        });

        // --- 指定投票 ---
        votePanel.readyVoteBtn.addActionListener(e -> handleReadyVoteButton());

        // --- CO询问 ---
        votePanel.askCoBtn.addActionListener(e -> {
            isSelectingCo = true;
            CoSelectionPanel.render(ui, votePanel, voteInfo, infoCoPanel, askList, this);
        });

        // --- CO按钮 ---
        votePanel.reiBtn.addActionListener(e -> handleRoleCo(Role.ling, GameStrings.MSG_NO_MEDIUM));
        votePanel.kariBtn.addActionListener(e -> handleRoleCo(Role.lie, GameStrings.MSG_NO_HUNTER));
        votePanel.uranaiBtn.addActionListener(e -> handleRoleCo(Role.zhan, GameStrings.MSG_NO_SEER));
        votePanel.kyouyuBtn.addActionListener(e -> handleRoleCo(Role.gong, GameStrings.MSG_NO_SHARED));
        votePanel.catBtn.addActionListener(e -> handleRoleCo(Role.mao, GameStrings.MSG_NO_CAT));

        // --- 指定パネル ---
        votePanel.fixedVoteBtn.addActionListener(e -> CharacterSelectionPanel.render(
                ui, votePanel, voteInfo, infoPanel, SelectionType.VOTE, this));
        votePanel.fixedUranaiBtn.addActionListener(e -> CharacterSelectionPanel.render(
                ui, votePanel, voteInfo, infoZhanPanel, SelectionType.DIVINATION, this));
        votePanel.protectBtn.addActionListener(e -> CharacterSelectionPanel.render(
                ui, votePanel, voteInfo, infoHuPanel, SelectionType.GUARD, this));
    }

    private void setAvoidance(boolean enabled)
    {
        GameUpdate update = ui.gameSession.setAvoidance(
                new SetAvoidanceCommand(ui.gameRevision(), enabled));
        ui.acceptGameUpdate(update);
        selections = update.selections();
    }

    // ===================================================================
    // 阶段8提取：按钮事件处理器
    // ===================================================================

    private void handleVoteButton()
    {
        refreshApplicationState();
        voteInfo.infoText.setVisible(false);
        ui.resources.playSound(SoundEffect.CLICK);
        votePanel.voteBtn.setVisible(false); votePanel.pointBtn.setVisible(false); votePanel.avoidBtn.setVisible(false);
        votePanel.recordBtn.setVisible(false); votePanel.avoidBtn1.setVisible(false);
        if (!selections.voteTargets().isEmpty()) {
            votePanel.readyVoteBtn.setVisible(true); votePanel.greyBtn.setVisible(false); votePanel.freeBtn.setVisible(false);
        } else {
            if (!greyVoteCandidates.isEmpty()) votePanel.greyBtn.setVisible(true);
            votePanel.freeBtn.setVisible(true); votePanel.readyVoteBtn.setVisible(false);
        }
        if (greyVoteCandidates.isEmpty()) votePanel.greyBtn.setVisible(false);
        votePanel.returnBtn.setVisible(true); votePanel.scrollPane1.setVisible(true);

        JTextArea isSelectedVoteTargetText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, UIConstants.FONT_SIZE_VOTE, "");
        StringBuilder isSelectedVoteTargetResult = new StringBuilder("-指定内容-\n");
        if (!selections.voteTargets().isEmpty()) {
            isSelectedVoteTargetResult.append(GameStrings.SPECIFY_VOTE);
            for (int target : selections.voteTargets())
                isSelectedVoteTargetResult.append(ui.getJobText(target)).append(" ");
            isSelectedVoteTargetResult.append("\n");
        }
        if (selections.divination().hasSelections()) {
            isSelectedVoteTargetResult.append(GameStrings.SPECIFY_DIVINATION);
            appendSkillSelections(isSelectedVoteTargetResult, selections.divination());
        }
        if (DebugLogger.getInstance().isEnabled()) {
            DebugLogger.log("是否护卫" + selections.guard().hasSelections());
            for (ClaimedActorTargets actor : selections.guard().claimedActors())
                for (int target : actor.targets())
                    DebugLogger.log(ui.getJobText(actor.playerNumber())
                            + "护卫了" + ui.getJobText(target));
        }
        if (selections.guard().hasSelections()) {
            isSelectedVoteTargetResult.append(GameStrings.SPECIFY_GUARD);
            appendSkillSelections(isSelectedVoteTargetResult, selections.guard());
        }
        isSelectedVoteTargetText.setText(isSelectedVoteTargetResult.toString());
        votePanel.scrollPane1.getViewport().setView(isSelectedVoteTargetText);
        UIHelpers.configureScrollPane(votePanel.scrollPane1);
        votePanel.scrollPane1.setBounds(40, 228, 200 + voteInfo.boardIcon.getIconWidth() - 80,
                50 + voteInfo.boardIcon.getIconHeight() - 60);
        isSelectedVoteTargetText.setVisible(true);
        ui.jPanel.setComponentZOrder(votePanel.scrollPane1, 0);
        ui.resizeComponents();
    }

    private void appendSkillSelections(
            StringBuilder text, SkillTargetSelections skillSelections)
    {
        for (ClaimedActorTargets actor : skillSelections.claimedActors())
        {
            text.append(ui.getJobText(actor.playerNumber())).append("→");
            for (int target : actor.targets())
                text.append(ui.getJobText(target)).append(",");
            text.append("\n");
        }
        if (!skillSelections.hiddenTargets().isEmpty())
        {
            text.append(GameStrings.HIDDEN_ARROW);
            for (int target : skillSelections.hiddenTargets())
                text.append(ui.getJobText(target)).append(",");
        }
        text.append("\n");
    }

    private void handleGreyVoteButton()
    {
        votePanel.freeBtn.setVisible(false); votePanel.greyBtn.setVisible(false); votePanel.returnBtn.setVisible(false);
        votePanel.scrollPane1.setVisible(false);
        StringBuilder greyText = new StringBuilder();
        for (int candidate : greyVoteCandidates)
            greyText.append(ui.getJobText(candidate));
        boolean isReVote;
        if (!greyVoteCandidates.isEmpty())
            DebugLogger.log("cxList不为空，且具体为" + greyVoteCandidates);
        VoteExecutionResult result = ui.gameSession.executeVote(
                new ExecuteVoteCommand(
                        ui.gameRevision(), VotingMethod.GREY,
                        greyVoteCandidates));
        ui.acceptGameUpdate(result.update());
        if (result.voteCompleted()) {
            int trueDay = ui.ctx.getEffectiveGameDay();
            isReVote = VoteResultRenderer.renderPiao(
                    ui, String.format(GameStrings.VOTE_TITLE_GREY, trueDay) + greyText + "\n",
                    currentRound, this);
            handleReVoteResult(isReVote);
        } else {
            votePanel.freeBtn.setVisible(true); votePanel.greyBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            votePanel.scrollPane1.setVisible(true);
            resetToAfternoon.run();
        }
    }

    private void handleReturnButton()
    {
        refreshApplicationState();
        if (isViewingVoteHistory) { ui.run(); }
        ui.resources.playSound(SoundEffect.CLICK);
        votePanel.voteBtn.setVisible(true); votePanel.pointBtn.setVisible(true); voteInfo.infoText.setVisible(true);
        ui.piaoText.setVisible(false); ui.piaoText1.setVisible(false);
        if (selections.avoidanceEnabled()) { votePanel.avoidBtn.setVisible(true); votePanel.avoidBtn1.setVisible(false); }
        else { votePanel.avoidBtn1.setVisible(true); votePanel.avoidBtn.setVisible(false); }
        levellb.setVisible(false);
        infoPanel.setVisible(false); infoZhanPanel.setVisible(false); infoHuPanel.setVisible(false);
        votePanel.scrollPane1.setVisible(false); hisPanel.setVisible(false); infoCoPanel.setVisible(false);
        votePanel.votehisBtn.setVisible(false); votePanel.doubtBtn.setVisible(false); votePanel.recordBtn.setVisible(true);
        votePanel.coBtn.setVisible(false); votePanel.ppBtn.setVisible(false); votePanel.returnBtn.setVisible(false);
        votePanel.reiBtn.setVisible(false); votePanel.kariBtn.setVisible(false); votePanel.uranaiBtn.setVisible(false);
        votePanel.kyouyuBtn.setVisible(false); votePanel.catBtn.setVisible(false); votePanel.askCoBtn.setVisible(false);
        votePanel.greyBtn.setVisible(false); votePanel.freeBtn.setVisible(false); votePanel.readyVoteBtn.setVisible(false);
        votePanel.nextBtn.setVisible(false); votePanel.againBtn.setVisible(false);
        if (!askList.isEmpty() && isSelectingCo) {
            isSelectingCo = false;
            DebugLogger.log(askList);
            GameUpdate update = ui.gameSession.requestClaims(
                    new AskClaimByPlayersCommand(ui.gameRevision(), askList));
            ui.acceptGameUpdate(update);
            if (!update.events().isEmpty()) {
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            } else {
                createTishi(GameStrings.MSG_NO_CO);
            }
        }
        ui.resizeComponents();
    }

    private void handleFreeVoteButton()
    {
        votePanel.freeBtn.setVisible(false); votePanel.greyBtn.setVisible(false); votePanel.returnBtn.setVisible(false);
        ui.resources.playSound(SoundEffect.CLICK); votePanel.scrollPane1.setVisible(false);
        List<Integer> chuxingList = freeVoteCandidates;
        DebugLogger.log("自由投票" + chuxingList);
        boolean isReVote;
        VoteExecutionResult result = ui.gameSession.executeVote(
                new ExecuteVoteCommand(
                        ui.gameRevision(), VotingMethod.FREE, chuxingList));
        ui.acceptGameUpdate(result.update());
        if (result.voteCompleted()) {
            if (ui.ctx.getEndResult() == GameResult.NONE) {
                isReVote = VoteResultRenderer.renderPiao(
                        ui, GameStrings.getVoteTitleFree(ui.ctx.getGameDay() - 1), currentRound, this);
            } else {
                isReVote = VoteResultRenderer.renderPiao(
                        ui, GameStrings.getVoteTitleFree(ui.ctx.getGameDay()), currentRound, this);
            }
            handleReVoteResult(isReVote);
        } else {
            votePanel.freeBtn.setVisible(true); votePanel.greyBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            votePanel.scrollPane1.setVisible(true);
            resetToAfternoon.run();
        }
    }

    private void handleReadyVoteButton()
    {
        votePanel.readyVoteBtn.setVisible(false); votePanel.returnBtn.setVisible(false); votePanel.scrollPane1.setVisible(false);
        List<Integer> chuxingList = designatedVoteCandidates;
        for (int candidate : chuxingList)
            DebugLogger.log("指定了" + candidate);
        DebugLogger.log("指定投票" + chuxingList);
        boolean isReVote;
        VoteExecutionResult result = ui.gameSession.executeVote(
                new ExecuteVoteCommand(
                        ui.gameRevision(), VotingMethod.DESIGNATED,
                        chuxingList));
        ui.acceptGameUpdate(result.update());
        if (result.voteCompleted()) {
            int trueDay = ui.ctx.getEffectiveGameDay();
            StringBuilder isSelectedVoteTargetText = new StringBuilder();
            for (int i = 0; i < chuxingList.size(); ++i)
                isSelectedVoteTargetText.append(ui.getJobText(chuxingList.get(i))).append(",");
            DebugLogger.log(isSelectedVoteTargetText);
            isReVote = VoteResultRenderer.renderPiao(
                    ui, GameStrings.getVoteTitleDesign(trueDay, isSelectedVoteTargetText.toString()),
                    currentRound, this);
            handleReVoteResult(isReVote);
        } else {
            votePanel.freeBtn.setVisible(true); votePanel.greyBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            votePanel.scrollPane1.setVisible(true);
            resetToAfternoon.run();
        }
    }

    // ===================================================================
    // 以下为原有私有方法（保持不变）
    // ===================================================================
    void stylePiaoTextArea(JTextArea area, String text, int x, int y, int w, int h) {
        area.setText(text);
        TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, UIConstants.FONT_SIZE_VOTE, text);
        area.setForeground(Color.black);
        area.setFont(new Font(UIConstants.FONT_FAMILY, Font.BOLD, UIConstants.FONT_SIZE_VOTE));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBackground(UIConstants.COLOR_TRANSPARENT);
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
        Timer timer = new Timer(UIConstants.TRANSITION_SHORT_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tishiText.setVisible(false);
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
        this.ui.resizeComponents();
    }

    private void handleRoleCo(Role role, String noRoleMsg) {
        ui.resources.playSound(SoundEffect.CLICK);
        GameUpdate update = ui.gameSession.requestClaims(
                new AskClaimByRoleCommand(ui.gameRevision(), role));
        ui.acceptGameUpdate(update);
        if (update.events().isEmpty()) {
            createTishi(noRoleMsg);
        } else {
            ui.currentScene = UI.Scene.DIALOGUE_DAY;
            ui.run();
        }
    }

    private void createDoubt() {
        this.ui.piaoText.setVisible(true);
        this.ui.piaoText1.setVisible(true);
        StringBuilder leftPiao = new StringBuilder(GameStrings.DOUBT_TITLE);
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        for (int i = 1; i <= this.ui.ctx.getPlayerSum(); ++i) {
            if (this.ui.ctx.isAlive(i)) {
                ArrayList<Integer> charas = new ArrayList<>();
                for (int t = 1; t <= 3; ++t) {
                    if (this.ui.ctx.getTop3SuspectedPlayer(i, t, this.ui.ctx.getGameDay()) != 0) {
                        charas.add(this.ui.ctx.getTop3SuspectedPlayer(i, t, this.ui.ctx.getGameDay()));
                    }
                }
                ArrayList<String> cmps = new ArrayList<>();
                for (int t = 0; t <= charas.size() - 2; ++t) {
                    int temp = this.ui.ctx.getSuspicionValue(i, charas.get(t)) - this.ui.ctx.getSuspicionValue(i, charas.get(t + 1));
                    if (temp <= 2) cmps.add("=");
                    else if (temp <= 5) cmps.add("≧");
                    else if (temp <= 10) cmps.add(">");
                    else cmps.add("≫");
                }
                StringBuilder targetPiao = leftCnt >= 10 ? rightPiao : leftPiao;
                appendVotePanel(targetPiao, i, cmps);
                if (leftCnt < 10) leftCnt++;
            }
        }
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);
        stylePiaoTextArea(this.ui.piaoText, leftPiao.toString(), 40, 228, 900, 430);
        stylePiaoTextArea(this.ui.piaoText1, rightPiao.toString(), 530, 228, 450, 430);
        this.ui.resizeComponents();
    }

    int charGridX(int i, int baseX, int spacing) {
        int half = (this.ui.ctx.getPlayerSum() + 1) / 2;
        return baseX + spacing * (i <= half ? i : i - this.ui.ctx.getPlayerSum() / 2);
    }

    int charGridY(int i, int yTop, int yBottom) {
        return i <= (this.ui.ctx.getPlayerSum() + 1) / 2 ? yTop : yBottom;
    }

    String charImageName(int i) {
        return ImageNames.characterStatusPortrait(
                ui.ctx.getCharacterNumber(i), ui.ctx.isAlive(i));
    }

    private void appendVotePanel(StringBuilder piao, int i, java.util.List<String> cmps) {
        for (int u = 1; u < 4; ++u) {
            if (ui.ctx.getTop3SuspectedPlayer(i, u, ui.ctx.getGameDay()) != 0) {
                if (u == 1) piao.append(ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(i))).append("：");
                piao.append(ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(ui.ctx.getTop3SuspectedPlayer(i, u, ui.ctx.getGameDay()))));
                if (!cmps.isEmpty()) { piao.append(cmps.get(0)); cmps.remove(0); }
            }
        }
        piao.append("\n");
    }

    private void handleReVoteResult(boolean isReVote) {
        if (isReVote) {
            votePanel.againBtn.setVisible(true);
            votePanel.nextBtn.setVisible(false);
            currentRound++;
        } else {
            votePanel.nextBtn.setVisible(true);
            votePanel.againBtn.setVisible(false);
        }
    }

    private void handleReVoteResultWithLimit(boolean isReVote) {
        if (isReVote) {
            votePanel.againBtn.setVisible(true);
            votePanel.nextBtn.setVisible(false);
            if (currentRound < GameConstants.MAX_VOTE_ROUNDS) currentRound++;
        } else {
            votePanel.nextBtn.setVisible(true);
            votePanel.againBtn.setVisible(false);
        }
    }

    void renderSkillTargets(JPanel panel, int baseX, int spacing, int yTop, int yBottom) {
        int n = this.ui.ctx.getPlayerSum() + 1;
        for (int k = 2; k <= this.ui.ctx.getGameDay(); ++k) {
            for (int j = 1; j < n; ++j) {
                if (this.ui.skillTargetPeople[j][k] == 0) continue;
                int i1 = this.ui.skillTargetPeople[j][k];
                int zynum = this.ui.claimedRolenum[j][k];
                if (zynum == 3) continue;
                if (zynum == 1 && this.ui.ctx.getDeathDay(j) != 0 && this.ui.ctx.getDeathDay(j) < k) continue;
                if (zynum == 2 && this.ui.ctx.getDeathDay(j) != 0 && this.ui.ctx.getDeathDay(j) < k) continue;
                JLabel stLabel = new JLabel(this.ui.resources.getImage(this.ui.skillTargetNames[j][k]));
                stLabel.setBounds(
                        charGridX(i1, baseX + spacing, spacing) - stLabel.getIcon().getIconWidth() * zynum,
                        charGridY(i1, yTop + (this.ui.skillTargetOrder[j][k] - 1) * stLabel.getIcon().getIconHeight(),
                                yBottom + (this.ui.skillTargetOrder[j][k] - 1) * stLabel.getIcon().getIconHeight()),
                        stLabel.getIcon().getIconWidth(), stLabel.getIcon().getIconHeight());
                panel.add(stLabel);
                panel.setComponentZOrder(stLabel, 0);
            }
        }
    }

}
