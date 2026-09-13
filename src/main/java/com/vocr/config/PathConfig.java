package com.vocr.config;

// PathConfig.java
// 集中管理所有文件路径配置

public final class PathConfig {

    // ==================== 数据目录 ====================
    public static final String DATA_DIR = "data/";
    public static final String CONFIG_DIR = "config/";

    // ==================== 数据文件 ====================
    public static final String GAME_RECORD = DATA_DIR + "game_record.json";
    public static final String REPLAY_SAVES = DATA_DIR + "replay_saves.json";

    // ==================== 配置文件 ====================
    public static final String PROBABILITY_CONFIG = CONFIG_DIR + "probability.json";

    private PathConfig() {}
}
