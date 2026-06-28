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
    private GreyListCalculator.Result greyResult;
    // === 保留在Handler中的UI组件 ===
    private JPanel hisPanel, infoPanel, infoZhanPanel, infoHuPanel, infoCoPanel;
    private JLabel levellb, background;
    // === 共享状态（实例字段）===
    private List<Integer> askList;
    private int[] round;
    private boolean[] isVotehis, isCo;
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

        // ========== 阶段5：按钮创建 ==========
        createAllButtons();

        // ========== 阶段6：灰名单计算 ==========
        computeGreyList();

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
        background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("komorebi002yuu.png"));
    }

    // ===================================================================
    // 阶段1提取：BGM按钮
    // ===================================================================
    private void createMusicButton()
    {
        String[] bgmSongs;
        String[] bgmTexts;
        if ((ui.ctx.getAliveCounter() - 1) / 2 == 1) {
            bgmSongs = new String[]{"真甜呢丨先来一斤吧 - ラストボス02.wav", "西江紫堂 - 黒き明日、清き闇.wav", "西江紫堂 - BLOOD.wav", "西江紫堂 - 灯り無き眼光.wav"};
            bgmTexts = new String[4];
        } else {
            bgmSongs = new String[]{"Death Impact.wav", "永久色五色.wav", "Emotionally Unstable.wav", "FELT LOVE.wav", "Just Complex.wav", "Peace of lost puzzle.wav", "TEEK TEEK TEEK.wav", "ダンジョン09.wav", "ライアービジネス.wav", "五月雨Vivaride.wav", "村08.wav", "真甜呢丨先来一斤吧 - ダンジョン14.wav", "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.wav"};
            bgmTexts = new String[]{"Death Impact.txt", "永久色五色.txt", "Emotionally Unstable.txt", "FELT LOVE.txt", "Just Complex.txt", "Peace of lost puzzle.txt", "TEEK TEEK TEEK.txt", "ダンジョン09.txt", "ライアービジネス.txt", "五月雨Vivaride.txt", "村08.txt", "真甜呢丨先来一斤吧 - ダンジョン14.txt", "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.txt"};
        }
        ImageIcon musicBtnIcon = ui.resources.getImage("musicBtn.png");
        JButton btnMusic = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 15, 35,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT, musicBtnIcon);
        btnMusic.addActionListener(e -> {
            int cur = ui.count % bgmSongs.length;
            ui.name = bgmSongs[cur];
            if (bgmTexts[cur] != null) ui.text = bgmTexts[cur];
            ui.count++;
            ui.resources.playBgm(ui.name);
        });
        ui.jPanel.add(btnMusic);
    }

    // ===================================================================
    // 阶段1提取：玩家状态渲染
    // ===================================================================
    private void renderPlayerStatuses() {
        PlayerStatusRenderer.render(ui, this);
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
        votePanel.create(ui);
    }

    // ===================================================================
    // 阶段6提取：灰名单计算
    // ===================================================================
    private void computeGreyList()
    {
        greyResult = GreyListCalculator.compute(ui);
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
            ui.resources.playSound("click.wav");
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
        isVotehis = new boolean[]{false};
        votePanel.votehisBtn.addActionListener(e -> VoteHistoryBrowser.render(ui, votePanel, voteInfo, hisPanel, levellb, isVotehis, this));

        // --- 投票ボタン ---
        votePanel.voteBtn.addActionListener(e -> handleVoteButton());

        round = new int[]{1};

        // --- 灰投票 ---
        votePanel.greyBtn.addActionListener(e -> handleGreyVoteButton());

        // --- 履歴チェック ---
        votePanel.recordBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
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
            ui.resources.playSound("click.wav");
            votePanel.avoidBtn1.setVisible(true); votePanel.avoidBtn.setVisible(false);
            ui.isAvoid = false;
            ui.resizeComponents();
        });
        votePanel.avoidBtn1.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            votePanel.avoidBtn1.setVisible(false); votePanel.avoidBtn.setVisible(true);
            ui.isAvoid = true;
            ui.resizeComponents();
        });

        // --- 指定/CO ---
        votePanel.pointBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            votePanel.voteBtn.setVisible(false); votePanel.pointBtn.setVisible(false); votePanel.avoidBtn.setVisible(false);
            votePanel.recordBtn.setVisible(false); votePanel.avoidBtn1.setVisible(false);
            votePanel.coBtn.setVisible(true); votePanel.ppBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            ui.resizeComponents();
        });

        votePanel.coBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            votePanel.coBtn.setVisible(false); votePanel.ppBtn.setVisible(false);
            votePanel.reiBtn.setVisible(true); votePanel.kariBtn.setVisible(true); votePanel.askCoBtn.setVisible(true);
            votePanel.uranaiBtn.setVisible(true); votePanel.kyouyuBtn.setVisible(true); votePanel.catBtn.setVisible(true);
            ui.resizeComponents();
        });

        votePanel.ppBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
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
        isCo = new boolean[]{false};

        // --- 戻るボタン ---
        votePanel.returnBtn.addActionListener(e -> handleReturnButton());

        // --- 自由投票 ---
        votePanel.freeBtn.addActionListener(e -> handleFreeVoteButton());

        // --- 再投票 ---
        votePanel.againBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            boolean[] isReVote = {false};
            if (ui.ctx.getEndResult() == 0) {
                VoteResultRenderer.renderPiao(ui, String.format(GameStrings.VOTE_TITLE_REDO, ui.ctx.getGameDay() - 1), round[0], isReVote, this);
            } else {
                VoteResultRenderer.renderPiao(ui, String.format(GameStrings.VOTE_TITLE_REDO, ui.ctx.getGameDay()), round[0], isReVote, this);
            }
            if (isReVote[0]) { votePanel.againBtn.setVisible(true); votePanel.nextBtn.setVisible(false); if (round[0] < 3) round[0]++; }
            else { votePanel.nextBtn.setVisible(true); votePanel.againBtn.setVisible(false); }
        });

        // --- 次へ ---
        votePanel.nextBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.voteRounds.add(round[0]);
            ui.currentScene = UI.Scene.DIALOGUE_CHUXING;
            ui.run();
        });

        // --- 指定投票 ---
        votePanel.readyVoteBtn.addActionListener(e -> handleReadyVoteButton());

        // --- CO询问 ---
        votePanel.askCoBtn.addActionListener(e -> CoSelectionPanel.render(ui, votePanel, voteInfo, infoCoPanel, askList, isCo, this));


        // --- CO按钮 ---
        votePanel.reiBtn.addActionListener(e -> handleRoleCo(Role.ling, GameStrings.MSG_NO_MEDIUM));
        votePanel.kariBtn.addActionListener(e -> handleRoleCo(Role.lie, GameStrings.MSG_NO_HUNTER));
        votePanel.uranaiBtn.addActionListener(e -> handleRoleCo(Role.zhan, GameStrings.MSG_NO_SEER));
        votePanel.kyouyuBtn.addActionListener(e -> handleRoleCo(Role.gong, GameStrings.MSG_NO_SHARED));
        votePanel.catBtn.addActionListener(e -> handleRoleCo(Role.mao, GameStrings.MSG_NO_CAT));

        // --- 指定パネル ---
        votePanel.fixedVoteBtn.addActionListener(e -> CharacterSelectionPanel.render(ui, votePanel, voteInfo, infoPanel, ui.voteChosen, ui.isVote, SelectionType.VOTE, this));
        votePanel.fixedUranaiBtn.addActionListener(e -> CharacterSelectionPanel.render(ui, votePanel, voteInfo, infoZhanPanel, ui.zhanChosen, ui.isZhan, SelectionType.DIVINATION, this));
        votePanel.protectBtn.addActionListener(e -> CharacterSelectionPanel.render(ui, votePanel, voteInfo, infoHuPanel, ui.huChosen, ui.isHu, SelectionType.GUARD, this));
    }

    // ===================================================================
    // 阶段8提取：按钮事件处理器
    // ===================================================================

    private void handleVoteButton()
    {
        voteInfo.infoText.setVisible(false);
        ui.resources.playSound("click.wav");
        votePanel.voteBtn.setVisible(false); votePanel.pointBtn.setVisible(false); votePanel.avoidBtn.setVisible(false);
        votePanel.recordBtn.setVisible(false); votePanel.avoidBtn1.setVisible(false);
        if (ui.isVote[0]) {
            votePanel.readyVoteBtn.setVisible(true); votePanel.greyBtn.setVisible(false); votePanel.freeBtn.setVisible(false);
        } else {
            if (!greyResult.cxList.isEmpty()) votePanel.greyBtn.setVisible(true);
            votePanel.freeBtn.setVisible(true); votePanel.readyVoteBtn.setVisible(false);
        }
        if (greyResult.cxList.isEmpty()) votePanel.greyBtn.setVisible(false);
        votePanel.returnBtn.setVisible(true); votePanel.scrollPane1.setVisible(true);

        JTextArea isSelectedVoteTargetText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, GameConstants.FONT_SIZE_VOTE, "");
        StringBuilder isSelectedVoteTargetResult = new StringBuilder("-指定内容-\n");
        int playerSum = ui.ctx.getPlayerSum() + 1;
        if (ui.isVote[0]) {
            isSelectedVoteTargetResult.append(GameStrings.SPECIFY_VOTE);
            for (int i = 1; i < playerSum; ++i) {
                if (ui.ctx.isSelectedVoteTarget(i, ui.ctx.getGameDay()))
                    isSelectedVoteTargetResult.append(ui.getJobText(i) + " ");
            }
            isSelectedVoteTargetResult.append("\n");
        }
        if (ui.isZhan[0]) {
            isSelectedVoteTargetResult.append(GameStrings.SPECIFY_DIVINATION);
            for (int i = 1; i < playerSum; ++i) {
                if (ui.ctx.getClaimedRole(i) == 1 && ui.ctx.isAlive(i)) {
                    isSelectedVoteTargetResult.append(ui.getJobText(i) + "→");
                    for (int j = 1; j < playerSum; ++j) {
                        if (ui.ctx.isClaimedRoleScheduled(i, j, ui.ctx.getGameDay()))
                            isSelectedVoteTargetResult.append(ui.getJobText(j) + ",");
                    }
                    isSelectedVoteTargetResult.append("\n");
                }
            }
            int cc = 0;
            for (int j = 1; j < playerSum; ++j) {
                if (ui.ctx.getHiddenSeerScheduledSkillTargets()[j][ui.ctx.getGameDay()]) {
                    if (cc++ == 0) isSelectedVoteTargetResult.append(GameStrings.HIDDEN_ARROW);
                    isSelectedVoteTargetResult.append(ui.getJobText(j) + ",");
                }
            }
            isSelectedVoteTargetResult.append("\n");
        }
        if (DebugLogger.getInstance().isEnabled()) {
            DebugLogger.log("是否护卫" + ui.isHu[0]);
            for (int i = 1; i < playerSum; ++i) {
                for (int j = 1; j < playerSum; ++j) {
                    if (ui.ctx.isClaimedRoleScheduled(i, j, ui.ctx.getGameDay()))
                        DebugLogger.log(ui.getJobText(i) + "护卫了"
                                + ui.ctx.isClaimedRoleScheduled(i, j, ui.ctx.getGameDay()));
                }
            }
        }
        if (ui.isHu[0]) {
            isSelectedVoteTargetResult.append(GameStrings.SPECIFY_GUARD);
            for (int i = 1; i < playerSum; ++i) {
                if (ui.ctx.getClaimedRole(i) == 3 && ui.ctx.isAlive(i)) {
                    isSelectedVoteTargetResult.append(ui.getJobText(i) + "→");
                    for (int j = 1; j < playerSum; ++j) {
                        if (ui.ctx.isClaimedRoleScheduled(i, j, ui.ctx.getGameDay()))
                            isSelectedVoteTargetResult.append(ui.getJobText(j) + ",");
                    }
                    isSelectedVoteTargetResult.append("\n");
                }
            }
            int vv = 0;
            for (int j = 1; j < playerSum; ++j) {
                if (ui.ctx.getHiddenHunterScheduledSkillTargets()[j][ui.ctx.getGameDay()]) {
                    if (vv++ == 0) isSelectedVoteTargetResult.append(GameStrings.HIDDEN_ARROW);
                    isSelectedVoteTargetResult.append(ui.getJobText(j) + ",");
                }
            }
            isSelectedVoteTargetResult.append("\n");
        }
        isSelectedVoteTargetText.setText(isSelectedVoteTargetResult.toString());
        votePanel.scrollPane1.getViewport().setView(isSelectedVoteTargetText);
        votePanel.scrollPane1.setBorder(BorderFactory.createEmptyBorder());
        votePanel.scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        votePanel.scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        votePanel.scrollPane1.setOpaque(false);
        votePanel.scrollPane1.getViewport().setOpaque(false);
        votePanel.scrollPane1.setBounds(40, 228, 200 + voteInfo.boardIcon.getIconWidth() - 80,
                50 + voteInfo.boardIcon.getIconHeight() - 60);
        isSelectedVoteTargetText.setVisible(true);
        ui.jPanel.setComponentZOrder(votePanel.scrollPane1, 0);
        ui.resizeComponents();
    }

    private void handleGreyVoteButton()
    {
        votePanel.freeBtn.setVisible(false); votePanel.greyBtn.setVisible(false); votePanel.returnBtn.setVisible(false);
        votePanel.scrollPane1.setVisible(false);
        StringBuilder greyText = new StringBuilder();
        for (int i = 0; i < greyResult.cxList.size(); ++i)
            greyText.append(ui.getJobText(greyResult.cxList.get(i)));
        List<Integer> beiZhan = new ArrayList<>();
        for (int j = 1; j < ui.ctx.getGameDay(); ++j) {
            for (int k = 1; k <= ui.ctx.getPlayerSum(); ++k) {
                if (ui.ctx.getClaimedRole(k) == 1) {
                    int num = ui.ctx.getSkillTarget(k, j);
                    if (num > ui.ctx.getPlayerSum()) num -= ui.ctx.getPlayerSum();
                    if (!beiZhan.contains(num)) { beiZhan.add(num); DebugLogger.log(num); }
                }
            }
        }
        boolean[] isReVote = {false};
        if (!greyResult.cxList.isEmpty()) DebugLogger.log("cxList不为空，且具体为" + greyResult.cxList);
        if (ui.mainLogic.shokei(1, greyResult.cxList, ui.isAvoid)) {
            int trueDay = (ui.ctx.getEndResult() == 0) ? ui.ctx.getGameDay() - 1 : ui.ctx.getGameDay();
            for (int f = 0; f < greyResult.cxList.size(); ++f)
                ui.greyCharas[f][trueDay] = greyResult.cxList.get(f);
            ui.voteMethods.add(1); greyResult.cxList.clear();
            VoteResultRenderer.renderPiao(ui, String.format(GameStrings.VOTE_TITLE_GREY, trueDay) + greyText + "\n", round[0], isReVote, this);
            if (isReVote[0]) { votePanel.againBtn.setVisible(true); votePanel.nextBtn.setVisible(false); round[0]++; }
            else { votePanel.nextBtn.setVisible(true); votePanel.againBtn.setVisible(false); }
        } else {
            greyResult.cxList.clear();
            votePanel.freeBtn.setVisible(true); votePanel.greyBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            votePanel.scrollPane1.setVisible(true);
            resetToAfternoon.run();
        }
    }

    private void handleReturnButton()
    {
        if (isVotehis[0]) { ui.run(); }
        ui.resources.playSound("click.wav");
        votePanel.voteBtn.setVisible(true); votePanel.pointBtn.setVisible(true); voteInfo.infoText.setVisible(true);
        ui.piaoText.setVisible(false); ui.piaoText1.setVisible(false);
        if (ui.isAvoid) { votePanel.avoidBtn.setVisible(true); votePanel.avoidBtn1.setVisible(false); }
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
        if (!askList.isEmpty() && isCo[0]) {
            isCo[0] = false;
            DebugLogger.log(askList);
            ui.mainLogic.askCo(askList);
            if (!ui.getEvents().isEmpty()) {
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
        ui.resources.playSound("click.wav"); votePanel.scrollPane1.setVisible(false);
        List<Integer> chuxingList = new ArrayList<>();
        for (int i = 1; i <= ui.ctx.getPlayerSum(); ++i)
            if (ui.ctx.isAlive(i)) chuxingList.add(i);
        DebugLogger.log("自由投票" + chuxingList);
        boolean[] isReVote = {false};
        if (ui.mainLogic.shokei(0, chuxingList, ui.isAvoid)) {
            ui.voteMethods.add(0); chuxingList.clear();
            if (ui.ctx.getEndResult() == 0) {
                VoteResultRenderer.renderPiao(ui, String.format(GameStrings.VOTE_TITLE_FREE, ui.ctx.getGameDay() - 1), round[0], isReVote, this);
            } else {
                VoteResultRenderer.renderPiao(ui, String.format(GameStrings.VOTE_TITLE_FREE, ui.ctx.getGameDay()), round[0], isReVote, this);
            }
            if (isReVote[0]) { votePanel.againBtn.setVisible(true); votePanel.nextBtn.setVisible(false); round[0]++; }
            else { votePanel.nextBtn.setVisible(true); votePanel.againBtn.setVisible(false); }
        } else {
            chuxingList.clear();
            votePanel.freeBtn.setVisible(true); votePanel.greyBtn.setVisible(true); votePanel.returnBtn.setVisible(true);
            votePanel.scrollPane1.setVisible(true);
            resetToAfternoon.run();
        }
    }

    private void handleReadyVoteButton()
    {
        votePanel.readyVoteBtn.setVisible(false); votePanel.returnBtn.setVisible(false); votePanel.scrollPane1.setVisible(false);
        List<Integer> chuxingList = new ArrayList<>();
        int p = 0;
        for (int i = 1; i <= ui.ctx.getPlayerSum(); ++i) {
            if (ui.ctx.isSelectedVoteTarget(i, ui.ctx.getGameDay()) && ui.ctx.isAlive(i)) {
                chuxingList.add(i);
                ui.isSelectedVoteTargetCharas[p++][ui.ctx.getGameDay()] = i;
                DebugLogger.log("指定了" + i);
            }
        }
        DebugLogger.log("指定投票" + chuxingList);
        boolean[] isReVote = {false};
        if (ui.mainLogic.shokei(2, chuxingList, ui.isAvoid)) {
            ui.voteMethods.add(2);
            int trueDay = (ui.ctx.getEndResult() == 0) ? ui.ctx.getGameDay() - 1 : ui.ctx.getGameDay();
            StringBuilder isSelectedVoteTargetText = new StringBuilder();
            for (int i = 0; i < chuxingList.size(); ++i)
                isSelectedVoteTargetText.append(ui.getJobText(chuxingList.get(i))).append(",");
            DebugLogger.log(isSelectedVoteTargetText);
            VoteResultRenderer.renderPiao(ui, String.format(GameStrings.VOTE_TITLE_DESIGN, trueDay, isSelectedVoteTargetText.toString()), round[0], isReVote, this);
            chuxingList.clear();
            if (isReVote[0]) { votePanel.againBtn.setVisible(true); votePanel.nextBtn.setVisible(false); round[0]++; }
            else { votePanel.nextBtn.setVisible(true); votePanel.againBtn.setVisible(false); }
        } else {
            chuxingList.clear();
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
        TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, GameConstants.FONT_SIZE_VOTE, text);
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

    private void handleRoleCo(Role role, String noRoleMsg) {
        ui.resources.playSound("click.wav");
        ui.mainLogic.askCo(role);
        if (ui.getEvents().isEmpty()) {
            createTishi(noRoleMsg);
        } else {
            ui.currentScene = UI.Scene.DIALOGUE_DAY;
            ui.run();
        }
    }





    void createDoubt() {
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
                if (leftCnt >= 10) {
                    for (int u = 1; u < 4; ++u) {
                        if (this.ui.ctx.getTop3SuspectedPlayer(i, u, this.ui.ctx.getGameDay()) != 0) {
                            if (u == 1) rightPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.ctx.getCharacterNumber(i))).append("：");
                            rightPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.ctx.getCharacterNumber(this.ui.ctx.getTop3SuspectedPlayer(i, u, this.ui.ctx.getGameDay()))));
                            if (!cmps.isEmpty()) { rightPiao.append(cmps.getFirst()); cmps.removeFirst(); }
                        }
                    }
                    rightPiao.append("\n");
                } else {
                    for (int u = 1; u < 4; ++u) {
                        if (this.ui.ctx.getTop3SuspectedPlayer(i, u, this.ui.ctx.getGameDay()) != 0) {
                            if (u == 1) leftPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.ctx.getCharacterNumber(i))).append("：");
                            leftPiao.append(this.ui.uiComponentFactory.getJobText(this.ui.ctx.getCharacterNumber(this.ui.ctx.getTop3SuspectedPlayer(i, u, this.ui.ctx.getGameDay()))));
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




    private void appendSkillResults(StringBuilder sb, int i, int startDay)
    {
        sb.append(ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(i))).append(" : ");
        for (int j = startDay; j < ui.ctx.getGameDay(); ++j) {
            if (ui.ctx.getDeathDay(i) != 0 && j >= ui.ctx.getDeathDay(i)) break;
            if (ui.ctx.getSkillTarget(i, j) > ui.ctx.getPlayerSum()) {
                sb.append(ui.uiComponentFactory.getJobText(
                        ui.ctx.getCharacterNumber(ui.ctx.getSkillTarget(i, j) - ui.ctx.getPlayerSum()))).append("●");
                sb.append("→");
            } else if (ui.ctx.getSkillTarget(i, j) > 0) {
                sb.append(ui.uiComponentFactory.getJobText(
                        ui.ctx.getCharacterNumber(ui.ctx.getSkillTarget(i, j)))).append("○");
                sb.append("→");
            }
        }
        if (ui.ctx.isNonHumanMarked(i)) {
            sb.append(GameStrings.MARKER_EXPOSED); sb.append("→");
        }
        sb.setLength(sb.length() - 1); sb.append("\n");
    }

    int charGridX(int i, int baseX, int spacing) {
        int half = (this.ui.ctx.getPlayerSum() + 1) / 2;
        return baseX + spacing * (i <= half ? i : i - this.ui.ctx.getPlayerSum() / 2);
    }

    int charGridY(int i, int yTop, int yBottom) {
        return i <= (this.ui.ctx.getPlayerSum() + 1) / 2 ? yTop : yBottom;
    }

    String charImageName(int i) {
        StringBuilder sb = new StringBuilder();
        if (this.ui.ctx.getCharacterNumber(i) <= 9) sb.append("0");
        sb.append(this.ui.ctx.getCharacterNumber(i));
        if (!this.ui.ctx.isAlive(i)) sb.append("g");
        sb.append("s.png");
        return sb.toString();
    }

    String claimedRoleIconName(int i) {
        StringBuilder sb = new StringBuilder("yaku");
        int cr = this.ui.ctx.getClaimedRole(i);
        if (cr <= 3) sb.append(cr).append("_").append(this.ui.ctx.getClaimedRoleOrder(i));
        else sb.append(cr);
        sb.append(".png");
        return sb.toString();
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

    void updateFlagFromVoteTargets(boolean[] flag) {
        for (int y = 1; y <= this.ui.ctx.getPlayerSum(); ++y) {
            if (this.ui.ctx.isSelectedVoteTarget(y, this.ui.ctx.getGameDay())) { flag[0] = true; return; }
        }
        flag[0] = false;
    }

    void updateFlagFromScheduledTargets(java.util.List<Integer> trueNums, boolean[] flag, boolean isSeer) {
        for (int b = 0; b < trueNums.size(); ++b) {
            for (int j = 1; j <= this.ui.ctx.getPlayerSum(); ++j) {
                if (this.ui.ctx.isClaimedRoleScheduled(trueNums.get(b), j, this.ui.ctx.getGameDay())) {
                    flag[0] = true; return;
                }
            }
        }
        for (int y = 1; y <= this.ui.ctx.getPlayerSum(); ++y) {
            if (isSeer && this.ui.ctx.getHiddenSeerScheduledSkillTargets()[y][this.ui.ctx.getGameDay()]) { flag[0] = true; return; }
            if (!isSeer && this.ui.ctx.getHiddenHunterScheduledSkillTargets()[y][this.ui.ctx.getGameDay()]) { flag[0] = true; return; }
        }
        flag[0] = false;
    }
}