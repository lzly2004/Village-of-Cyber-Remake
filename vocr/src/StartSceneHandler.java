// StartSceneHandler.java - 新建文件
import javax.swing.JButton;
import javax.swing.JLabel;

public class StartSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.count = 0;
        ui.name = "";
        ui.text = "";
        ui.isAvoid = true;
        ui.isCo = false;
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

        ui.getEvents().clear();
        DebugLogger.log("事件成功清空");

        ui.jPanel.removeAll();

        ui.resources.playBgm("start_menu.wav");

        JLabel titleLabel = LabelSimpleFactory.makeLabel(
                LabelConst.Black_Label, 600, 200, 554, 138,
                ui.resources.getImage("titleLogo.png")
        );
        ui.jPanel.add(titleLabel);

        int x = GameConstants.START_BTN_X;
        int y = GameConstants.START_BTN_Y;
        int width = GameConstants.START_BTN_W;
        int height = GameConstants.START_BTN_H;
        int x_div = GameConstants.START_BTN_X_GAP;
        int y_div = GameConstants.START_BTN_Y_GAP;

        JButton btnStart = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y, width, height,
                ui.resources.getImage("startButton.png")
        );
        UIHelpers.setButtonSceneTransitionNoSound(btnStart, ui, UI.Scene.GAME_SCENE_SELECT);
        ui.jPanel.add(btnStart);

        JButton btnContinue = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y, width, height,
                ui.resources.getImage("continueButton.png")
        );
        UIHelpers.setButtonListener(btnContinue, ui, () -> {});
        ui.jPanel.add(btnContinue);

        JButton btnSave = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y + y_div, width, height,
                ui.resources.getImage("replayButton.png")
        );
        UIHelpers.setButtonListener(btnSave, ui, () -> {
            ui.replayPage = 0;
            ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
        });
        ui.jPanel.add(btnSave);

        JButton btnRecord = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y + y_div, width, height,
                ui.resources.getImage("recordButton.png")
        );
        UIHelpers.setButtonListener(btnRecord, ui, () -> {
            ui.recordPage = 0;
            ui.transitionTo(UI.Scene.RECORD_SCENE);
        });
        ui.jPanel.add(btnRecord);

        JButton btnInfo = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y + y_div * 2, width, height,
                ui.resources.getImage("infoButton.png")
        );
        UIHelpers.setButtonListener(btnInfo, ui, () -> {
            ui.currentScene = UI.Scene.INFO_SCENE;
            ui.resources.playBgm("Info.wav");
            ui.run();
        });
        ui.jPanel.add(btnInfo);

        JButton btnCollections = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y + y_div * 2, width, height,
                ui.resources.getImage("collectionsButton.png")
        );
        UIHelpers.setButtonListener(btnCollections, ui, () -> {});
        ui.jPanel.add(btnCollections);

        JLabel background = LabelSimpleFactory.makeLabel(
                LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("title_base_resized.png")
        );
        ui.jPanel.add(background);

        ui.resizeComponents();
        ui.jPanel.revalidate();
        ui.jPanel.repaint();
        ui.getJFrame().setVisible(true);
    }
}