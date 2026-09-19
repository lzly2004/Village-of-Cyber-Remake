package com.vocr.ui;

import com.vocr.application.ClaimInquiryOptions;
import com.vocr.application.ClaimedRoleActor;
import com.vocr.application.GameUpdate;
import com.vocr.application.LegalTargetSelection;
import com.vocr.resources.SoundEffect;
import com.vocr.resources.ImageNames;
import com.vocr.resources.UiImage;

import com.vocr.domain.SelectionType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CO询问面板 —— 从 GameSceneVoteHandler.wireAllEventHandlers() 提取。
 * 负责渲染拖拽式CO询问界面（占卜师CO对象选择）。
 */
class CoSelectionPanel {

    static void render(UI ui, VoteButtonPanel vp, VoteInfoRenderer vi,
                       JPanel infoCoPanel, List<Integer> askList,
                       GameSceneVoteHandler handler) {
        vp.reiBtn.setVisible(false); vp.kariBtn.setVisible(false); vp.uranaiBtn.setVisible(false);
        vp.kyouyuBtn.setVisible(false); vp.catBtn.setVisible(false); vp.askCoBtn.setVisible(false);
        infoCoPanel.setVisible(true);
        ui.resources.playSound(SoundEffect.CLICK);
        vi.infoText.setVisible(false);
        vp.fixedVoteBtn.setVisible(false); vp.fixedUranaiBtn.setVisible(false); vp.protectBtn.setVisible(false);
        vp.returnBtn.setVisible(true);
        infoCoPanel.removeAll();

        GameUpdate update = ui.currentGameUpdate();
        ClaimInquiryOptions claimOptions = update.claimInquiry();
        List<Integer> claimCandidates = claimOptions.players();
        LegalTargetSelection divination = update
                .legalTargetSelection(SelectionType.DIVINATION)
                .orElseThrow(() -> new IllegalStateException("当前没有占卜目标预约动作"));
        List<Integer> zhanbuNum = new ArrayList<>();
        List<Integer> zhanbuOrder = new ArrayList<>();
        for (ClaimedRoleActor actor : divination.claimedActors()) {
            zhanbuNum.add(actor.playerNumber());
            zhanbuOrder.add(actor.claimedRoleOrder());
        }
        List<JLabel> targetLabels = new ArrayList<>();
        List<JLabel> frameLabels = new ArrayList<>();
        List<JLabel> resultLabels = new ArrayList<>();
        List<JLabel> zbLabels = new ArrayList<>();
        int playerSum = ui.ctx.getPlayerSum();
        for (int i = 1; i <= playerSum; i++) {
            ImageIcon characterImage = ui.resources.getImage(ImageNames.characterStatusPortrait(
                    ui.ctx.getCharacterNumber(i), ui.ctx.isAlive(i)));
            int charIndex = UIHelpers.calculateRowIndex(i, playerSum);
            boolean isFirstRow = UIHelpers.isFirstRow(i, playerSum);
            int baseY = isFirstRow ? 20 : 128;

            if (ui.ctx.getClaimedRole(i) > 0 && ui.ctx.getClaimedRole(i) < 6) {
                JLabel claimedRoleLabel = UIHelpers.createClaimedRoleIcon(ui, ui.ctx.getClaimedRole(i),
                        ui.ctx.getClaimedRoleOrder(i), 60 + 74 * charIndex, baseY);
                if (claimedRoleLabel != null) infoCoPanel.add(claimedRoleLabel);
            }
            JLabel chooseLabel = UIHelpers.createIconLabel(
                    ui, UiImage.SELECTION_FRAME_DIVINATION, 60 + 74 * charIndex, baseY);
            frameLabels.add(chooseLabel);
            infoCoPanel.add(chooseLabel);
            chooseLabel.setVisible(false);

            JLabel voteLabel = UIHelpers.createIconLabel(
                    ui, UiImage.VOTE_RESULT_ALL, 60 + 5 + 74 * charIndex, baseY);
            infoCoPanel.add(voteLabel);
            infoCoPanel.setComponentZOrder(voteLabel, 0);
            voteLabel.setVisible(false);
            if (ui.ctx.isSelectedVoteTarget(i, ui.ctx.getGameDay())) voteLabel.setVisible(true);

            JLabel voteAllLabel = UIHelpers.createIconLabel(
                    ui, UiImage.DIVINATION_RESULT_ALL, 60 + 5 + 74 * charIndex, baseY + 20);
            infoCoPanel.add(voteAllLabel);
            resultLabels.add(voteAllLabel);
            voteAllLabel.setVisible(false);

            if (i < zhanbuNum.size() + 1) {
                for (int i2 = 1; i2 <= playerSum; ++i2) {
                    JLabel zbLabel = UIHelpers.createIconLabel(
                            ui, ImageNames.skillResultIcon(1, zhanbuOrder.get(i - 1), false),
                            60 + 5 + 74 * UIHelpers.calculateRowIndex(i2, playerSum),
                            (UIHelpers.isFirstRow(i2, playerSum) ? 20 : 128) + 16 * zhanbuOrder.get(i - 1));
                    zbLabels.add(zbLabel);
                    infoCoPanel.add(zbLabel);
                    zbLabel.setVisible(false);
                }
            }
            JLabel label = new JLabel(characterImage);
            targetLabels.add(label);
            int charWidth = characterImage.getIconWidth();
            int charHeight = characterImage.getIconHeight();
            label.setBounds(60 + (charWidth + 10) * charIndex, baseY + 10,
                    charWidth, charHeight);
            infoCoPanel.add(label);
        }
        handler.renderSkillTargets(infoCoPanel, 50, 74, 20, 128);
        infoCoPanel.setBounds(0, 198, 200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight());

        JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight(), vi.boardIcon);

