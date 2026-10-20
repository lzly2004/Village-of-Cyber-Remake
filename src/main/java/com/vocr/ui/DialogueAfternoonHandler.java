package com.vocr.ui;

import com.vocr.domain.Event;
import com.vocr.application.EventPresentationStage;
import com.vocr.application.PresentedEvent;
import com.vocr.resources.UiImage;

// DialogueAfternoonHandler.java - 下午对话场景处理器（用于回避CO）
import javax.swing.*;
import java.awt.*;

public class DialogueAfternoonHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        PresentedEvent next = ui.peekPresentedEvent();
        if (next == null || next.stage() != EventPresentationStage.AFTERNOON_DIALOGUE) {
            ui.currentScene = UI.Scene.GAME_SCENE_VOTE;
            ui.run();
            return;
        }
        PresentedEvent presented = ui.pollPresentedEvent();
        Event event = presented.event();
        UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(
                ui, UiImage.DIALOGUE_AFTERNOON_BACKGROUND, event);
        UIHelpers.bindTypewriter(ds.dc().dialogText, ds.text(), ds.dc().nextBtn, () -> {
            if (!ui.hasPendingEvents(EventPresentationStage.AFTERNOON_DIALOGUE)) {
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
