package com.vocr.ui;

import com.vocr.resources.BgmTrack;
import com.vocr.resources.ImageNames;
import com.vocr.resources.SoundEffect;
import com.vocr.resources.UiImage;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.whyDie;

import java.awt.Image;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;

public class UIHelpers {

    /** 按指定尺寸平滑缩放 Swing 图标。 */
    public static ImageIcon scaleIcon(ImageIcon originalIcon, int width, int height) {
        if (originalIcon == null) return null;
        if (originalIcon.getIconWidth() == width && originalIcon.getIconHeight() == height)
            return originalIcon;
        Image scaledImage = originalIcon.getImage().getScaledInstance(
                width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    public static void setButtonListener(JButton button, UI ui, Runnable action) {
        button.addActionListener(e -> {
            ui.resources.playSound(SoundEffect.CLICK);
            action.run();
        });
    }

    public static void setButtonSceneTransitionNoSound(JButton button, UI ui, UI.Scene scene) {
        button.addActionListener(e -> ui.transitionTo(scene));
    }

    public static void finishRender(UI ui) {
        ui.resizeComponents();
        ui.jPanel.revalidate();
        ui.jPanel.repaint();
        ui.getJFrame().setVisible(true);
    }

    /** 创建窗口全屏背景标签，若图片不存在返回null */
    public static JLabel createBackgroundLabel(UI ui, UiImage image) {
        ImageIcon img = ui.resources.getImage(image);
        if (img == null) return null;
        return LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT, img);
    }

    /** 创建并添加窗口全屏背景到ui.jPanel，若图片不存在则不添加 */
    public static void addBackground(UI ui, UiImage image) {
        JLabel bg = createBackgroundLabel(ui, image);
        if (bg != null) ui.jPanel.add(bg);
    }

    /** 批量隐藏按钮 */
    public static void hideButtons(JButton... buttons) {
        for (JButton b : buttons) b.setVisible(false);
    }

    private record CharacterLabelResult(JLabel label, boolean addToLinkIcon) {}

    /** 对话角色立绘定位：特定双人事件放在左侧，并缓存供下一角色配对。 */
    private static CharacterLabelResult createCharacterLabel(Event event, ImageIcon icon) {
        boolean addToLinkIcon = switch (event.eventname()) {
            case gyfo1, qfjc5, zjgh8b, zjgb8, gprz11p, zcrh12 -> true;
            default -> false;
        };
        int x = addToLinkIcon ? 300 : (UIConstants.WINDOW_WIDTH - icon.getIconWidth()) / 2;
        JLabel label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, x,
                UIConstants.WINDOW_HEIGHT - icon.getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                icon.getIconWidth(), icon.getIconHeight(), icon);
        return new CharacterLabelResult(label, addToLinkIcon);
    }

