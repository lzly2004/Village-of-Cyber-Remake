package com.vocr.ui;

import com.vocr.domain.Event;
import com.vocr.resources.UiImage;

// DialogueAfternoonHandler.java - 下午对话场景处理器（用于回避CO）
import javax.swing.*;
import java.awt.*;

public class DialogueAfternoonHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        Event event = ui.pollEvent();
        UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(
                ui, UiImage.DIALOGUE_AFTERNOON_BACKGROUND, event);
        UIHelpers.bindTypewriter(ds.dc().dialogText, ds.text(), ds.dc().nextBtn, () -> {
            if (!ui.hasPendingEvents()) {
                ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            }
            ui.run();
        });
        ds.dc().dialogPanel.setVisible(true);
        if (!ui.linkIcon.isEmpty()) {
            UIHelpers.renderLinkIconPair(ui, ds.charIcon());
        } else {
            UIHelpers.renderDialogueCharacter(ui, event, ds.charIcon());
        }
        DialogueBox.finalize(ui, ds.dc());
    }
}
