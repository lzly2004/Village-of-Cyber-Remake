package com.vocr.ui;

import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;
import com.vocr.application.EventPresentationStage;
import com.vocr.application.PresentedEvent;

import com.vocr.domain.Event;
import com.vocr.domain.EventName;

import javax.swing.*;
import java.awt.*;

public class DialogueDayDeathHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        PresentedEvent next = ui.peekPresentedEvent();
        if (next == null || next.stage() != EventPresentationStage.NIGHT_RESULT) {
            UIHelpers.playDayPhaseBgm(ui);
            ui.currentScene = UI.Scene.DIALOGUE_DAY;
            ui.run();
            return;
        }
        if (next != null && next.event().eventname() == EventName.wsw) {
            ui.resources.playSound(SoundEffect.PEACEFUL_NIGHT);
            DialogueBox.Components dc = DialogueBox.setup(ui, UiImage.DIALOGUE_DAY_BACKGROUND);
            ui.pollPresentedEvent();
            UIHelpers.bindTypewriter(dc.dialogText, GameStrings.NO_SACRIFICE, dc.nextBtn, () -> {
                UIHelpers.playDayPhaseBgm(ui);
                ui.currentScene = UI.Scene.DIALOGUE_DAY;
                ui.run();
            });
            dc.dialogPanel.setVisible(true);
            DialogueBox.finalize(ui, dc);
        } else {
            PresentedEvent presented = ui.pollPresentedEvent();
            Event event = presented.event();
            ui.jPanel.removeAll();
            UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(
                    ui, UiImage.DIALOGUE_DAY_BACKGROUND, event);
            ds.dc().nextBtn.setVisible(false);
            ds.dc().nextBtn.setEnabled(false);
            Timer typeTimer = UIHelpers.bindTypewriter(ds.dc().dialogText, ds.text(), ds.dc().nextBtn, () -> {
                if (!ui.hasPendingEvents(EventPresentationStage.NIGHT_RESULT)) {
                    UIHelpers.playDayPhaseBgm(ui);
                    ui.currentScene = UI.Scene.DIALOGUE_DAY;
                }
                ui.run();
            });
            ds.dc().dialogPanel.setVisible(true);
            ImageIcon[] CharIcon = ds.charIcon();
            if (CharIcon.length != 0) {
                typeTimer.stop();
                ds.dc().dialogPanel.setVisible(false);
                JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        (UIConstants.WINDOW_WIDTH - CharIcon[0].getIconWidth()) / 2,
                        UIConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                ui.diaPanel.add(Chara);
                Timer t1 = new Timer(UIConstants.TRANSITION_SHORT_MS, e -> {
                    ui.resources.playSound(SoundEffect.NIGHT_DEATH);
                    Chara.setVisible(false);
                    if (CharIcon.length >= 2) {
                        JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                                (UIConstants.WINDOW_WIDTH - CharIcon[1].getIconWidth()) / 2,
                                UIConstants.WINDOW_HEIGHT - CharIcon[1].getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                                CharIcon[1].getIconWidth(), CharIcon[1].getIconHeight(), CharIcon[1]);
                        ui.diaPanel.add(Chara2);
                        ui.diaPanel.setComponentZOrder(Chara2, 1);
                    }
                    ui.resizeComponents();
                    ((Timer) e.getSource()).stop();
                });
                t1.start();
                Timer t2 = new Timer(UIConstants.TRANSITION_MEDIUM_MS, e -> {
                    ds.dc().dialogPanel.setVisible(true);
                    ds.dc().nextBtn.setVisible(true);
                    ds.dc().nextBtn.setEnabled(true);
                    typeTimer.start();
                    ((Timer) e.getSource()).stop();
                });
                t2.start();
            }
            ds.dc().nextBtn.setVisible(false);
            DialogueBox.finalize(ui, ds.dc());
        }
    }
}
