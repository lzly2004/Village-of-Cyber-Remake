package com.vocr.config;

/**
 * 游戏全局常量 — 第1步重构核心。
 * 集中管理所有跨文件使用的魔法数字，消除硬编码。
 *
 * 只保存对局规则和规则策略所需常量；Swing 展示常量位于 ui.UIConstants。
 */
public final class GameConstants
{
    private GameConstants() {} // 工具类不可实例化

    public static final int MAX_GAME_DAYS    = 50;
    public static final int MAX_VOTE_ROUNDS  = 3;
    public static final int CHARACTER_SUM    = 44;

    // ==================== 游戏逻辑常量 ====================
    public static final int INF  = 999;
    public static final int INFJ = 500;
    public static final int MAXN = 100;
    /** 概率计算器默认概率值 */
    public static final int DEFAULT_PROBABILITY = 20;
    /** 怀疑前三数组大小(3+1) */
    public static final int TOP3_ARRAY_SIZE = 4;

    // ==================== 怀疑度变化常量 ====================
    /** 占卜师CO时怀疑度减少 */
    public static final int SUSPICION_DECREASE_SEER_CO = -50;
    /** 灵能者CO时怀疑度减少 */
    public static final int SUSPICION_DECREASE_MEDIUM_CO = -30;
    /** 3日及以后CO时额外增加的怀疑度 */
    public static final int SUSPICION_INCREASE_LATE_CO = 10;
    /** 白球占文目标怀疑度减少 */
    public static final int SUSPICION_DECREASE_WHITE_BALL_TARGET = -10;
    /** 黑球占文目标怀疑度增加 */
    public static final int SUSPICION_INCREASE_BLACK_BALL_TARGET = 10;
    /** 出黒球时占卜师自身轻微怀疑度增加 */
    public static final int SUSPICION_INCREASE_BLACK_BALL_CASTER = 2;
    /** 占灵冲突/矛盾时怀疑度增加 */
    public static final int SUSPICION_INCREASE_CONFLICT = 5;
    /** 接黒回复时怀疑度增加 */
    public static final int SUSPICION_INCREASE_BLACK_RESPONSE = 10;

    // ==================== AI 概率参数 ====================
    /** 假猎重选概率 (%) */
    public static final int PROB_FAKE_HUNTER_REROLL = 10;
    /** 假猎白球权重 */
    public static final int WEIGHT_FAKE_HUNTER_WHITE = 50;
    /** 假猎黑球权重 */
    public static final int WEIGHT_FAKE_HUNTER_BLACK = 50;
    /** 共有暴露决策概率 (%) */
    public static final int PROB_SHARED_EXPOSURE_DECISION = 50;

    // ==================== 灵能捏造概率常量 ====================
    /** 狼/狂信灵能捏造-狼阵营默认一致概率 (%) */
    public static final int PROB_LING_WOLF_FACTION_DEFAULT = 90;
    /** 狼/狂信灵能捏造-狂信者一致概率 (%) */
    public static final int PROB_LING_FANATIC_SPECIAL = 95;

    /** 信赖占卜师结果一致概率-人狼 (%) */
    public static final int PROB_LING_TRUSTED_SEER_WOLF = 95;
    /** 信赖占卜师结果一致概率-狂人 (%) */
    public static final int PROB_LING_TRUSTED_SEER_MADMAN = 90;
    /** 信赖占卜师结果一致概率-狂信者 (%) */
    public static final int PROB_LING_TRUSTED_SEER_FANATIC = 99;
    /** 信赖占卜师结果一致概率-妖狐 (%) */
    public static final int PROB_LING_TRUSTED_SEER_FOX = 95;
    /** 信赖占卜师结果一致概率-背德者 (%) */
    public static final int PROB_LING_TRUSTED_SEER_DEVIANT = 80;

    /** 怀疑占卜师结果一致概率-人狼 (%) */
    public static final int PROB_LING_SUSPICIOUS_SEER_WOLF = 85;
    /** 怀疑占卜师结果一致概率-狂人 (%) */
    public static final int PROB_LING_SUSPICIOUS_SEER_MADMAN = 90;
    /** 怀疑占卜师结果一致概率-狂信者 (%) */
    public static final int PROB_LING_SUSPICIOUS_SEER_FANATIC = 95;
    /** 怀疑占卜师结果一致概率-妖狐 (%) */
    public static final int PROB_LING_SUSPICIOUS_SEER_FOX = 90;
    /** 怀疑占卜师结果一致概率-背德者 (%) */
    public static final int PROB_LING_SUSPICIOUS_SEER_DEVIANT = 80;
}
