package com.vocr.ui;

import com.vocr.application.SetAllTargetSelectionCommand;
import com.vocr.application.SetClaimedRoleTargetSelectionCommand;
import com.vocr.resources.SoundEffect;
import com.vocr.resources.ImageNames;
import com.vocr.resources.UiImage;

import com.vocr.domain.GameContextView;
import com.vocr.domain.SelectionType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色選択パネル —— 从 GameSceneVoteHandler.createCharacterSelectionPanel() 提取。
 * 负责渲染拖拽式角色选择面板（投票/占卜/护卫）。
 */
class CharacterSelectionPanel {

    private static final UiImage[] FRAME_ICONS = {
            UiImage.SELECTION_FRAME_VOTE,
            UiImage.SELECTION_FRAME_DIVINATION,
            UiImage.SELECTION_FRAME_GUARD
    };
    private static final UiImage[] RESULT_ALL_ICONS = {
            UiImage.VOTE_RESULT_ALL,
            UiImage.DIVINATION_RESULT_ALL,
            UiImage.GUARD_RESULT_ALL
    };
    private static final UiImage[] DRAG_ALL_ICONS = {
            UiImage.VOTE_DRAG,
            UiImage.DIVINATION_DRAG,
            UiImage.GUARD_DRAG
    };
    private static final int[] ROLE_FILTERS = {0, 1, 3};

    static void render(UI ui, VoteButtonPanel vp, VoteInfoRenderer vi,
                       JPanel panel, List<Integer> selectedTargets,
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
        UiImage frameIcon = FRAME_ICONS[typeIdx];
        UiImage resultAllIcon = RESULT_ALL_ICONS[typeIdx];
        UiImage dragAllIcon = DRAG_ALL_ICONS[typeIdx];
        int claimedRoleFilter = ROLE_FILTERS[typeIdx];

        List<Integer> roleNums = new ArrayList<>();
        List<Integer> roleOrders = new ArrayList<>();
        for (int i = 1; i <= n - 1; i++) {
            if (ui.ctx.getClaimedRole(i) == claimedRoleFilter && ui.ctx.isAlive(i)) {
                roleNums.add(i);
                roleOrders.add(ui.ctx.getClaimedRoleOrder(i));
            }
        }

        List<JLabel> targetLabels = new ArrayList<>();
        List<JLabel> frameLabels = new ArrayList<>();
        List<JLabel> resultLabels = new ArrayList<>();
        List<JLabel> zbLabels = new ArrayList<>();

        for (int i = 1; i <= n - 1; i++) {
            String imageName = handler.charImageName(i);

            if (ui.ctx.getClaimedRole(i) > 0 && ui.ctx.getClaimedRole(i) < 6) {
                String crName = ImageNames.claimedRoleIcon(
                        ui.ctx.getClaimedRole(i), ui.ctx.getClaimedRoleOrder(i));
                JLabel crLabel = new JLabel(ui.resources.getImage(crName));
                crLabel.setBounds(handler.charGridX(i, 60, 74), handler.charGridY(i, 20, 128),
                        crLabel.getIcon().getIconWidth(), crLabel.getIcon().getIconHeight());
                panel.add(crLabel);
            }

            JLabel chooseLabel = new JLabel(ui.resources.getImage(frameIcon));
            frameLabels.add(chooseLabel);
            chooseLabel.setBounds(handler.charGridX(i, 60, 74), handler.charGridY(i, 20, 128), 64, 98);
            panel.add(chooseLabel);
            chooseLabel.setVisible(selectedTargets.contains(i));

            if (type == SelectionType.VOTE) {
                JLabel voteLabel = new JLabel(ui.resources.getImage(UiImage.VOTE_RESULT_ALL));
                voteLabel.setBounds(handler.charGridX(i, 65, 74), handler.charGridY(i, 20, 128),
                        voteLabel.getIcon().getIconWidth(), voteLabel.getIcon().getIconHeight());
                panel.add(voteLabel);
                panel.setComponentZOrder(voteLabel, 0);
                voteLabel.setVisible(ui.ctx.isSelectedVoteTarget(i, ui.ctx.getGameDay()));
            }

            JLabel resultLabel = new JLabel(ui.resources.getImage(resultAllIcon));
            resultLabels.add(resultLabel);
            resultLabel.setBounds(handler.charGridX(i, 65, 74), handler.charGridY(i, 40, 148),
                    resultLabel.getIcon().getIconWidth(), resultLabel.getIcon().getIconHeight());
            panel.add(resultLabel);
            resultLabel.setVisible(selectedTargets.contains(i));

            if (type != SelectionType.VOTE) {
                for (int r = 0; r < roleNums.size(); r++) {
                    JLabel zbLabel = new JLabel(ui.resources.getImage(
                            ImageNames.selectionResultIcon(type, roleOrders.get(r))));
                    zbLabels.add(zbLabel);
                    zbLabel.setBounds(handler.charGridX(i, 65 + zbLabel.getIcon().getIconWidth(), 74),
                            handler.charGridY(i, 20 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r),
                                    128 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r)),
                            zbLabel.getIcon().getIconWidth(), zbLabel.getIcon().getIconHeight());
                    panel.add(zbLabel);
                    zbLabel.setVisible(selectedTargets.contains(i));
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

        panel.setBounds(UIConstants.INFO_PANEL_X, UIConstants.INFO_PANEL_Y,
                200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight());
        panel.add(LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight(), vi.boardIcon));

        ImageIcon dragIcon = ui.resources.getImage(dragAllIcon);
        JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 250, 350,
                dragIcon.getIconWidth() / 2, dragIcon.getIconHeight() / 2, dragIcon);
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
                    ui.resources.playSound(SoundEffect.CLICK);
                    ui.gameApplication.setTargetSelection(
                            new SetAllTargetSelectionCommand(type, idx + 1, true));
                    if (!selectedTargets.contains(idx + 1)) selectedTargets.add(idx + 1);
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
            for (int r = 0; r < roleNums.size(); r++) {
                final int cur = r;
                ImageIcon roleDragIcon = ui.resources.getImage(
                        ImageNames.claimedRoleActionIcon(type, roleOrders.get(r)));
                JButton perBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                        roleOrders.get(r) * (type == SelectionType.DIVINATION ? 100 : 150)
                                + (type == SelectionType.DIVINATION ? 150 : 250), 350,
                        roleDragIcon.getIconWidth() / 2,
                        roleDragIcon.getIconHeight() / 2,
                        roleDragIcon);
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
                            ui.resources.playSound(SoundEffect.CLICK);
                            ui.gameApplication.setTargetSelection(
                                    new SetClaimedRoleTargetSelectionCommand(
                                            type, roleNums.get(cur), idx + 1, true));
                            if (!selectedTargets.contains(idx + 1)) selectedTargets.add(idx + 1);
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
                ui.resources.getImage(UiImage.DELETE).getIconWidth() / 2,
                ui.resources.getImage(UiImage.DELETE).getIconHeight() / 2,
                ui.resources.getImage(UiImage.DELETE));
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
                    ui.resources.playSound(SoundEffect.CLICK);
                    ui.gameApplication.setTargetSelection(
                            new SetAllTargetSelectionCommand(type, idx + 1, false));
                    selectedTargets.remove(Integer.valueOf(idx + 1));
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
