import javax.swing.*;
import java.awt.*;

public class EndAnimeHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        ui.jPanel.removeAll();
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("frame #19252.png"));
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,
                1130, 720 - GameConstants.RETURN_HEIGHT,
                GameConstants.RETURN_WIDTH * 6 / 10, GameConstants.RETURN_HEIGHT * 6 / 10,
                ui.resources.getImage("PVBtitile.png"));
        nextBtn.addActionListener(e -> ui.transitionTo(UI.Scene.START_SCENE));
        ui.jPanel.add(nextBtn);
        for (int i = 1; i <= ui.ctx.getPlayerSum(); i++) {
            StringBuilder infoText = new StringBuilder();
            infoText.append(GameStrings.CLAIMED_ROLE_PREFIX).append(ui.uiComponentFactory.getZY(ui.ctx.getClaimedRole(i)))
                    .append(GameStrings.ACTUAL_ROLE_PREFIX).append(ui.uiComponentFactory.getZY(ui.ctx.getActualRole(i))).append("\n");
            if (!ui.ctx.isAlive(i)) {
                switch (ui.ctx.getDeathReason(i)) {
                    case beiyao:    infoText.append(GameStrings.getDeathBite(ui.ctx.getDeathDay(i))); break;
                    case chuxing:   infoText.append(GameStrings.getDeathExecute(ui.ctx.getDeathDay(i))); break;
                    case zhousha:   infoText.append(GameStrings.getDeathCurse(ui.ctx.getDeathDay(i))); break;
                    case dayhouzhui:
                    case nighthouzhui: infoText.append(GameStrings.getDeathFollow(ui.ctx.getDeathDay(i))); break;
                    case daymaozhou:
                    case nightmaozhou: infoText.append(GameStrings.getDeathCat(ui.ctx.getDeathDay(i))); break;
                    default: break;
                }
            } else {
                GameResult result = ui.ctx.getEndResult();
                if (result == GameResult.VILLAGE_WIN) {
                    infoText.append(GameStrings.END_SURVIVE);
                } else if (result == GameResult.WOLF_WIN) {
                    if (ui.ctx.getActualRole(i) < 7 || ui.ctx.getActualRole(i) > 9) {
                        infoText.append(GameStrings.END_DEAD);
                    } else {
                        infoText.append(GameStrings.END_WIN);
                    }
                } else if (result == GameResult.FOX_WIN) {
                    if (ui.ctx.getActualRole(i) < 10) {
                        infoText.append(GameStrings.END_DEAD);
                    } else {
                        infoText.append(GameStrings.END_WIN);
                    }
                }
            }
            DebugLogger.log(infoText);
            JTextArea infoLabel = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 16,
                    infoText.toString());
            StringBuilder xName = new StringBuilder();
            String imageName = GameStrings.buildCharacterImageName(ui.ctx.getCharacterNumber(i), ui.ctx.getDeathReason(i), ui.ctx.getActualRole(i), true);
            xName.append(ui.ctx.getDeathReason(i).getDeathIconName());
            int charNumber = ui.ctx.getCharacterNumber(i);
            ImageIcon characterImage = ui.resources.getImage(imageName.toString());
            if (characterImage == null) continue;
            int charWidth = characterImage.getIconWidth();
            int charHeight = characterImage.getIconHeight();
            int playerSum = ui.ctx.getPlayerSum();
            int charIndex = UIHelpers.calculateRowIndex(i, playerSum);
            boolean isFirstRow = UIHelpers.isFirstRow(i, playerSum);
            int spacing = charWidth + 40;

            if (!xName.isEmpty()) {
                int deathX = 22 + spacing * charIndex;
                int deathY = isFirstRow ? 110 : (260 + charHeight);
                JLabel deathLabel = UIHelpers.createDeathMarker(ui, ui.ctx.getDeathReason(i), deathX, deathY);
                if (deathLabel != null) ui.jPanel.add(deathLabel);
            }

            JLabel label;
            JLabel textLabel;
            if (isFirstRow) {
                label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        20 + spacing * charIndex, 100, charWidth, charHeight, characterImage);
                textLabel = UIHelpers.createCharacterText(ui, charNumber,
                        35 + spacing * charIndex, 100 + charHeight - 10, true);
                infoLabel.setBounds(20 + spacing * charIndex, 210, 100, 150);
            } else {
                label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        20 + spacing * charIndex, 250 + charHeight, charWidth, charHeight, characterImage);
                textLabel = UIHelpers.createCharacterText(ui, charNumber,
                        35 + spacing * charIndex, 250 + 2 * charHeight - 10, true);
                infoLabel.setBounds(20 + spacing * charIndex, 460, 100, 150);
            }
            ui.jPanel.add(infoLabel);
            if (textLabel != null) ui.jPanel.add(textLabel);
            ui.jPanel.add(label);
        }
        String winIconText = "";
        String winText = "";
        switch (ui.ctx.getEndResult()) {
            case VILLAGE_WIN: winIconText = "Icon1_0.png"; winText = GameStrings.WIN_VILLAGER; break;
            case WOLF_WIN: winIconText = "Icon2.png";   winText = GameStrings.WIN_WOLF; break;
            case FOX_WIN: winIconText = "Icon4.png";   winText = GameStrings.WIN_FOX; break;
        }
        JLabel winLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 50, 25, 40, 40,
                ui.resources.getImage(winIconText));
        ui.jPanel.add(winLabel);
        JTextArea infoLabel = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 24, winText);
        infoLabel.setBounds(100, 29, 200, 100);
        ui.jPanel.add(infoLabel);
        ui.jPanel.add(background);
        ui.resizeComponents();
        ui.getJFrame().setVisible(true);
    }
}