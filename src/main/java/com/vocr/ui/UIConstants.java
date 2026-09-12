package com.vocr.ui;

import java.awt.Color;

/**
 * Swing 展示层常量。
 *
 * <p>这里只保存窗口、布局、字体、颜色与动画时序；对局规则常量继续由
 * {@code com.vocr.config.GameConstants} 管理。</p>
 */
final class UIConstants
{
    private UIConstants() {}

    static final String FONT_FAMILY = "Takao Mincho";
    static final int FONT_SIZE_BUTTON = 20;
    static final int FONT_SIZE_VOTE = 24;
    static final int FONT_SIZE_DIALOG = 26;
    static final int FONT_SIZE_TITLE = 50;

    static final int TYPEWRITER_DELAY_MS = 50;
    static final int TRANSITION_SHORT_MS = 1000;
    static final int TRANSITION_MEDIUM_MS = 2000;
    static final int HOWL_TRANSITION_MS = 2500;
    static final int ENDING_DISPLAY_MS = 3000;
    static final int NIGHT_SCREEN_DURATION_MS = 7000;

    static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);
    static final Color COLOR_TRANSLUCENT_BLACK = new Color(0, 0, 0, 180);

    static final String WINDOW_TITLE = "Village of Cyber:Remake v1.0.3.3";
    static final int WINDOW_WIDTH = 1280;
    static final int WINDOW_HEIGHT = 720;

    static final int CHAR_ICON_BOTTOM_MARGIN = 30;

    static final int INFO_PANEL_X = 0;
    static final int INFO_PANEL_Y = 198;

    static final int START_BTN_X = 660;
    static final int START_BTN_Y = 400;
    static final int START_BTN_W = 309;
    static final int START_BTN_H = 68;
    static final int START_BTN_X_GAP = 300;
    static final int START_BTN_Y_GAP = 80;

    static final int RETURN_WIDTH = 194;
    static final int RETURN_HEIGHT = 127;
}
