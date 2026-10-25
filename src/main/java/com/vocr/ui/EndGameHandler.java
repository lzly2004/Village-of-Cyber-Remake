package com.vocr.ui;

import com.vocr.resources.BgmTrack;
import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;
import com.vocr.application.EventPresentationStage;
import com.vocr.application.PresentedEvent;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.Event;

import javax.swing.*;
import java.awt.*;

public class EndGameHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        UI.Scene scene = ui.currentScene;
        PresentedEvent presented = ui.pollPresentedEvent();
        Event event = seekSettlementEvent(ui, presented);
        ImageIcon bgIcon;
        switch (scene) {
            case END_FOX:
                ui.resources.playBgm(BgmTrack.DEFEAT_SCREEN);
                bgIcon = ui.resources.getImage(UiImage.END_FOX);
                break;
            case END_WOLF:
                ui.resources.playBgm(BgmTrack.DEFEAT_SCREEN);
                bgIcon = ui.resources.getImage(UiImage.END_WOLF);
                break;
            case END_VILLAGE:
            default:
                ui.resources.playBgm(BgmTrack.VICTORY_SCREEN);
                bgIcon = ui.resources.getImage(UiImage.END_VILLAGE);
                break;
        }
        DialogueBox.Components dc = DialogueBox.setupWithIcon(ui, bgIcon);
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 20, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1()));
        dc.dialogPanel.add(nameLabel);
        Timer typeTimer = UIHelpers.bindTypewriter(dc.dialogText, ui.resources.getEventText(event), dc.nextBtn, () -> {
            dc.dialogPanel.setVisible(false);
            switch (scene) {
                case END_VILLAGE: ui.resources.playSound(SoundEffect.VILLAGE_VICTORY); break;
                case END_WOLF:    ui.resources.playSound(SoundEffect.WOLF_VICTORY); break;
                case END_FOX:     ui.resources.playSound(SoundEffect.FOX_VICTORY); break;
            }
            Timer t = new Timer(UIConstants.ENDING_DISPLAY_MS, e1 -> {
                ui.currentScene = UI.Scene.END_ANIME;
                ui.run();
                ((Timer) e1.getSource()).stop();
            });
            t.start();
        });
        dc.dialogPanel.setVisible(true);
        ImageIcon[] CharIcon = ui.resources.getEventImage(event);
        JLabel Chara = new JLabel();
        if (!ui.linkIcon.isEmpty()) {
            UIHelpers.renderLinkIconPair(ui, CharIcon);
        } else {
            UIHelpers.renderDialogueCharacter(ui, event, CharIcon);
        }
        DialogueBox.finalize(ui, dc);
    }

    private static final int MAX_SEEK_ITERATIONS = 200;

    private static Event seekSettlementEvent(UI ui, PresentedEvent current) {
        int iterations = 0;
        PresentedEvent presented = current;
        while (presented != null && iterations < MAX_SEEK_ITERATIONS) {
            if (presented.stage() == EventPresentationStage.SETTLEMENT)
                return presented.event();
            presented = ui.pollPresentedEvent();
            iterations++;
        }
        if (presented == null) {
            DebugLogger.error("EndGameHandler: 事件队列为空，未找到目标事件");
        } else if (iterations >= MAX_SEEK_ITERATIONS) {
            DebugLogger.error("EndGameHandler: 超过最大迭代次数，未找到目标事件");
        }
        return presented == null ? null : presented.event();
    }
}
