import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.*;

public class UIHelpers {

    /** 批量隐藏按钮 */
    public static void hideButtons(JButton... buttons) {
        for (JButton b : buttons) b.setVisible(false);
    }

    /** 对话角色立绘定位：根据事件类型决定左侧/居中，并标记是否需要加入linkIcon */
    public static JLabel createCharacterLabel(Event event, ImageIcon[] CharIcon,
                                              boolean[] shouldAddToLinkIcon) {
        JLabel Chara;
        switch (event.eventname) {
            case gyfo1:
            case qfjc5:
            case zjgh8b:
            case zjgb8:
            case gprz11p:
            case zcrh12:
                Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 300,
                        GameConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                shouldAddToLinkIcon[0] = true;
                break;
            default:
                Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        (GameConstants.WINDOW_WIDTH - CharIcon[0].getIconWidth()) / 2,
                        GameConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
                shouldAddToLinkIcon[0] = false;
                break;
        }
        return Chara;
    }

    /** 连接事件角色对显示：当前事件角色(右)+linkIcon中缓存的上一事件角色(左) */
    public static void renderLinkIconPair(UI ui, ImageIcon[] CharIcon) {
        JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 650,
                GameConstants.WINDOW_HEIGHT - CharIcon[0].getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(), CharIcon[0]);
        ui.diaPanel.add(Chara);
        ui.resizeComponents();
        JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 300,
                GameConstants.WINDOW_HEIGHT - ui.linkIcon.get(0).getIconHeight() - GameConstants.CHAR_ICON_BOTTOM_MARGIN,
                ui.linkIcon.get(0).getIconWidth(), ui.linkIcon.get(0).getIconHeight(),
                ui.linkIcon.get(0));
        ui.diaPanel.add(Chara2);
        ui.diaPanel.setComponentZOrder(Chara2, 1);
        ui.linkIcon.remove(0);
    }

    /** 白天阶段BGM选择：存活人数≤3时切换紧张BGM */
    public static void playDayPhaseBgm(UI ui) {
        if ((ui.ctx.getAliveCounter() - 1) / 2 == 1) {
            ui.resources.playBgm("西江紫堂 - 灯り無き眼光.wav");
        } else {
            ui.resources.playBgm("Emotionally Unstable.wav");
        }
    }

    /** 打字机效果：逐字显示文本，nextBtn点击时跳到末尾或触发回调 */
    public static Timer bindTypewriter(JTextArea target, String fullText,
                                       JButton nextBtn, Runnable onComplete) {
        final int[] index = {0};
        Timer timer = new Timer(GameConstants.TYPEWRITER_DELAY_MS, e -> {
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

    public static DialogueSetup prepareDialogueEvent(UI ui, String bgImage, Event event) {
        DialogueBox.Components dc = DialogueBox.setup(ui, bgImage);
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label, 40, 10, 1000, 30,
                ui.uiComponentFactory.getCharacterFullName(event.ch1));
        dc.dialogPanel.add(nameLabel);
        return new DialogueSetup(dc, ui.resources.getEventText(event), ui.resources.getEventImage(event));
    }

    public static void renderDialogueCharacter(UI ui, Event event, ImageIcon[] CharIcon) {
        boolean[] shouldAdd = {false};
        JLabel Chara = createCharacterLabel(event, CharIcon, shouldAdd);
        if (shouldAdd[0]) ui.linkIcon.add(CharIcon[0]);
        ui.diaPanel.add(Chara);
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
        String iconName = ui.uiComponentFactory.getClaimedRoleIconName(claimedRole, claimedRoleOrder);
        ImageIcon icon = ui.resources.getImage(iconName);
        if (icon == null) return null;
        JLabel label = new JLabel(icon);
        label.setBounds(x, y, icon.getIconWidth(), icon.getIconHeight());
        return label;
    }

    public static JLabel createDeathMarker(UI ui, whyDie deathReason, int x, int y) {
        String iconName = deathReason.getDeathIconName();
        if (iconName.isEmpty()) return null;
        ImageIcon icon = ui.resources.getImage(iconName);
        if (icon == null) return null;
        JLabel label = new JLabel(icon);
        label.setBounds(x, y, icon.getIconWidth(), icon.getIconHeight());
        return label;
    }

    public static JLabel createDeathMarker(UI ui, int deathReason, int x, int y) {
        return createDeathMarker(ui, whyDie.values()[deathReason], x, y);
    }

    public static JLabel createPlayerAvatar(UI ui, int charNumber, whyDie deathReason,
            int actualRole, boolean showActualRole, int x, int y) {
        String imageName = showActualRole
                ? GameStrings.buildCharacterImageName(charNumber, deathReason, actualRole, true)
                : GameStrings.buildCharacterImageNameSimple(charNumber, deathReason);
        ImageIcon icon = ui.resources.getImage(imageName);
        if (icon == null) return null;
        JLabel label = new JLabel(icon);
        label.setBounds(x, y, icon.getIconWidth(), icon.getIconHeight());
        return label;
    }

    public static JLabel createCharacterText(UI ui, int charNumber, int x, int baseY, boolean isTextLabel) {
        String textName = GameStrings.buildCharacterTextName(charNumber);
        ImageIcon icon = ui.resources.getImage(textName);
        if (icon == null) return null;
        int textWidth = icon.getIconWidth();
        int textHeight = icon.getIconHeight();
        return LabelSimpleFactory.makeLabel(isTextLabel ? LabelConst.Text_Label : LabelConst.Simple_Label, x,
                baseY - textHeight / 2, textWidth / 2, textHeight / 2, icon);
    }
}