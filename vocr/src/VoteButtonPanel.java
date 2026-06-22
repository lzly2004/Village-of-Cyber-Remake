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
        voteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("goTohyo.png"));
        ui.jPanel.add(voteBtn);
        recordBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("check.png"));
        ui.jPanel.add(recordBtn);
        pointBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("shiji.png"));
        ui.jPanel.add(pointBtn);
        avoidBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("关闭回避.png"));
        ui.jPanel.add(avoidBtn);
        avoidBtn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("开启回避.png"));
        avoidBtn1.setVisible(false);
        ui.jPanel.add(avoidBtn1);
        menuBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 5 - 40, ui.resources.getImage("IntroTitle.png"));
        menuBtn.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));
        ui.jPanel.add(menuBtn);
        greyBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoGrey.png"));
        greyBtn.setVisible(false); ui.jPanel.add(greyBtn);
        freeBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("tohyoFree.png"));
        freeBtn.setVisible(false); ui.jPanel.add(freeBtn);
        doubtBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("checkUtagai.png"));
        doubtBtn.setVisible(false); ui.jPanel.add(doubtBtn);
        votehisBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("checkTohyo.png"));
        votehisBtn.setVisible(false); ui.jPanel.add(votehisBtn);
        coBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("doCO.png"));
        coBtn.setVisible(false); ui.jPanel.add(coBtn);
        ppBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("doShitei.png"));
        ppBtn.setVisible(false); ui.jPanel.add(ppBtn);
        returnBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("return.png"));
        returnBtn.setVisible(false); ui.jPanel.add(returnBtn);
        nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("nextDay.png"));
        nextBtn.setVisible(false); ui.jPanel.add(nextBtn);
        againBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("goTohyo.png"));
        againBtn.setVisible(false); ui.jPanel.add(againBtn);
        readyVoteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoShitei.png"));
        readyVoteBtn.setVisible(false); ui.jPanel.add(readyVoteBtn);
        askCoBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("询问CO.png"));
        askCoBtn.setVisible(false); ui.jPanel.add(askCoBtn);
        reiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("reiCO.png"));
        reiBtn.setVisible(false); ui.jPanel.add(reiBtn);
        kariBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("kariCO.png"));
        kariBtn.setVisible(false); ui.jPanel.add(kariBtn);
        uranaiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("uranaiCO.png"));
        uranaiBtn.setVisible(false); ui.jPanel.add(uranaiBtn);
        kyouyuBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("kyouyuCO.png"));
        kyouyuBtn.setVisible(false); ui.jPanel.add(kyouyuBtn);
        catBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126, ui.resources.getImage("catCO.png"));
        catBtn.setVisible(false); ui.jPanel.add(catBtn);
        fixedVoteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoShitei.png"));
        fixedVoteBtn.setVisible(false); ui.jPanel.add(fixedVoteBtn);
        fixedUranaiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("shiteiUranai.png"));
        fixedUranaiBtn.setVisible(false); ui.jPanel.add(fixedUranaiBtn);
        protectBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("shiteiGoei.png"));
        protectBtn.setVisible(false); ui.jPanel.add(protectBtn);

        if (ui.isAvoid) { avoidBtn.setVisible(true); avoidBtn1.setVisible(false); }
        else { avoidBtn1.setVisible(true); avoidBtn.setVisible(false); }

        scrollPane1 = new JScrollPane();
        if (!scrollPane1.isAncestorOf(ui.jPanel)) ui.jPanel.add(scrollPane1);
    }
}
