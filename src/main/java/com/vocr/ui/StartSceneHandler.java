package com.vocr.ui;

import com.vocr.resources.BgmTrack;
import com.vocr.resources.UiImage;
import com.vocr.logging.DebugLogger;

// StartSceneHandler.java - 新建文件
import javax.swing.JButton;
import javax.swing.JLabel;

public class StartSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.count = 0;
        ui.isAvoid = true;
        ui.isCo = false;
        ui.selectedVoteTargets.clear();
        ui.selectedDivinationTargets.clear();
        ui.selectedGuardTargets.clear();

        ui.clearPendingEvents();
        DebugLogger.log("事件成功清空");

        ui.jPanel.removeAll();

        ui.resources.playBgm(BgmTrack.START_MENU);

        JLabel titleLabel = LabelSimpleFactory.makeLabel(
                LabelConst.Black_Label, 600, 200, 554, 138,
                ui.resources.getImage(UiImage.TITLE_LOGO)
        );
        ui.jPanel.add(titleLabel);

        int x = UIConstants.START_BTN_X;
        int y = UIConstants.START_BTN_Y;
        int width = UIConstants.START_BTN_W;
        int height = UIConstants.START_BTN_H;
        int x_div = UIConstants.START_BTN_X_GAP;
        int y_div = UIConstants.START_BTN_Y_GAP;

        JButton btnStart = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y, width, height,
                ui.resources.getImage(UiImage.START_BUTTON)
        );
        UIHelpers.setButtonSceneTransitionNoSound(btnStart, ui, UI.Scene.GAME_SCENE_SELECT);
        ui.jPanel.add(btnStart);

        JButton btnContinue = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y, width, height,
                ui.resources.getImage(UiImage.CONTINUE_BUTTON)
        );
        UIHelpers.setButtonListener(btnContinue, ui, () -> {});
        ui.jPanel.add(btnContinue);

        JButton btnSave = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y + y_div, width, height,
                ui.resources.getImage(UiImage.REPLAY_BUTTON)
        );
        UIHelpers.setButtonListener(btnSave, ui, () -> {
            ui.replayPage = 0;
            ui.transitionTo(UI.Scene.REPLAY_BROWSER_SCENE);
        });
        ui.jPanel.add(btnSave);

        JButton btnRecord = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y + y_div, width, height,
                ui.resources.getImage(UiImage.RECORD_BUTTON)
        );
        UIHelpers.setButtonListener(btnRecord, ui, () -> {
            ui.recordPage = 0;
            ui.transitionTo(UI.Scene.RECORD_SCENE);
        });
        ui.jPanel.add(btnRecord);

        JButton btnInfo = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x, y + y_div * 2, width, height,
                ui.resources.getImage(UiImage.INFO_BUTTON)
        );
        UIHelpers.setButtonListener(btnInfo, ui, () -> {
            ui.currentScene = UI.Scene.INFO_SCENE;
            ui.resources.playBgm(BgmTrack.INFO);
            ui.run();
        });
        ui.jPanel.add(btnInfo);

        JButton btnCollections = ButtonSimpleFactory.makeButton(
                ButtonConst.Simple_Button, x + x_div, y + y_div * 2, width, height,
                ui.resources.getImage(UiImage.COLLECTIONS_BUTTON)
        );
        UIHelpers.setButtonListener(btnCollections, ui, () -> {});
        ui.jPanel.add(btnCollections);

        UIHelpers.addBackground(ui, UiImage.TITLE_BACKGROUND);

        UIHelpers.finishRender(ui);
    }
}
