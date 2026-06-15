// ProbabilityCalculator.java - 简化的概率计算类
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 简化的概率计算器类
 * 使用纯文本格式存储560种组合的概率
 */
public class ProbabilityCalculator {
    // 参数取值范围定义（保持不变）
    private static final int ZHI_MIN = 0;
    private static final int ZHI_MAX = 4;
    private static final int SITUATION_MIN = 0;
    private static final int SITUATION_MAX = 6;
    private static final int P1_MIN = 0;
    private static final int P1_MAX = 3;
    private static final int P2_MIN = 0;
    private static final int P2_MAX = 3;

    // 使用Map存储概率，键为组合的字符串表示
    private Map<String, Integer> probabilityMap;

    // 默认概率值
    private static final int DEFAULT_PROBABILITY = 20;

    // 配置文件路径
    private final String configFilePath;

    /**
     * 构造函数
     */
    public ProbabilityCalculator(String configFilePath) {
        this.configFilePath = configFilePath;
        this.probabilityMap = new HashMap<>();
        loadFromConfig();
    }

    /**
     * 无参构造函数，使用默认值
     */
    public ProbabilityCalculator() {
        this.configFilePath = null;
        this.probabilityMap = new HashMap<>();
        // 初始化所有组合为默认值
        initWithDefaultValues();
    }

    /**
     * 主函数：根据四个参数获取概率
     */
    public int maolieco(int zhi, int situation, int p1, int p2) {
        // 参数校验
        if (!isValid(zhi, situation, p1, p2)) {
            throw new IllegalArgumentException(String.format(
                    "参数超出范围: zhi=%d[%d-%d], situation=%d[%d-%d], p1=%d[%d-%d], p2=%d[%d-%d]",
                    zhi, ZHI_MIN, ZHI_MAX,
                    situation, SITUATION_MIN, SITUATION_MAX,
                    p1, P1_MIN, P1_MAX,
                    p2, P2_MIN, P2_MAX
            ));
        }

        // 生成键
        String key = String.format("%d %d %d %d", zhi, situation, p1, p2);

        // 从Map中获取概率，如果不存在则返回默认值
        return probabilityMap.getOrDefault(key, DEFAULT_PROBABILITY);
    }

    /**
     * 加载配置文件
     */
    private void loadFromConfig() {
        if (configFilePath == null) {
            DebugLogger.log("未指定配置文件，使用默认概率值" + DEFAULT_PROBABILITY);
            initWithDefaultValues();
            return;
        }

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            DebugLogger.log("配置文件不存在，生成默认配置文件并加载");
            saveDefaultConfig(configFilePath);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            int loadedCount = 0;
            int errorCount = 0;

            while ((line = reader.readLine()) != null) {
                // 跳过空行和注释行
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 按空格或逗号分割
                String[] parts = line.split("[ ,]+");
                if (parts.length < 5) {
                    errorCount++;
                    continue;
                }

                try {
                    int zhi = Integer.parseInt(parts[0]);
                    int situation = Integer.parseInt(parts[1]);
                    int p1 = Integer.parseInt(parts[2]);
                    int p2 = Integer.parseInt(parts[3]);
                    int probability = Integer.parseInt(parts[4]);

                    // 验证范围
                    if (isValid(zhi, situation, p1, p2)) {
                        String key = String.format("%d %d %d %d", zhi, situation, p1, p2);
                        probabilityMap.put(key, probability);
                        loadedCount++;
                    } else {
                        errorCount++;
                        DebugLogger.error(String.format("参数超出范围: %s", line));
                    }
                } catch (NumberFormatException e) {
                    errorCount++;
                    DebugLogger.error("解析失败: " + line);
                }
            }

            DebugLogger.log(String.format("概率配置加载完成，成功加载 %d 条规则，失败 %d 条", loadedCount, errorCount));

            // 如果加载的规则太少，用默认值填充
            if (loadedCount < 100) { // 假设至少应该有100条有效规则
                DebugLogger.log("有效规则太少，用默认值填充缺失的规则");
                initWithDefaultValues();
            }

        } catch (IOException e) {
            DebugLogger.error("加载配置文件失败: " + e.getMessage());
            initWithDefaultValues();
        }
    }

    /**
     * 初始化所有组合为默认值
     */
    private void initWithDefaultValues() {
        probabilityMap.clear();
        // 生成所有560种组合并设置为默认值
        for (int zhi = ZHI_MIN; zhi <= ZHI_MAX; zhi++) {
            for (int situation = SITUATION_MIN; situation <= SITUATION_MAX; situation++) {
                for (int p1 = P1_MIN; p1 <= P1_MAX; p1++) {
                    for (int p2 = P2_MIN; p2 <= P2_MAX; p2++) {
                        String key = String.format("%d %d %d %d", zhi, situation, p1, p2);
                        probabilityMap.put(key, DEFAULT_PROBABILITY);
                    }
                }
            }
        }
    }

    /**
     * 生成默认配置文件
     */
    public void saveDefaultConfig(String outputPath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("# 概率配置文件");
            writer.println("# 格式: zhi situation p1 p2 probability");
            writer.println("# zhi: 0人狼 1狂人 2狂信 3妖狐 4背德");
            writer.println("# situation: 0被指定 1接黒 2被询问co 3询问职业 4共有全死co猫 5猫村双死co猫 6平和co猎");
            writer.println("# p1: 0完全怂狼 1单狼上职 2双狼上职 3三狼上职");
            writer.println("# p2: 0孤狼剩余 1双狼剩余 2三狼剩余 3四狼俱在");
            writer.println();

            for (int zhi = ZHI_MIN; zhi <= ZHI_MAX; zhi++) {
                for (int situation = SITUATION_MIN; situation <= SITUATION_MAX; situation++) {
                    for (int p1 = P1_MIN; p1 <= P1_MAX; p1++) {
                        for (int p2 = P2_MIN; p2 <= P2_MAX; p2++) {
                            writer.printf("%d %d %d %d %d%n",
                                    zhi, situation, p1, p2, DEFAULT_PROBABILITY);
                        }
                    }
                }
            }

            DebugLogger.log("默认配置文件已生成: " + outputPath);
            DebugLogger.log("总配置项数: " +
                    ((ZHI_MAX - ZHI_MIN + 1) *
                            (SITUATION_MAX - SITUATION_MIN + 1) *
                            (P1_MAX - P1_MIN + 1) *
                            (P2_MAX - P2_MIN + 1)));

        } catch (IOException e) {
            DebugLogger.error("保存配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 参数有效性检查
     */
    private boolean isValid(int zhi, int situation, int p1, int p2) {
        return zhi >= ZHI_MIN && zhi <= ZHI_MAX &&
                situation >= SITUATION_MIN && situation <= SITUATION_MAX &&
                p1 >= P1_MIN && p1 <= P1_MAX &&
                p2 >= P2_MIN && p2 <= P2_MAX;
    }

    /**
     * 获取统计信息
     */
    public void printStatistics() {
        if (probabilityMap.isEmpty()) {
            DebugLogger.log("概率表为空");
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;

        for (Integer value : probabilityMap.values()) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        double average = (double) sum / probabilityMap.size();

        DebugLogger.log("概率配置统计信息:");
        DebugLogger.log("  组合总数: " + probabilityMap.size());
        DebugLogger.log("  最小值: " + min);
        DebugLogger.log("  最大值: " + max);
        DebugLogger.log("  平均值: " + String.format("%.2f", average));
    }
}