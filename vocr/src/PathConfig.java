// PathConfig.java
// 集中管理所有文件路径配置

public final class PathConfig {

    // ==================== 数据目录 ====================
    public static final String DATA_DIR = "data/";
    public static final String CONFIG_DIR = "config/";
    public static final String BASELINE_DIR = "baseline/";
    public static final String BASELINE_NEW_DIR = "baseline_new/";

    // ==================== 数据文件 ====================
    public static final String GAME_RECORD = DATA_DIR + "game_record.json";
    public static final String REPLAY_SAVES = DATA_DIR + "replay_saves.json";

    // ==================== 配置文件 ====================
    public static final String PROBABILITY_CONFIG = CONFIG_DIR + "probability.json";

    // ==================== 辅助方法 ====================

    public static String baselineFile(String label) {
        return BASELINE_DIR + label + ".txt";
    }

    public static String baselineNewFile(String label) {
        return BASELINE_NEW_DIR + label + ".txt";
    }

    private PathConfig() {}
}
