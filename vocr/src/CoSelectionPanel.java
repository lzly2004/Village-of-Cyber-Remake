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
                       JPanel infoCoPanel, List<Integer> askList, boolean[] isCo,
                       GameSceneVoteHandler handler) {
        isCo[0] = true;
        vp.reiBtn.setVisible(false); vp.kariBtn.setVisible(false); vp.uranaiBtn.setVisible(false);
        vp.kyouyuBtn.setVisible(false); vp.catBtn.setVisible(false); vp.askCoBtn.setVisible(false);
        infoCoPanel.setVisible(true);
        ui.resources.playSound("click.wav");
        vi.infoText.setVisible(false);
        vp.fixedVoteBtn.setVisible(false); vp.fixedUranaiBtn.setVisible(false); vp.protectBtn.setVisible(false);
        vp.returnBtn.setVisible(true);
        infoCoPanel.removeAll();

        List<Integer> zhanbuNum = new ArrayList<>();
        List<Integer> zhanbuOrder = new ArrayList<>();
        for (int i = 1; i <= ui.ctx.getPlayerSum(); i++) {
            if (ui.ctx.getClaimedRole(i) == 1 && ui.ctx.isAlive(i)) {
                zhanbuNum.add(i);
                zhanbuOrder.add(ui.ctx.getClaimedRoleOrder(i));
            }
        }
        List<JLabel> targetLabels = new ArrayList<>();
        List<JLabel> frameLabels = new ArrayList<>();
        List<JLabel> resultLabels = new ArrayList<>();
        List<JLabel> zbLabels = new ArrayList<>();
        for (int i = 1; i <= ui.ctx.getPlayerSum(); i++) {
            ImageIcon characterImage = ui.resources.getImage(handler.charImageName(i));
            if (ui.ctx.getClaimedRole(i) > 0 && ui.ctx.getClaimedRole(i) < 6) {
                ImageIcon claimedRoleIcon = ui.resources.getImage(handler.claimedRoleIconName(i));
                JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                    claimedRoleLabel.setBounds(60 + 74 * i, 20, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                else
                    claimedRoleLabel.setBounds(60 + 74 * (i - ((ui.ctx.getPlayerSum() + 1) / 2)), 128,
                            claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                infoCoPanel.add(claimedRoleLabel);
            }
            ImageIcon chooseIcon = ui.resources.getImage("frameSRed.png");
            JLabel chooseLabel = new JLabel(chooseIcon);
            frameLabels.add(chooseLabel);
            if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                chooseLabel.setBounds(60 + 74 * i, 20, chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
            else
                chooseLabel.setBounds(60 + 74 * (i - ((ui.ctx.getPlayerSum() + 1) / 2)), 128,
                        chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
            infoCoPanel.add(chooseLabel);
            chooseLabel.setVisible(false);

            ImageIcon voteIcon = ui.resources.getImage("result2_all.png");
            JLabel voteLabel = new JLabel(voteIcon);
            if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                voteLabel.setBounds(60 + 5 + 74 * i, 20, voteIcon.getIconWidth(), voteIcon.getIconHeight());
            else
                voteLabel.setBounds(60 + 5 + 74 * (i - (ui.ctx.getPlayerSum() + 1) / 2), 128,
                        voteIcon.getIconWidth(), voteIcon.getIconHeight());
            infoCoPanel.add(voteLabel);
            infoCoPanel.setComponentZOrder(voteLabel, 0);
            voteLabel.setVisible(false);
            if (ui.ctx.isSelectedVoteTarget(i, ui.ctx.getGameDay())) voteLabel.setVisible(true);

            ImageIcon voteAllIcon = ui.resources.getImage("result1_all.png");
            JLabel voteAllLabel = new JLabel(voteAllIcon);
            if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                voteAllLabel.setBounds(60 + 5 + 74 * i, 40, voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
            else
                voteAllLabel.setBounds(60 + 5 + 74 * (i - (ui.ctx.getPlayerSum() + 1) / 2), 148,
                        voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
            infoCoPanel.add(voteAllLabel);
            resultLabels.add(voteAllLabel);
            voteAllLabel.setVisible(false);

            if (i < zhanbuNum.size() + 1) {
                for (int i2 = 1; i2 <= ui.ctx.getPlayerSum(); ++i2) {
                    ImageIcon zbIcon = ui.resources.getImage("result1_" + zhanbuOrder.get(i - 1) + "white.png");
                    JLabel zbLabel = new JLabel(zbIcon);
                    zbLabels.add(zbLabel);
                    if (i2 <= (ui.ctx.getPlayerSum() + 1) / 2)
                        zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * i2,
                                20 + zbIcon.getIconHeight() * zhanbuOrder.get(i - 1),
                                zbIcon.getIconWidth(), zbIcon.getIconHeight());
                    else
                        zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - (ui.ctx.getPlayerSum() + 1) / 2),
                                128 + zbIcon.getIconHeight() * zhanbuOrder.get(i - 1),
                                zbIcon.getIconWidth(), zbIcon.getIconHeight());
                    infoCoPanel.add(zbLabel);
                    zbLabel.setVisible(false);
                }
            }
            JLabel label = new JLabel(characterImage);
            targetLabels.add(label);
            if (i <= (ui.ctx.getPlayerSum() + 1) / 2)
                label.setBounds(60 + (characterImage.getIconWidth() + 10) * i, 20,
                        characterImage.getIconWidth(), characterImage.getIconHeight());
            else
                label.setBounds((60 + (characterImage.getIconWidth() + 10) * (i - (ui.ctx.getPlayerSum() + 1) / 2)),
                        30 + characterImage.getIconHeight(), characterImage.getIconWidth(), characterImage.getIconHeight());
            infoCoPanel.add(label);
        }
        handler.renderSkillTargets(infoCoPanel, 50, 74, 20, 128);
        infoCoPanel.setBounds(0, 198, 200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight());

        JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                200 + vi.boardIcon.getIconWidth(), 50 + vi.boardIcon.getIconHeight(), vi.boardIcon);

        ImageIcon dragIcon = ui.resources.getImage("uranaiAll.png");
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
                        if (!ui.ctx.isAlive(index + 1)) break;
                        ui.resources.playSound("click.wav");
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

        dragIcon = ui.resources.getImage("delete.png");
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
                        if (!ui.ctx.isAlive(index + 1)) break;
                        ui.resources.playSound("click.wav");
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