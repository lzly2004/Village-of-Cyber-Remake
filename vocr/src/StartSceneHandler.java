// StartSceneHandler.java - 新建文件
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StartSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        // 第一步：先只做最简单的背景和标题
        ui.count = 0; // 切歌计数
        ui.name = ""; // 歌名
        ui.text = ""; // 文本
        ui.isAvoid = true; // 是否回避
        ui.isCo = false; // 是否询问co
        ui.isVote[0] = false;
        ui.isZhan[0] = false;
        ui.isHu[0] = false;
        ui.voteRounds.clear();
        ui.voteMethods.clear();
        ui.greyCharas = new int[21][50];
        ui.isSelectedVoteTargetCharas = new int[21][50];
        ui.voteChosen.clear();
        ui.zhanChosen.clear();
        ui.huChosen.clear();

        // 清空事件队列
        ui.getEvents().clear();
        DebugLogger.log("事件成功清空");

        // 清空面板
        ui.jPanel.removeAll();

        // 播放背景音乐
        ui.resources.playBgm("start_menu.wav");

        // 添加标题
        JLabel titleLabel = LabelSimpleFactory.makeLabel(
                LabelConst.Black_Label, 600, 200, 554, 138,
                ui.resources.getImage("titleLogo.png")
        );
        ui.jPanel.add(titleLabel);

        // 按钮属性
        int x = GameConstants.START_BTN_X;
        int y = GameConstants.START_BTN_Y;
        int width = GameConstants.START_BTN_W;
        int height = GameConstants.START_BTN_H;
        int x_div = GameConstants.START_BTN_X_GAP;
        int y_div = GameConstants.START_BTN_Y_GAP;

        // 新游戏按钮
        JButton btnStart = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y, width, height,
                ui.resources.getImage("startButton.png")
        );
        btnStart.addActionListener(e -> ui.transitionTo(UI.Scene.GAME_SCENE_SELECT));
        ui.jPanel.add(btnStart);

        // 继续游戏按钮
        JButton btnContinue = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y, width, height,
                ui.resources.getImage("continueButton.png")
        );
        btnContinue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ui.resources.playSound("click.wav");
            }
        });
        ui.jPanel.add(btnContinue);

        // 选择存档按钮
        JButton btnSave = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y + y_div, width, height,
                ui.resources.getImage("replayButton.png")
        );
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ui.resources.playSound("click.wav");
            }
        });
        ui.jPanel.add(btnSave);

        // 数据统计按钮
        JButton btnRecord = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y + y_div, width, height,
                ui.resources.getImage("recordButton.png")
        );
        btnRecord.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ui.resources.playSound("click.wav");
            }
        });
        ui.jPanel.add(btnRecord);

        // 信息查看按钮
        JButton btnInfo = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y + y_div * 2, width, height,
                ui.resources.getImage("infoButton.png")
        );
        btnInfo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ui.resources.playSound("click.wav");
                ui.currentScene = UI.Scene.INFO_SCENE;
                ui.resources.playBgm("Info.wav");
                ui.run();
            }
        });
        ui.jPanel.add(btnInfo);

        // 角色收集按钮
        JButton btnCollections = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y + y_div * 2, width, height,
                ui.resources.getImage("collectionsButton.png")
        );
        btnCollections.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ui.resources.playSound("click.wav");
            }
        });
        ui.jPanel.add(btnCollections);

        // 背景图片（放在最后添加，确保在最底层）
        JLabel background = LabelSimpleFactory.makeLabel(
                LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("title_base_resized.png")
        );
        ui.jPanel.add(background);

        // 强制重置一次
        ui.resizeComponents();
        ui.jPanel.revalidate();
        ui.jPanel.repaint();
        ui.jFrame.setVisible(true);
    }
}