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
        for (int i = 1; i <= playerSum; i++) {
            int charIndex = UIHelpers.calculateRowIndex(i, playerSum);
            boolean isFirstRow = UIHelpers.isFirstRow(i, playerSum);
            int charNumber = ui.ctx.getCharacterNumber(i);
            whyDie deathReason = ui.ctx.getDeathReason(i);

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
                            skillTargetName.append(".png");
                            ui.skillTargetPeople[i][ui.ctx.getGameDay()] = (lastSkillTarget >= playerSum + 1)
                                    ? (lastSkillTarget - playerSum) : lastSkillTarget;
                        }
                        ui.skillTargetNames[i][ui.ctx.getGameDay()] = skillTargetName.toString();
                    }
                }
                int roleX = STATUS_START_X + STATUS_ITEM_WIDTH * charIndex;
                int roleY = isFirstRow ? 0 : ROW_Y_OFFSET;
                JLabel claimedRoleLabel = UIHelpers.createClaimedRoleIcon(ui, ui.ctx.getClaimedRole(i),
                        ui.ctx.getClaimedRoleOrder(i), roleX, roleY);
                if (claimedRoleLabel != null) ui.jPanel.add(claimedRoleLabel);
            }

            int deathX = STATUS_START_X + DEATH_OFFSET_X + STATUS_ITEM_WIDTH * charIndex;
            int deathY = isFirstRow ? 10 : (10 + ROW_Y_OFFSET);
            JLabel deathLabel = UIHelpers.createDeathMarker(ui, deathReason, deathX, deathY);
            if (deathLabel != null) ui.jPanel.add(deathLabel);

            JLabel label = UIHelpers.createPlayerAvatar(ui, charNumber, deathReason,
                    ui.ctx.getActualRole(i), false, STATUS_START_X + 64 * charIndex,
                    isFirstRow ? 0 : 98);
            if (label != null) {
                int charWidth = ((ImageIcon) label.getIcon()).getIconWidth();
                int charHeight = ((ImageIcon) label.getIcon()).getIconHeight();
                int textX = STATUS_START_X + TEXT_OFFSET_X + charWidth * charIndex;
                JLabel textLabel = UIHelpers.createCharacterText(ui, charNumber, textX,
                        isFirstRow ? charHeight : 2 * charHeight, false);
                if (textLabel != null) ui.jPanel.add(textLabel);
                ui.jPanel.add(label);
            }
        }
    }
}
