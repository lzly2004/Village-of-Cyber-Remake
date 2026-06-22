import java.awt.*;

/**
 * 游戏全局常量 — 第1步重构核心。
 * 集中管理所有跨文件使用的魔法数字，消除硬编码。
 *
 * 包含:
 *   - 字体名称与字号
 *   - 计时器延迟
 *   - 颜色常量
 *   - UI布局尺寸常量
 */
public final class GameConstants
{
    private GameConstants() {} // 工具类不可实例化

    // ==================== 字体 ====================
    /** 游戏中唯一使用的日文字体 */
    public static final String FONT_FAMILY = "Takao Mincho";

    // 字号
    public static final int FONT_SIZE_BUTTON  = 20;  // 按钮文字
    public static final int FONT_SIZE_VOTE    = 24;  // 票型/怀疑度/指定信息
    public static final int FONT_SIZE_DIALOG  = 26;  // 对话框正文
    public static final int FONT_SIZE_TITLE   = 50;  // 标题/黑色标签

    // ==================== 计时器延迟(毫秒) ====================
    /** 打字效果逐字间隔 */
    public static final int TYPEWRITER_DELAY_MS     = 50;
    /** 提示气泡显示时长 */
    public static final int HINT_DISPLAY_MS         = 1000;
    /** 立绘切换/处刑过渡 */
    public static final int TRANSITION_SHORT_MS     = 1000;
    /** 处刑后等待/死亡立绘显示 */
    public static final int TRANSITION_MEDIUM_MS    = 2000;
    /** 狼嚎音效后进入对话 */
    public static final int HOWL_TRANSITION_MS      = 2500;
    /** 结束动画显示 */
    public static final int ENDING_DISPLAY_MS       = 3000;
    /** 入夜界面停留 */
    public static final int NIGHT_SCREEN_DURATION_MS = 7000;

    // ==================== 颜色 ====================
    /** 完全透明 */
    public static final Color COLOR_TRANSPARENT      = new Color(0, 0, 0, 0);
    /** 半透明黑色遮罩(提示弹窗用) */
    public static final Color COLOR_TRANSLUCENT_BLACK = new Color(0, 0, 0, 180);

    // ==================== 窗口尺寸 ====================
    /** 游戏窗口标题 */
    public static final String WINDOW_TITLE = "Village of Cyber:Remake v1.0.3.1";
    /** 窗口宽度 */
    public static final int WINDOW_WIDTH  = 1280;
    /** 窗口高度 */
    public static final int WINDOW_HEIGHT = 720;

    // ==================== 对话框 ====================
    /** 对话框面板X偏移 */
    public static final int DIALOG_X      = 260;
    /** 对话框面板Y偏移 */
    public static final int DIALOG_Y      = 450;
    /** 对话框宽度 */
    public static final int DIALOG_WIDTH  = 760;
    /** 对话框高度 */
    public static final int DIALOG_HEIGHT = 230;
    /** 对话框内文本X偏移 */
    public static final int DIALOG_TEXT_X = 20;
    /** 对话框内文本Y偏移 */
    public static final int DIALOG_TEXT_Y = 50;
    /** 对话框内文本宽度 */
    public static final int DIALOG_TEXT_W = 710;
    /** 对话框内文本高度 */
    public static final int DIALOG_TEXT_H = 200;

    // ==================== 投票界面右侧按钮区 ====================
    /** 按钮区X基准 */
    public static final int VOTE_BTN_X       = 1060;
    /** 按钮宽度 */
    public static final int VOTE_BTN_W       = 194;
    /** 按钮高度 */
    public static final int VOTE_BTN_H       = 126;
    /** 按钮间距 */
    public static final int VOTE_BTN_GAP     = 10;
    /** 按钮底边距 */
    public static final int VOTE_BTN_BOTTOM  = 40;
    /** 第二列按钮X偏移 */
    public static final int VOTE_BTN_X2      = 1060 - 194 - 30;

    // ==================== 角色头像网格 ====================
    /** 头像网格X起点 */
    public static final int CHAR_GRID_X      = 60;
    /** 头像Y(上行) */
    public static final int CHAR_GRID_Y1     = 20;
    /** 头像Y(下行) */
    public static final int CHAR_GRID_Y2     = 128;
    /** 头像间距 */
    public static final int CHAR_GRID_SPACING = 74;
    /** 头像尺寸(宽) */
    public static final int CHAR_ICON_W      = 64;
    /** 头像尺寸(高) */
    public static final int CHAR_ICON_H      = 98;
    /** 角色立绘底部边距 */
    public static final int CHAR_ICON_BOTTOM_MARGIN = 30;

    // ==================== 信息面板 ====================
    /** 信息面板X */
    public static final int INFO_PANEL_X     = 0;
    /** 信息面板Y */
    public static final int INFO_PANEL_Y     = 198;

    // ==================== 开始界面按钮布局 ====================
    /** 开始界面按钮基准X */
    public static final int START_BTN_X      = 660;
    /** 开始界面按钮基准Y */
    public static final int START_BTN_Y      = 400;
    /** 开始界面按钮宽度 */
    public static final int START_BTN_W      = 309;
    /** 开始界面按钮高度 */
    public static final int START_BTN_H      = 68;
    /** 开始界面按钮列间距 */
    public static final int START_BTN_X_GAP  = 300;
    /** 开始界面按钮行间距 */
    public static final int START_BTN_Y_GAP  = 80;

    // ==================== 关卡选择界面 ====================
    /** 关卡按钮X */
    public static final int LEVEL_BTN_X      = 30;
    /** 关卡按钮Y */
    public static final int LEVEL_BTN_Y      = 20;
    /** 关卡按钮宽度 */
    public static final int LEVEL_BTN_W      = 435;
    /** 关卡按钮高度 */
    public static final int LEVEL_BTN_H      = 138;

    public static final int RETURN_WIDTH     = 194;
    public static final int RETURN_HEIGHT    = 127;
    public static final int MAX_GAME_DAYS    = 50;
    public static final int CHARACTER_SUM    = 44;

    // ==================== 游戏逻辑常量 ====================
    public static final int INF  = 999;
    public static final int INFJ = 500;
    public static final int MAXN = 100;
    /** 概率计算器默认概率值 */
    public static final int DEFAULT_PROBABILITY = 20;
    /** 怀疑前三数组大小(3+1) */
    public static final int TOP3_ARRAY_SIZE = 4;
}