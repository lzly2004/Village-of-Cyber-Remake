package com.vocr.ui;

import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;
import com.vocr.application.EventPresentationStage;
import com.vocr.application.PresentedEvent;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;

import javax.swing.*;
import java.awt.*;

public class DialogueChuxingHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.resources.stopBgm();
        PresentedEvent next = ui.peekPresentedEvent();
        if (next == null || next.stage() != EventPresentationStage.EXECUTION) {
            ui.currentScene = UI.Scene.GAME_SCENE_NIGHT;
            ui.run();
            return;
        }
        PresentedEvent presented = ui.pollPresentedEvent();
        Event event = presented.event();
        ui.jPanel.removeAll();
        UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(
                ui, UiImage.DIALOGUE_AFTERNOON_BACKGROUND, event);
        ui.resizeComponents();
        JPanel dialogPanel = ds.dc().dialogPanel;
        JTextArea dialogText = ds.dc().dialogText;
        JButton nextBtn = ds.dc().nextBtn;
        ImageIcon[] CharIcon = ds.charIcon();
        JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                (UIConstants.WINDOW_WIDTH - CharIcon[0].getIconWidth()) / 2,
                UIConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
        ui.diaPanel.add(Chara);
        Timer typeTimer = UIHelpers.bindTypewriter(dialogText, ds.text(), nextBtn, () -> {
            dialogPanel.setVisible(false);
            nextBtn.setVisible(false);
            Timer timer = new Timer(UIConstants.TRANSITION_MEDIUM_MS, e1 -> {
                dialogText.setText("");
                nextBtn.setVisible(true);
                String resultText;
                if (event.eventname() == EventName.cxs) {
                    resultText = String.format(GameStrings.EXECUTED_FORMAT,
                            ui.uiComponentFactory.getCharacterDisplayName(event.ch1()));
                } else if (event.eventname() == EventName.hzsw) {
                    resultText = String.format(GameStrings.FOLLOW_DEATH_FORMAT,
                            ui.uiComponentFactory.getCharacterDisplayName(event.ch1()));
                } else if (event.eventname() == EventName.mzsw) {
                    resultText = String.format(GameStrings.CAT_CURSE_FORMAT,
                            ui.uiComponentFactory.getCharacterDisplayName(event.ch1()));
                } else {
                    resultText = "";
                }
                UIHelpers.bindTypewriter(dialogText, resultText, nextBtn, () -> {
                    if (!ui.hasPendingEvents(EventPresentationStage.EXECUTION)) {
                        ui.currentScene = UI.Scene.GAME_SCENE_NIGHT;
                    }
                    ui.run();
                });
                dialogPanel.setVisible(true);
                ((Timer) e1.getSource()).stop();
            });
            timer.start();
            ui.resources.playSound(SoundEffect.DAY_EXECUTION);
            Chara.setVisible(false);
            if (CharIcon.length >= 2) {
                JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        (UIConstants.WINDOW_WIDTH - CharIcon[1].getIconWidth()) / 2,
                        UIConstants.WINDOW_HEIGHT - CharIcon[1].getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[1].getIconWidth(), CharIcon[1].getIconHeight(), CharIcon[1]);
                ui.diaPanel.add(Chara2);
                ui.resizeComponents();
                ui.diaPanel.setComponentZOrder(Chara2, 1);
            }
        });
        DialogueBox.finalize(ui, ds.dc());
    }
}
