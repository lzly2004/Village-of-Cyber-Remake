package com.vocr.ui;

import com.vocr.resources.UiImage;

import javax.swing.*;

/**
 * 投票界面按钮面板 —— 从 GameSceneVoteHandler 提取。
 * 管理所有投票场景按钮的创建。
 */
class VoteButtonPanel {

    JButton voteBtn, recordBtn, pointBtn, avoidBtn, avoidBtn1, menuBtn;
    JButton greyBtn, freeBtn, doubtBtn, votehisBtn, coBtn, ppBtn;
    JButton returnBtn, nextBtn, againBtn, readyVoteBtn, askCoBtn;
    JButton reiBtn, kariBtn, uranaiBtn, kyouyuBtn, catBtn;
    JButton fixedVoteBtn, fixedUranaiBtn, protectBtn;
    JScrollPane scrollPane1;

    void create(UI ui, boolean avoidanceEnabled) {
        voteBtn = createVisibleButton(ui, 1060, 720 - 40 - 126, UiImage.VOTE_CONFIRM);
        recordBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 2 - 10, UiImage.VOTE_INFO);
        pointBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.DESIGNATION_MENU);
        avoidBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 4 - 30, UiImage.AVOID_DISABLE);
        avoidBtn1 = createHiddenButton(ui, 1060, 720 - 40 - 126 * 4 - 30, UiImage.AVOID_ENABLE);
        menuBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 5 - 40, UiImage.GAME_MENU);
        menuBtn.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));

        greyBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.VOTE_GREY);
        freeBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, UiImage.VOTE_FREE);
        doubtBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.SUSPICION_INFO);
        votehisBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, UiImage.VOTE_HISTORY);
        coBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.CLAIM_MENU);
        ppBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, UiImage.POWER_PLAY);
        returnBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, UiImage.ACTION_RETURN);
        nextBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, UiImage.NEXT_DAY);
        againBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, UiImage.VOTE_CONFIRM);
        readyVoteBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.DESIGNATED_VOTE);
        askCoBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 4 - 30, UiImage.ASK_CLAIM);
        reiBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.CLAIM_MEDIUM);
        kariBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, UiImage.CLAIM_HUNTER);
        uranaiBtn = createHiddenButton(
                ui, 1060 - 194 - 30, 720 - 40 - 126 * 3 - 20, UiImage.CLAIM_SEER);
        kyouyuBtn = createHiddenButton(
                ui, 1060 - 194 - 30, 720 - 40 - 126 * 2 - 10, UiImage.CLAIM_SHARED);
        catBtn = createHiddenButton(ui, 1060 - 194 - 30, 720 - 40 - 126, UiImage.CLAIM_CAT);
        fixedVoteBtn = createHiddenButton(
                ui, 1060, 720 - 40 - 126 * 3 - 20, UiImage.DESIGNATED_VOTE);
        fixedUranaiBtn = createHiddenButton(
                ui, 1060, 720 - 40 - 126 * 2 - 10, UiImage.DESIGNATED_DIVINATION);
        protectBtn = createHiddenButton(
                ui, 1060, 720 - 40 - 126, UiImage.DESIGNATED_GUARD);

        if (avoidanceEnabled) { avoidBtn.setVisible(true); avoidBtn1.setVisible(false); }
        else { avoidBtn1.setVisible(true); avoidBtn.setVisible(false); }

        scrollPane1 = new JScrollPane();
        ui.jPanel.add(scrollPane1);
    }

    private JButton createVisibleButton(UI ui, int x, int y, UiImage image) {
        JButton btn = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y, ui.resources.getImage(image));
        ui.jPanel.add(btn);
        return btn;
    }

    private JButton createHiddenButton(UI ui, int x, int y, UiImage image) {
        JButton btn = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y, ui.resources.getImage(image));
        btn.setVisible(false);
        ui.jPanel.add(btn);
        return btn;
    }
}
