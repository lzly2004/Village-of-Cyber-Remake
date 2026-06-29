import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色選択パネル —— 从 GameSceneVoteHandler.createCharacterSelectionPanel() 提取。
 * 负责渲染拖拽式角色选择面板（投票/占卜/护卫）。
 */
class CharacterSelectionPanel {

    private static final String[] FRAME_ICONS = {"frameSBlue.png", "frameSRed.png", "frameOrange.png"};
    private static final String[] RESULT_ALL_ICONS = {"result2_all.png", "result1_all.png", "result3_all.png"};
    private static final String[] DRAG_ALL_ICONS = {"touhyou.png", "uranaiAll.png", "goeiAll.png"};
    private static final int[] ROLE_FILTERS = {0, 1, 3};

    static void render(UI ui, VoteButtonPanel vp, VoteInfoRenderer vi,
                       JPanel panel, List<Integer> chosenList, boolean[] flag,
                       SelectionType type, GameSceneVoteHandler handler) {
        UIHelpers.hideButtons(vp.fixedVoteBtn, vp.fixedUranaiBtn, vp.protectBtn);
        vi.infoText.setVisible(false);
        vp.returnBtn.setVisible(true);

        panel.setVisible(false);
        panel.removeAll();
        ui.jPanel.add(panel);
        ui.jPanel.setComponentZOrder(panel, 0);
        int n = ui.ctx.getPlayerSum() + 1;

        int typeIdx = type.ordinal();
        String frameIconName = FRAME_ICONS[typeIdx];
        String resultAllIconName = RESULT_ALL_ICONS[typeIdx];
        String dragAllIconName = DRAG_ALL_ICONS[typeIdx];
        int claimedRoleFilter = ROLE_FILTERS[typeIdx];

        List<Integer> roleNums = new ArrayList<>();
        List<Integer> roleOrders = new ArrayList<>();
        List<Integer> trueNums = new ArrayList<>();
        for (int i = 1; i <= n - 1; i++) {
            if (ui.ctx.getClaimedRole(i) == claimedRoleFilter && ui.ctx.isAlive(i)) {
                roleNums.add(i);
                roleOrders.add(ui.ctx.getClaimedRoleOrder(i));
            }
            if ((ui.ctx.getActualRole(i) == claimedRoleFilter || ui.ctx.getClaimedRole(i) == claimedRoleFilter)
                    && ui.ctx.isAlive(i)) {
                trueNums.add(i);
            }
        }

        List<JLabel> targetLabels = new ArrayList<>();
        List<JLabel> frameLabels = new ArrayList<>();
        List<JLabel> resultLabels = new ArrayList<>();
        List<JLabel> zbLabels = new ArrayList<>();

        for (int i = 1; i <= n - 1; i++) {
            String imageName = handler.charImageName(i);

            if (ui.ctx.getClaimedRole(i) > 0 && ui.ctx.getClaimedRole(i) < 6) {
                StringBuilder crName = new StringBuilder("yaku");
                if (ui.ctx.getClaimedRole(i) <= 3)
                    crName.append(ui.ctx.getClaimedRole(i)).append("_").append(ui.ctx.getClaimedRoleOrder(i)).append(".png");
                else
                    crName.append(ui.ctx.getClaimedRole(i)).append(".png");
                JLabel crLabel = new JLabel(ui.resources.getImage(crName.toString()));
                crLabel.setBounds(handler.charGridX(i, 60, 74), handler.charGridY(i, 20, 128),
                        crLabel.getIcon().getIconWidth(), crLabel.getIcon().getIconHeight());
                panel.add(crLabel);
            }

            JLabel chooseLabel = new JLabel(ui.resources.getImage(frameIconName));
            frameLabels.add(chooseLabel);
            chooseLabel.setBounds(handler.charGridX(i, 60, 74), handler.charGridY(i, 20, 128), 64, 98);
            panel.add(chooseLabel);
            chooseLabel.setVisible(chosenList.contains(i));

            if (type == SelectionType.VOTE) {
                JLabel voteLabel = new JLabel(ui.resources.getImage("result2_all.png"));
                voteLabel.setBounds(handler.charGridX(i, 65, 74), handler.charGridY(i, 20, 128),
                        voteLabel.getIcon().getIconWidth(), voteLabel.getIcon().getIconHeight());
                panel.add(voteLabel);
                panel.setComponentZOrder(voteLabel, 0);
                voteLabel.setVisible(ui.ctx.isSelectedVoteTarget(i, ui.ctx.getGameDay()));
            }

            JLabel resultLabel = new JLabel(ui.resources.getImage(resultAllIconName));
            resultLabels.add(resultLabel);
            resultLabel.setBounds(handler.charGridX(i, 65, 74), handler.charGridY(i, 40, 148),
                    resultLabel.getIcon().getIconWidth(), resultLabel.getIcon().getIconHeight());
            panel.add(resultLabel);
            resultLabel.setVisible(chosenList.contains(i));

            if (type != SelectionType.VOTE) {
                String zbIconPrefix = (type == SelectionType.DIVINATION) ? "result1_" : "result3_";
                String zbSuffix = (type == SelectionType.DIVINATION) ? "white.png" : ".png";
                for (int r = 0; r < roleNums.size(); r++) {
                    JLabel zbLabel = new JLabel(ui.resources.getImage(zbIconPrefix + roleOrders.get(r) + zbSuffix));
                    zbLabels.add(zbLabel);
                    zbLabel.setBounds(handler.charGridX(i, 65 + zbLabel.getIcon().getIconWidth(), 74),
                            handler.charGridY(i, 20 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r),
                                    128 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r)),
                            zbLabel.getIcon().getIconWidth(), zbLabel.getIcon().getIconHeight());
                    panel.add(zbLabel);
                    zbLabel.setVisible(chosenList.contains(i));
                }
            }

