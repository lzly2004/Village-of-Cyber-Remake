import javax.swing.*;

/**
 * 玩家状态渲染器 —— 从 GameSceneVoteHandler.renderPlayerStatuses() 提取。
 * 负责渲染角色头像、死亡标记、CO役职图标、技能目标标记。
 */
class PlayerStatusRenderer {

    private static final int STATUS_START_X = 160;
    private static final int STATUS_ITEM_WIDTH = 64;
    private static final int DEATH_OFFSET_X = 5;
    private static final int TEXT_OFFSET_X = 15;
    private static final int ROW_Y_OFFSET = 98;

    static void render(UI ui) {
        int playerSum = ui.ctx.getPlayerSum();
        int halfCount = (playerSum + 1) / 2;
        for (int i = 1; i <= playerSum; i++) {
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
            String claimedRoleIconName = ui.uiComponentFactory.getClaimedRoleIconName(
                    ui.ctx.getClaimedRole(i), ui.ctx.getClaimedRoleOrder(i));
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
                            if (lastSkillTarget >= playerSum + 1) {
                                skillTargetName.append("black.png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget - playerSum);
                            } else {
                                skillTargetName.append("white.png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget);
                            }
                        } else {
                            if (lastSkillTarget >= playerSum + 1) {
                                skillTargetName.append(".png");
                                ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget - playerSum);
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
                int roleX = STATUS_START_X + STATUS_ITEM_WIDTH * (i <= halfCount ? i : (i - halfCount));
                int roleY = i <= halfCount ? 0 : ROW_Y_OFFSET;
                claimedRoleLabel.setBounds(roleX, roleY,
                        claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                ui.jPanel.add(claimedRoleLabel);
            }
            if (!xName.isEmpty()) {
                ImageIcon deathImage = ui.resources.getImage(xName.toString());
                JLabel deathLabel = new JLabel(deathImage);
                int deathX = STATUS_START_X + DEATH_OFFSET_X + STATUS_ITEM_WIDTH * (i <= halfCount ? i : (i - halfCount));
                int deathY = i <= halfCount ? 10 : (10 + ROW_Y_OFFSET);
                deathLabel.setBounds(deathX, deathY,
                        deathImage.getIconWidth(), deathImage.getIconHeight());
                ui.jPanel.add(deathLabel);
            }
            ImageIcon characterImage = ui.resources.getImage(imageName.toString());
            ImageIcon characterText = ui.resources.getImage(textName);
            JLabel label = new JLabel(characterImage);
            JLabel textLabel;
            int charWidth = characterImage.getIconWidth();
            int charHeight = characterImage.getIconHeight();
            int textWidth = characterText.getIconWidth();
            int textHeight = characterText.getIconHeight();
            int charIndex = i <= halfCount ? i : (i - halfCount);
            if (i <= halfCount) {
                label.setBounds(STATUS_START_X + charWidth * charIndex, 0,
                        charWidth, charHeight);
                textLabel = UIFactory.makeLabel(LabelConst.Simple_Label,
                        STATUS_START_X + TEXT_OFFSET_X + charWidth * charIndex,
                        charHeight - textHeight / 2,
                        textWidth / 2, textHeight / 2, characterText);
            } else {
                label.setBounds(STATUS_START_X + charWidth * charIndex,
                        charHeight,
                        charWidth, charHeight);
                textLabel = UIFactory.makeLabel(LabelConst.Simple_Label,
                        STATUS_START_X + TEXT_OFFSET_X + charWidth * charIndex,
                        2 * charHeight - textHeight / 2,
                        textWidth / 2, textHeight / 2, characterText);
            }
            ui.jPanel.add(textLabel);
            ui.jPanel.add(label);
        }
    }
}