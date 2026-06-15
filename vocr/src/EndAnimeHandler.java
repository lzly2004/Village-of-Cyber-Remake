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
        nextBtn.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = UI.Scene.START_SCENE;
            ui.run();
        });
        ui.jPanel.add(nextBtn);
        for (int i = 1; i < ui.gs.gc.length; i++) {
            StringBuilder infoText = new StringBuilder();
            infoText.append("公称職業:\n").append(ui.uiComponentFactory.getZY(ui.gs.gc[i].claimedRole))
                    .append("\n真の職業:\n").append(ui.uiComponentFactory.getZY(ui.gs.gc[i].actualRole)).append("\n");
            if (ui.gs.gc[i].whyDie != whyDie.NONE) {
                switch (ui.gs.gc[i].whyDie) {
                    case beiyao:    infoText.append(ui.gs.gc[i].dieDay).append("日目狼噛"); break;
                    case chuxing:   infoText.append(ui.gs.gc[i].dieDay).append("日目処刑 "); break;
                    case zhousha:   infoText.append(ui.gs.gc[i].dieDay).append("日目呪殺 "); break;
                    case dayhouzhui:
                    case nighthouzhui: infoText.append(ui.gs.gc[i].dieDay).append("日目後追 "); break;
                    case daymaozhou:
                    case nightmaozhou: infoText.append(ui.gs.gc[i].dieDay).append("日目猫呪"); break;
                    default: break;
                }
            } else {
                if (ui.gs.end == 1) {
                    infoText.append("最終存活");
                } else if (ui.gs.end == 2) {
                    if (ui.gs.gc[i].actualRole < 7 || ui.gs.gc[i].actualRole > 9) {
                        infoText.append("最終死亡");
                    } else {
                        infoText.append("最終胜利");
                    }
                } else if (ui.gs.end == 3) {
                    if (ui.gs.gc[i].actualRole < 10) {
                        infoText.append("最終死亡");
                    } else {
                        infoText.append("最終胜利");
                    }
                }
            }
            DebugLogger.log(infoText);
            JTextArea infoLabel = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 16,
                    infoText.toString(), false);
            StringBuilder xName = new StringBuilder();
            StringBuilder imageName = new StringBuilder();
            if (ui.gs.gc[i].number <= 9) imageName.append("0");
            imageName.append(ui.gs.gc[i].number);
            switch (ui.gs.gc[i].whyDie) {
                case NONE:
                    switch (ui.gs.gc[i].actualRole) {
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
            String textName = ui.gs.gc[i].number + "job.png";
            ImageIcon characterImage = ui.resources.getImage(imageName.toString());
            ImageIcon characterText = ui.resources.getImage(textName);
            if (!xName.isEmpty()) {
                ImageIcon deathImage = ui.resources.getImage(xName.toString());
                JLabel deathLabel;
                if (i <= (ui.gs.gc.length - 1 + 1) / 2) {
                    deathLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            22 + (characterImage.getIconWidth() + 40) * i, 110,
                            deathImage.getIconWidth(), deathImage.getIconHeight(), deathImage);
                } else {
                    deathLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            22 + (characterImage.getIconWidth() + 40) * (i - ((ui.gs.gc.length - 1 + 1) / 2)),
                            260 + characterImage.getIconHeight(),
                            deathImage.getIconWidth(), deathImage.getIconHeight(), deathImage);
                }
                ui.jPanel.add(deathLabel);
            }
            JLabel label;
            JLabel textLabel;
            if (i <= (ui.gs.gc.length - 1 + 1) / 2) {
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
                        20 + (characterImage.getIconWidth() + 40) * (i - ((ui.gs.gc.length - 1 + 1) / 2)),
                        250 + characterImage.getIconHeight(),
                        characterImage.getIconWidth(), characterImage.getIconHeight(), characterImage);
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                        35 + (characterImage.getIconWidth() + 40) * (i - ((ui.gs.gc.length - 1 + 1) / 2)),
                        250 + 2 * characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
                infoLabel.setBounds(20 + (characterImage.getIconWidth() + 40) * (i - ((ui.gs.gc.length - 1 + 1) / 2)),
                        460, 100, 150);
            }
            ui.jPanel.add(infoLabel);
            ui.jPanel.add(textLabel);
            ui.jPanel.add(label);
        }
        String winIconText = "";
        String winText = "";
        switch (ui.gs.end) {
            case 1: winIconText = "Icon1_0.png"; winText = "村人勝利"; break;
            case 2: winIconText = "Icon2.png";   winText = "人狼勝利"; break;
            case 3: winIconText = "Icon4.png";   winText = "妖狐勝利"; break;
        }
        JLabel winLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 50, 25, 40, 40,
                ui.resources.getImage(winIconText));
        ui.jPanel.add(winLabel);
        JTextArea infoLabel = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, 24, winText, false);
        infoLabel.setBounds(100, 29, 200, 100);
        ui.jPanel.add(infoLabel);
        ui.jPanel.add(background);
        ui.resizeComponents();
        ui.jFrame.setVisible(true);
    }
}