    /** 连接事件角色对显示：当前事件角色(右)+linkIcon中缓存的上一事件角色(左) */
    public static void renderLinkIconPair(UI ui, ImageIcon[] CharIcon) {
        JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 650,
                UIConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
        ui.diaPanel.add(Chara);
        ui.resizeComponents();
        JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 300,
                UIConstants.WINDOW_HEIGHT - ui.linkIcon.get(0).getIconHeight() - UIConstants.CHAR_ICON_BOTTOM_MARGIN,
                ui.linkIcon.get(0).getIconWidth(), ui.linkIcon.get(0).getIconHeight(),
                ui.linkIcon.get(0));
        ui.diaPanel.add(Chara2);
        ui.diaPanel.setComponentZOrder(Chara2, 1);
        ui.linkIcon.remove(0);
    }

    /** 白天阶段BGM选择：存活人数≤3时切换紧张BGM */
    public static void playDayPhaseBgm(UI ui) {
        if ((ui.ctx.getAliveCounter() - 1) / 2 == 1) {
            ui.resources.playBgm(BgmTrack.LIGHTLESS_GAZE);
        } else {
            ui.resources.playBgm(BgmTrack.EMOTIONALLY_UNSTABLE);
        }
    }

    /** 打字机效果：逐字显示文本，nextBtn点击时跳到末尾或触发回调 */
    public static Timer bindTypewriter(JTextArea target, String fullText,
                                       JButton nextBtn, Runnable onComplete) {
        final int[] index = {0};
        Timer timer = new Timer(UIConstants.TYPEWRITER_DELAY_MS, e -> {
            if (index[0] < fullText.length()) {
                target.append(String.valueOf(fullText.charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
        for (ActionListener al : nextBtn.getActionListeners())
            nextBtn.removeActionListener(al);
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText.length()) {
                target.setText(fullText);
                index[0] = fullText.length();
                timer.stop();
            } else {
                onComplete.run();
            }
        });
        return timer;
    }

    public record DialogueSetup(DialogueBox.Components dc, String text, ImageIcon[] charIcon) {}

    public static DialogueSetup prepareDialogueEvent(UI ui, UiImage bgImage, Event event) {
        DialogueBox.Components dc = DialogueBox.setup(ui, bgImage);
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1()));
        dc.dialogPanel.add(nameLabel);
        return new DialogueSetup(dc, ui.resources.getEventText(event), ui.resources.getEventImage(event));
    }

    public static void renderDialogueCharacter(UI ui, Event event, ImageIcon[] CharIcon) {
        CharacterLabelResult result = createCharacterLabel(event, CharIcon[0]);
        if (result.addToLinkIcon()) ui.linkIcon.add(CharIcon[0]);
        ui.diaPanel.add(result.label());
        ui.resizeComponents();
    }

    public static void configureScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
    }

    public static int calculateRowIndex(int playerIndex, int playerSum) {
        return playerIndex <= (playerSum + 1) / 2 ? playerIndex : (playerIndex - (playerSum + 1) / 2);
    }

    public static boolean isFirstRow(int playerIndex, int playerSum) {
        return playerIndex <= (playerSum + 1) / 2;
    }

    public static JLabel createClaimedRoleIcon(UI ui, int claimedRole, int claimedRoleOrder, int x, int y) {
        if (claimedRole <= 0 || claimedRole >= 6) return null;
        String iconName = ImageNames.claimedRoleIcon(claimedRole, claimedRoleOrder);
        ImageIcon icon = ui.resources.getImage(iconName);
        return createIconLabel(icon, x, y);
    }

    public static JLabel createDeathMarker(UI ui, whyDie deathReason, int x, int y) {
        String iconName = ImageNames.deathMarker(deathReason);
        if (iconName.isEmpty()) return null;
        ImageIcon icon = ui.resources.getImage(iconName);
        return createIconLabel(icon, x, y);
    }

    public static JLabel createDeathMarker(UI ui, int deathReason, int x, int y) {
        return createDeathMarker(ui, whyDie.values()[deathReason], x, y);
    }

    public static JLabel createPlayerAvatar(UI ui, int charNumber, whyDie deathReason,
            int actualRole, boolean showActualRole, int x, int y) {
        String imageName = showActualRole
                ? ImageNames.characterResultPortrait(charNumber, deathReason, actualRole)
                : ImageNames.characterStatusPortrait(charNumber, deathReason == whyDie.NONE);
        ImageIcon icon = ui.resources.getImage(imageName);
        return createIconLabel(icon, x, y);
    }

    public static JLabel createCharacterText(UI ui, int charNumber, int x, int baseY, boolean isTextLabel) {
        String textName = ImageNames.characterText(charNumber);
        ImageIcon icon = ui.resources.getImage(textName);
        if (icon == null) return null;
        int textWidth = icon.getIconWidth();
        int textHeight = icon.getIconHeight();
        return LabelSimpleFactory.makeLabel(isTextLabel ? LabelConst.Text_Label : LabelConst.Simple_Label, x,
                baseY - textHeight / 2, textWidth / 2, textHeight / 2, icon);
    }

    public static JLabel createIconLabel(UI ui, String imageName, int x, int y) {
        return createIconLabel(ui.resources.getImage(imageName), x, y);
    }

    public static JLabel createIconLabel(UI ui, UiImage image, int x, int y) {
        return createIconLabel(ui.resources.getImage(image), x, y);
    }

    private static JLabel createIconLabel(ImageIcon icon, int x, int y) {
        if (icon == null) return null;
        JLabel label = new JLabel(icon);
        label.setBounds(x, y, icon.getIconWidth(), icon.getIconHeight());
        return label;
    }

    public static void appendSkillResultLog(UI ui, StringBuilder sb, int player, int startDay) {
        appendSkillResultLog(ui, sb, player, startDay, true);
    }

    public static void appendSkillResultLog(UI ui, StringBuilder sb, int player, int startDay,
                                            boolean showBallResult) {
        sb.append(ui.getJobText(player)).append(" : ");
        for (int day = startDay; day < ui.ctx.getGameDay(); ++day) {
            if (ui.ctx.getDeathDay(player) != 0 && day >= ui.ctx.getDeathDay(player)) break;
            int target = ui.ctx.getSkillTarget(player, day);
            if (target > 0) {
                boolean isBlack = target > ui.ctx.getPlayerSum();
                if (isBlack) target -= ui.ctx.getPlayerSum();
                sb.append(ui.getJobText(target));
                if (showBallResult) sb.append(isBlack ? "●" : "○");
                sb.append("→");
            }
        }
        if (ui.ctx.isNonHumanMarked(player)) {
            sb.append(GameStrings.MARKER_EXPOSED).append("→");
        }
        sb.setLength(sb.length() - 1);
        sb.append("\n");
    }
}
