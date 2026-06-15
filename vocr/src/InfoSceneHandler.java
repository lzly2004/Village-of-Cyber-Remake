// InfoSceneHandler.java - 信息界面场景处理器
import javax.swing.*;
import java.awt.*;

public class InfoSceneHandler implements SceneHandler {
    @Override
    public void render(UI ui) {
        UI.Scene scene = ui.currentScene;
        if (scene == UI.Scene.INFO_SCENE) {
            renderMain(ui);
        } else if (isFirstLevel(scene)) {
            renderFirst(ui, scene);
        } else {
            renderSecond(ui, scene);
        }
    }

    private boolean isFirstLevel(UI.Scene scene) {
        int ord = scene.ordinal();
        return ord >= UI.Scene.INFO_SCENE_1.ordinal() && ord <= UI.Scene.INFO_SCENE_5.ordinal();
    }

    private void renderMain(UI ui) {
        ui.jPanel.removeAll();
        ImageIcon btnNext = ui.resources.getImage("avg_button2.png");
        JButton btn_next[] = new JButton[6];
        for (int i = 1; i <= 5; i++) {
            final int j = i;
            btn_next[i] = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 80, 10 + 70 * i, 220, 50,
                    ui.resources.getHelpText("Info" + i + ".txt"), btnNext);
            btn_next[i].setHorizontalTextPosition(SwingConstants.CENTER);
            btn_next[i].addActionListener(e -> {
                ui.resources.playSound("click.wav");
                ui.currentScene = UI.Scene.valueOf("INFO_SCENE_" + j);
                ui.run();
            });
            ui.jPanel.add(btn_next[i]);
        }
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("PVBG.png"));
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 10, 10, 980, 660,
                ui.resources.getImage("avg1_resized(3).png"));
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 560,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT,
                ui.resources.getImage("PVBtitile.png"));
        btnMenu.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = UI.Scene.START_SCENE;
            ui.run();
        });
        ui.jPanel.add(btnMenu);
        ui.jPanel.add(backgroundLabel_2);
        ui.jPanel.add(backgroundLabel);
        ui.jFrame.setVisible(true);
        ui.resizeComponents();
    }

    private void renderSecond(UI ui, UI.Scene scene) {
        ui.jPanel.removeAll();
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("PVBG.png"));
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 10, 10, 980, 660,
                ui.resources.getImage("avg1_resized(3).png"));
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 560,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT,
                ui.resources.getImage("PVBtitile.png"));
        btnMenu.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = UI.Scene.START_SCENE;
            ui.run();
        });
        JButton btnBack = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 400,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT,
                ui.resources.getImage("PVBreturn.png"));
        btnBack.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = scene.FatherScene(scene);
            ui.run();
        });
        JTextArea dialogText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE, 24,
                ui.resources.getHelpText(scene.toString()), true);
        JScrollPane scrollPane = new JScrollPane(dialogText);
        scrollPane.setBounds(50, 50, 880, 630);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        ui.jPanel.add(btnMenu);
        ui.jPanel.add(btnBack);
        ui.jPanel.add(scrollPane);
        ui.jPanel.add(backgroundLabel_2);
        ui.jPanel.add(backgroundLabel);
        ui.jFrame.setVisible(true);
        ui.resizeComponents();
    }

    private void renderFirst(UI ui, UI.Scene scene) {
        ui.jPanel.removeAll();
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT,
                ui.resources.getImage("PVBG.png"));
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 10, 10, 980, 660,
                ui.resources.getImage("avg1_resized(3).png"));
        ImageIcon menu = ui.resources.getImage("PVBtitile.png");
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 560,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT, menu);
        btnMenu.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = UI.Scene.START_SCENE;
            ui.run();
        });
        ImageIcon back = ui.resources.getImage("PVBreturn.png");
        JButton btnBack = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button, 1050, 400,
                GameConstants.RETURN_WIDTH, GameConstants.RETURN_HEIGHT, back);
        btnBack.addActionListener(e -> {
            ui.resources.playSound("click.wav");
            ui.currentScene = UI.Scene.INFO_SCENE;
            ui.run();
        });
        int subSum = scene.SubInfoSum(scene);
        ImageIcon btnNext[] = new ImageIcon[subSum + 1];
        JButton btn_next[] = new JButton[subSum + 1];
        for (int i = 1; i <= subSum; i++) {
            final int currentIndex = i;
            btnNext[i] = ui.resources.getImage("avg_button2.png");
            btn_next[i] = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,
                    80, 70 * i + 10, 222, 50,
                    ui.resources.getHelpText("Info" + scene.FirstInfoNum(scene) + "-" + i + ".txt"), btnNext[i]);
            btn_next[i].setHorizontalTextPosition(SwingConstants.CENTER);
            btn_next[i].addActionListener(e -> {
                ui.resources.playSound("click.wav");
                ui.currentScene = UI.Scene.values()[scene.ordinal() + currentIndex];
                ui.run();
            });
            ui.jPanel.add(btn_next[i]);
        }
        ui.jPanel.add(btnMenu);
        ui.jPanel.add(btnBack);
        ui.jPanel.add(backgroundLabel_2);
        ui.jPanel.add(backgroundLabel);
        ui.jFrame.setVisible(true);
        ui.resizeComponents();
    }
}