            ImageIcon charImg = ui.resources.getImage(imageName);
            JLabel label = new JLabel(charImg);
            targetLabels.add(label);
            label.setBounds(handler.charGridX(i, 60, charImg.getIconWidth() + 10),
                    handler.charGridY(i, 20, 30 + charImg.getIconHeight()),
                    charImg.getIconWidth(), charImg.getIconHeight());
            panel.add(label);
        }

        handler.renderSkillTargets(panel, 50, 74, 20, 128);

        panel.setBounds(GameConstants.INFO_PANEL_X, GameConstants.INFO_PANEL_Y,
                200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight());
        panel.add(LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight(), vi.boardIcon));

        JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 250, 350,
                ui.resources.getImage(dragAllIconName).getIconWidth() / 2,
                ui.resources.getImage(dragAllIconName).getIconHeight() / 2,
                ui.resources.getImage(dragAllIconName));
        dragBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                Container parent = dragBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                Rectangle btnRect = dragBtn.getBounds();
                int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    if (!label.getBounds().contains(cx, cy)) continue;
                    int idx = targetLabels.indexOf(label);
                    if (!ui.ctx.isAlive(idx + 1)) break;
                    ui.resources.playSound("click.wav");
                    switch (type) {
                        case VOTE:
                            ui.ctx.setIsSelectedVoteTarget(idx + 1, ui.ctx.getGameDay(), true);
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            handler.updateFlagFromVoteTargets(flag);
                            break;
                        case DIVINATION:
                            ui.ctx.getHiddenSeerScheduledSkillTargets()[idx + 1][ui.ctx.getGameDay()] = true;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.ctx.setClaimedRoleScheduled(trueNums.get(a), idx + 1, ui.ctx.getGameDay(), true);
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            break;
                        case GUARD:
                            ui.ctx.getHiddenHunterScheduledSkillTargets()[idx + 1][ui.ctx.getGameDay()] = true;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.ctx.setClaimedRoleScheduled(trueNums.get(a), idx + 1, ui.ctx.getGameDay(), true);
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            break;
                    }
                    resultLabels.get(idx).setVisible(true);
                    frameLabels.get(idx).setVisible(true);
                    frameLabels.get(idx).repaint();
                    ui.jPanel.repaint(label.getBounds());
                    break;
                }
                dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
        panel.add(dragBtn);
        panel.setComponentZOrder(dragBtn, 0);

        if (type != SelectionType.VOTE) {
            String perIconPrefix = (type == SelectionType.DIVINATION) ? "uranai" : "goei";
            for (int r = 0; r < roleNums.size(); r++) {
                final int cur = r;
                JButton perBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                        roleOrders.get(r) * (type == SelectionType.DIVINATION ? 100 : 150)
                                + (type == SelectionType.DIVINATION ? 150 : 250), 350,
                        ui.resources.getImage(perIconPrefix + roleOrders.get(r) + ".png").getIconWidth() / 2,
                        ui.resources.getImage(perIconPrefix + roleOrders.get(r) + ".png").getIconHeight() / 2,
                        ui.resources.getImage(perIconPrefix + roleOrders.get(r) + ".png"));
                perBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        Container parent = perBtn.getParent();
                        if (parent == null || targetLabels.isEmpty()) return;
                        Rectangle btnRect = perBtn.getBounds();
                        int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                        for (JLabel label : targetLabels) {
                            if (label.getParent() != parent || !label.isVisible()) continue;
                            if (!label.getBounds().contains(cx, cy)) continue;
                            int idx = targetLabels.indexOf(label);
                            if (!ui.ctx.isAlive(idx + 1)) break;
                            ui.resources.playSound("click.wav");
                            ui.ctx.setClaimedRoleScheduled(roleNums.get(cur), idx + 1, ui.ctx.getGameDay(), true);
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            zbLabels.get(idx + cur * ui.ctx.getPlayerSum()).setVisible(true);
                            frameLabels.get(idx).setVisible(true);
                            frameLabels.get(idx).repaint();
                            ui.jPanel.repaint(label.getBounds());
                            break;
                        }
                        perBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                });
                panel.add(perBtn);
                panel.setComponentZOrder(perBtn, 0);
            }
        }

        JButton delBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                (type == SelectionType.VOTE) ? 500 : 800, 350,
                ui.resources.getImage("delete.png").getIconWidth() / 2,
                ui.resources.getImage("delete.png").getIconHeight() / 2,
                ui.resources.getImage("delete.png"));
        delBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                Container parent = delBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                Rectangle btnRect = delBtn.getBounds();
                int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    if (!label.getBounds().contains(cx, cy)) continue;
                    int idx = targetLabels.indexOf(label);
                    if (!ui.ctx.isAlive(idx + 1)) break;
                    ui.resources.playSound("click.wav");
                    chosenList.remove(Integer.valueOf(idx + 1));
                    switch (type) {
                        case VOTE:
                            ui.ctx.setIsSelectedVoteTarget(idx + 1, ui.ctx.getGameDay(), false);
                            handler.updateFlagFromVoteTargets(flag);
                            break;
                        case DIVINATION:
                            ui.ctx.getHiddenSeerScheduledSkillTargets()[idx + 1][ui.ctx.getGameDay()] = false;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.ctx.setClaimedRoleScheduled(trueNums.get(a), idx + 1, ui.ctx.getGameDay(), false);
                            handler.updateFlagFromScheduledTargets(trueNums, flag, true);
                            break;
                        case GUARD:
                            ui.ctx.getHiddenHunterScheduledSkillTargets()[idx + 1][ui.ctx.getGameDay()] = false;
                            for (int a = 0; a < trueNums.size(); a++)
                                ui.ctx.setClaimedRoleScheduled(trueNums.get(a), idx + 1, ui.ctx.getGameDay(), false);
                            handler.updateFlagFromScheduledTargets(trueNums, flag, false);
                            break;
                    }
                    if (type != SelectionType.VOTE) {
                        for (int u = 0; u < roleNums.size(); u++)
                            zbLabels.get(idx + u * (ui.ctx.getPlayerSum())).setVisible(false);
                    }
                    resultLabels.get(idx).setVisible(false);
                    frameLabels.get(idx).setVisible(false);
                    frameLabels.get(idx).repaint();
                    ui.jPanel.repaint(label.getBounds());
                    break;
                }
                delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
        panel.add(delBtn);
        panel.setComponentZOrder(delBtn, 0);

        panel.setVisible(true);
        panel.revalidate();
        panel.repaint();
        ui.resizeComponents();
    }
}