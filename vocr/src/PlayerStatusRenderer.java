import javax.swing.*;

/**
 * 玩家状态渲染器 —— 从 GameSceneVoteHandler.renderPlayerStatuses() 提取。
 * 负责渲染角色头像、死亡标记、CO役职图标、技能目标标记。
 */
class PlayerStatusRenderer {

    static void render(UI ui, GameSceneVoteHandler handler) {
        for (int i = 1; i <= ui.ctx.getPlayerSum(); i++) {
            StringBuilder xName = new StringBuilder();
            StringBuilder imageName = new StringBuilder();
            if (ui.ctx.getCharacterNumber(i) <= 9) imageName.append("0");
            imageName.append(ui.ctx.getCharacterNumber(i));
            switch (ui.ctx.getDeathReason(i)) {
                case NONE: break;
                case chuxing: imageName.append("g"); xName.append("turi.png"); break;
                case daymaozhou: imageName.append("g"); xName.append("noroi.png"); break;
                case dayhouzhui: imageName.append("g"); xName.append("atooi.png"); break;
                default: imageName.append("g"); xName.append("kami.png"); break;
            }
            imageName.append("s.png");
            String textName = ui.ctx.getCharacterNumber(i) + "job.png";
            String claimedRoleIconName = handler.claimedRoleIconName(i);
            StringBuilder skillTargetName = new StringBuilder("result");
            if (ui.ctx.getClaimedRole(i) > 0 && ui.ctx.getClaimedRole(i) < 6) {
                if (ui.ctx.getClaimedRole(i) <= 3) {
                    skillTargetName.append(ui.ctx.getClaimedRole(i)).append("_")
                            .append(ui.ctx.getClaimedRoleOrder(i));
                    int lastSkillTarget = ui.ctx.getSkillTarget(i, ui.ctx.getGameDay() - 1);
                    if (lastSkillTarget != 0) {
                        ui.claimedRolenum[i][ui.ctx.getGameDay()] = ui.ctx.getClaimedRole(i);
                        ui.skillTargetOrder[i][ui.ctx.getGameDay()] = ui.ctx.getClaimedRoleOrder(i);
                        if (ui.ctx.getClaimedRole(i) != 3) {
                            if (lastSkillTarget >= ui.ctx.getPlayerSum() + 1) {
                                skillTargetName.append("black.png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget - ui.ctx.getPlayerSum());
                            } else {
                                skillTargetName.append("white.png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget);
                            }
                        } else {
                            if (lastSkillTarget >= ui.ctx.getPlayerSum() + 1) {
                                skillTargetName.append(".png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget - ui.ctx.getPlayerSum());
                            } else {
                                skillTargetName.append(".png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget);
                            }
                        }
                        ui.skillTargetNames[i][ui.ctx.getGameDay()] = (skillTargetName.toString());
                    }
                }
                ImageIcon claimedRoleIcon = ui.resources.getImage(claimedRoleIconName);
                JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                    claimedRoleLabel.setBounds(160 + 64 * i, 0,
                            claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                else
                    claimedRoleLabel.setBounds(160 + 64 * (i - ((ui.ctx.getPlayerSum() + 1) / 2)), 98,
                            claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                ui.jPanel.add(claimedRoleLabel);
            }
            if (!xName.isEmpty()) {
                ImageIcon deathImage = ui.resources.getImage(xName.toString());
                JLabel deathLabel = new JLabel(deathImage);
                if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                    deathLabel.setBounds(165 + 64 * i, 10,
                            deathImage.getIconWidth(), deathImage.getIconHeight());
                else
                    deathLabel.setBounds(165 + 64 * (i - ((ui.ctx.getPlayerSum() + 1) / 2)), 108,
                            deathImage.getIconWidth(), deathImage.getIconHeight());
                ui.jPanel.add(deathLabel);
            }
            ImageIcon characterImage = ui.resources.getImage(imageName.toString());
            ImageIcon characterText = ui.resources.getImage(textName);
            JLabel label = new JLabel(characterImage);
            JLabel textLabel;
            if (i <= (ui.ctx.getPlayerSum() + 1) / 2) {
                label.setBounds(160 + characterImage.getIconWidth() * i, 0,
                        characterImage.getIconWidth(), characterImage.getIconHeight());
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        175 + characterImage.getIconWidth() * i,
                        characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
            } else {
                label.setBounds(160 + characterImage.getIconWidth() * (i - (ui.ctx.getPlayerSum() + 1) / 2),
                        characterImage.getIconHeight(),
                        characterImage.getIconWidth(), characterImage.getIconHeight());
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        175 + characterImage.getIconWidth() * (i - (ui.ctx.getPlayerSum() + 1) / 2),
                        2 * characterImage.getIconHeight() - characterText.getIconHeight() / 2,
                        characterText.getIconWidth() / 2, characterText.getIconHeight() / 2, characterText);
            }
            ui.jPanel.add(textLabel);
            ui.jPanel.add(label);
        }
    }
}