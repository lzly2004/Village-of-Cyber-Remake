// DialogueAfternoonHandler.java - 下午对话场景处理器（用于回避CO）
import javax.swing.*;
import java.awt.*;

public class DialogueAfternoonHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        Event event = ui.getEvents().poll();
        UIHelpers.DialogueSetup ds = UIHelpers.prepareDialogueEvent(ui, "haikei.png", event);
        UIHelpers.bindTypewriter(ds.dc().dialogText, ds.text(), ds.dc().nextBtn, () -> {
            if (ui.getEvents().isEmpty()) {
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