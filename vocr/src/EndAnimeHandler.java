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
                if (ui.ctx.getEndResult() == 1) {
                    infoText.append(GameStrings.END_SURVIVE);
                } else if (ui.ctx.getEndResult() == 2) {
                    if (ui.ctx.getActualRole(i) < 7 || ui.ctx.getActualRole(i) > 9) {
                        infoText.append(GameStrings.END_DEAD);
                    } else {
                        infoText.append(GameStrings.END_WIN);
                    }
                } else if (ui.ctx.getEndResult() == 3) {
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
            StringBuilder imageName = new StringBuilder();
            if (ui.ctx.getCharacterNumber(i) <= 9) imageName.append("0");
            imageName.append(ui.ctx.getCharacterNumber(i));
            switch (ui.ctx.getDeathReason(i)) {
                case NONE:
                    switch (ui.ctx.getActualRole(i)) {
                        case 5:  imageName.append("cs"); break;
                        case 10: imageName.append("fs"); break;
                        case 11: imageName.append("hs"); break;
                        case 7:  imageName.append("ws"); break;
                        case 8:
                        case 9:  imageName.append("ks"); break;
                        default: imageName.append("s");  break;
                    }
                    break;
                case chuxing:     imageName.append("gs"); xName.append("turi.png");   break;
                case daymaozhou:  imageName.append("gs"); xName.append("noroi.png");  break;
                case dayhouzhui:  imageName.append("gs"); xName.append("atooi.png");  break;
                default:          imageName.append("gs"); xName.append("kami.png");   break;
            }
            imageName.append(".png");
            String textName = ui.ctx.getCharacterNumber(i) + "job.png";
            ImageIcon characterImage = ui.resources.getImage(imageName.toString());
            ImageIcon characterText = ui.resources.getImage(textName);
            if (!xName.isEmpty()) {
                ImageIcon deathImage = ui.resources.getImage(xName.toString());
                JLabel deathLabel;
                if (i <= (ui.ctx.getPlayerSum() + 1) / 2) {
                    deathLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            22 + (characterImage.getIconWidth() + 40) * i, 110,
                            deathImage.getIconWidth(), deathImage.getIconHeight(), deathImage);
                } else {
                    deathLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            22 + (characterImage.getIconWidth() + 40) * (i - ((ui.ctx.getPlayerSum() + 1) / 2)),
                            260 + characterImage.getIconHeight(),
                            deathImage.getIconWidth(), deathImage.getIconHeight(), deathImage);
                }
                ui.jPanel.add(deathLabel);
            }
            JLabel label;
            JLabel textLabel;
            if (i <= (ui.ctx.getPlayerSum() + 1) / 2) {
                label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        20 + (characterImage.getIconWidth() + 40) * i, 100,
                        characterImage.getIconWidth(), characterImage.getIconHeight(), characterImage);
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                        35 + (characterImage.getIconWidth() + 40) * i,
                        100 + characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
                infoLabel.setBounds(20 + (characterImage.getIconWidth() + 40) * i, 210, 100, 150);
            } else {
                label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        20 + (characterImage.getIconWidth() + 40) * (i - ((ui.ctx.getPlayerSum() + 1) / 2)),
                        250 + characterImage.getIconHeight(),
                        characterImage.getIconWidth(), characterImage.getIconHeight(), characterImage);
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                        35 + (characterImage.getIconWidth() + 40) * (i - ((ui.ctx.getPlayerSum() + 1) / 2)),
                        250 + 2 * characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
                infoLabel.setBounds(20 + (characterImage.getIconWidth() + 40) * (i - ((ui.ctx.getPlayerSum() + 1) / 2)),
                        460, 100, 150);
            }
            ui.jPanel.add(infoLabel);
            ui.jPanel.add(textLabel);
            ui.jPanel.add(label);
        }
        String winIconText = "";
        String winText = "";
        switch (ui.ctx.getEndResult()) {
            case 1: winIconText = "Icon1_0.png"; winText = GameStrings.WIN_VILLAGER; break;
            case 2: winIconText = "Icon2.png";   winText = GameStrings.WIN_WOLF; break;
            case 3: winIconText = "Icon4.png";   winText = GameStrings.WIN_FOX; break;
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