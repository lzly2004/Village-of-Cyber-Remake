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
        voteBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("goTohyo.png"));
        ui.jPanel.add(voteBtn);
        recordBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("check.png"));
        ui.jPanel.add(recordBtn);
        pointBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("shiji.png"));
        ui.jPanel.add(pointBtn);
        avoidBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("关闭回避.png"));
        ui.jPanel.add(avoidBtn);
        avoidBtn1 = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("开启回避.png"));
        avoidBtn1.setVisible(false);
        ui.jPanel.add(avoidBtn1);
        menuBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 5 - 40, ui.resources.getImage("IntroTitle.png"));
        menuBtn.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));
        ui.jPanel.add(menuBtn);
        greyBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoGrey.png"));
        greyBtn.setVisible(false); ui.jPanel.add(greyBtn);
        freeBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("tohyoFree.png"));
        freeBtn.setVisible(false); ui.jPanel.add(freeBtn);
        doubtBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("checkUtagai.png"));
        doubtBtn.setVisible(false); ui.jPanel.add(doubtBtn);
        votehisBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("checkTohyo.png"));
        votehisBtn.setVisible(false); ui.jPanel.add(votehisBtn);
        coBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("doCO.png"));
        coBtn.setVisible(false); ui.jPanel.add(coBtn);
        ppBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("doShitei.png"));
        ppBtn.setVisible(false); ui.jPanel.add(ppBtn);
        returnBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("return.png"));
        returnBtn.setVisible(false); ui.jPanel.add(returnBtn);
        nextBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("nextDay.png"));
        nextBtn.setVisible(false); ui.jPanel.add(nextBtn);
        againBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("goTohyo.png"));
        againBtn.setVisible(false); ui.jPanel.add(againBtn);
        readyVoteBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoShitei.png"));
        readyVoteBtn.setVisible(false); ui.jPanel.add(readyVoteBtn);
        askCoBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 4 - 30, ui.resources.getImage("询问CO.png"));
        askCoBtn.setVisible(false); ui.jPanel.add(askCoBtn);
        reiBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("reiCO.png"));
        reiBtn.setVisible(false); ui.jPanel.add(reiBtn);
        kariBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("kariCO.png"));
        kariBtn.setVisible(false); ui.jPanel.add(kariBtn);
        uranaiBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("uranaiCO.png"));
        uranaiBtn.setVisible(false); ui.jPanel.add(uranaiBtn);
        kyouyuBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("kyouyuCO.png"));
        kyouyuBtn.setVisible(false); ui.jPanel.add(kyouyuBtn);
        catBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060 - 194 - 30,
                720 - 40 - 126, ui.resources.getImage("catCO.png"));
        catBtn.setVisible(false); ui.jPanel.add(catBtn);
        fixedVoteBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 3 - 20, ui.resources.getImage("tohyoShitei.png"));
        fixedVoteBtn.setVisible(false); ui.jPanel.add(fixedVoteBtn);
        fixedUranaiBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126 * 2 - 10, ui.resources.getImage("shiteiUranai.png"));
        fixedUranaiBtn.setVisible(false); ui.jPanel.add(fixedUranaiBtn);
        protectBtn = UIFactory.makeButton(ButtonConst.Simple_Button, 1060,
                720 - 40 - 126, ui.resources.getImage("shiteiGoei.png"));
        protectBtn.setVisible(false); ui.jPanel.add(protectBtn);

        if (ui.isAvoid) { avoidBtn.setVisible(true); avoidBtn1.setVisible(false); }
        else { avoidBtn1.setVisible(true); avoidBtn.setVisible(false); }

        scrollPane1 = new JScrollPane();
        if (!scrollPane1.isAncestorOf(ui.jPanel)) ui.jPanel.add(scrollPane1);
    }
}