        ImageIcon dragIcon = ui.resources.getImage(UiImage.DIVINATION_DRAG);
        JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 250, 350,
                dragIcon.getIconWidth() / 2, dragIcon.getIconHeight() / 2, dragIcon);
        dragBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Container parent = dragBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                Rectangle btnRect = dragBtn.getBounds();
                int btnCenterX = btnRect.x + btnRect.width / 2;
                int btnCenterY = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    Rectangle labelRect = label.getBounds();
                    if (labelRect.contains(btnCenterX, btnCenterY)) {
                        int index = targetLabels.indexOf(label);
                        if (!claimCandidates.contains(index + 1)) break;
                        ui.resources.playSound(SoundEffect.CLICK);
                        if (!askList.contains(index + 1)) askList.add(index + 1);
                        resultLabels.get(index).setVisible(true);
                        frameLabels.get(index).setVisible(true);
                        frameLabels.get(index).repaint();
                        ui.jPanel.repaint(label.getBounds());
                        break;
                    }
                    dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        });
        infoCoPanel.add(dragBtn);

        dragIcon = ui.resources.getImage(UiImage.DELETE);
        JButton dragBtn_delete = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 800, 350,
                dragIcon.getIconWidth() / 2, dragIcon.getIconHeight() / 2, dragIcon);
        dragBtn_delete.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Container parent = dragBtn_delete.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                Rectangle btnRect = dragBtn_delete.getBounds();
                int btnCenterX = btnRect.x + btnRect.width / 2;
                int btnCenterY = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    Rectangle labelRect = label.getBounds();
                    if (labelRect.contains(btnCenterX, btnCenterY)) {
                        int index = targetLabels.indexOf(label);
                        if (!claimCandidates.contains(index + 1)) break;
                        ui.resources.playSound(SoundEffect.CLICK);
                        if (askList.contains(index + 1)) {
                            int uu = askList.indexOf(index + 1);
                            askList.remove(uu);
                        }
                        for (int u = 0; u < zhanbuNum.size(); ++u)
                            zbLabels.get(index + u * ui.ctx.getPlayerSum()).setVisible(false);
                        resultLabels.get(index).setVisible(false);
                        frameLabels.get(index).setVisible(false);
                        frameLabels.get(index).repaint();
                        ui.jPanel.repaint(label.getBounds());
                        break;
                    }
                    dragBtn_delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        });
        infoCoPanel.add(dragBtn_delete);
        infoCoPanel.add(infoBoard);
        ui.jPanel.add(infoCoPanel);
        ui.jPanel.setComponentZOrder(infoCoPanel, 0);
        ui.resizeComponents();
    }
}
