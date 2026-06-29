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

    void create(UI ui) {
        voteBtn = createVisibleButton(ui, 1060, 720 - 40 - 126, "goTohyo.png");
        recordBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 2 - 10, "check.png");
        pointBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "shiji.png");
        avoidBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 4 - 30, "关闭回避.png");
        avoidBtn1 = createHiddenButton(ui, 1060, 720 - 40 - 126 * 4 - 30, "开启回避.png");
        menuBtn = createVisibleButton(ui, 1060, 720 - 40 - 126 * 5 - 40, "IntroTitle.png");
        menuBtn.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));

        greyBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "tohyoGrey.png");
        freeBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, "tohyoFree.png");
        doubtBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "checkUtagai.png");
        votehisBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, "checkTohyo.png");
        coBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "doCO.png");
        ppBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, "doShitei.png");
        returnBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, "return.png");
        nextBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, "nextDay.png");
        againBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, "goTohyo.png");
        readyVoteBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "tohyoShitei.png");
        askCoBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 4 - 30, "询问CO.png");
        reiBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "reiCO.png");
        kariBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, "kariCO.png");
        uranaiBtn = createHiddenButton(ui, 1060 - 194 - 30, 720 - 40 - 126 * 3 - 20, "uranaiCO.png");
        kyouyuBtn = createHiddenButton(ui, 1060 - 194 - 30, 720 - 40 - 126 * 2 - 10, "kyouyuCO.png");
        catBtn = createHiddenButton(ui, 1060 - 194 - 30, 720 - 40 - 126, "catCO.png");
        fixedVoteBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 3 - 20, "tohyoShitei.png");
        fixedUranaiBtn = createHiddenButton(ui, 1060, 720 - 40 - 126 * 2 - 10, "shiteiUranai.png");
        protectBtn = createHiddenButton(ui, 1060, 720 - 40 - 126, "shiteiGoei.png");

        if (ui.isAvoid) { avoidBtn.setVisible(true); avoidBtn1.setVisible(false); }
        else { avoidBtn1.setVisible(true); avoidBtn.setVisible(false); }

        scrollPane1 = new JScrollPane();
        if (!scrollPane1.isAncestorOf(ui.jPanel)) ui.jPanel.add(scrollPane1);
    }

    private JButton createVisibleButton(UI ui, int x, int y, String imageName) {
        JButton btn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, ui.resources.getImage(imageName));
        ui.jPanel.add(btn);
        return btn;
    }

    private JButton createHiddenButton(UI ui, int x, int y, String imageName) {
        JButton btn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, x, y, ui.resources.getImage(imageName));
        btn.setVisible(false);
        ui.jPanel.add(btn);
        return btn;
    }
}
