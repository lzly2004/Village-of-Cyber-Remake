package com.vocr.config;

import com.vocr.DebugLogger;

// ProbabilityCalculator.java - 简化的概率计算类
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 简化的概率计算器类
 * 使用JSON格式存储560种组合的概率
 */
public class ProbabilityCalculator {
    // 参数取值范围定义
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
        initWithDefaultValues();
    }

    /**
     * 主函数：根据四个参数获取概率
     */
    public int maolieco(int zhi, int situation, int p1, int p2) {
        if (!isValid(zhi, situation, p1, p2)) {
            throw new IllegalArgumentException(String.format(
                    "参数超出范围: zhi=%d[%d-%d], situation=%d[%d-%d], p1=%d[%d-%d], p2=%d[%d-%d]",
                    zhi, ZHI_MIN, ZHI_MAX,
                    situation, SITUATION_MIN, SITUATION_MAX,
                    p1, P1_MIN, P1_MAX,
                    p2, P2_MIN, P2_MAX
            ));
        }

        String key = String.format("%d %d %d %d", zhi, situation, p1, p2);
        return probabilityMap.getOrDefault(key, GameConstants.DEFAULT_PROBABILITY);
    }

    /**
     * 加载配置文件（JSON格式）
     */
    private void loadFromConfig() {
        // 始终先建立完整默认表；文件只覆盖合法条目，任何缺失/损坏都不会留下空表。
        initWithDefaultValues();
        if (configFilePath == null) {
            DebugLogger.log("未指定配置文件，使用默认概率值" + GameConstants.DEFAULT_PROBABILITY);
            return;
        }

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            DebugLogger.log("配置文件不存在，生成默认配置文件并加载");
            saveDefaultConfig(configFilePath);
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(configFile);

            JsonNode probabilities = root.get("probabilities");
            if (probabilities == null || !probabilities.isArray()) {
                DebugLogger.error("JSON格式错误：缺少probabilities数组");
                return;
            }

            Map<String, Integer> loadedValues = new HashMap<>();
            for (JsonNode entry : probabilities) {
                if (!entry.has("zhi") || !entry.has("situation") || !entry.has("p1")
                        || !entry.has("p2") || !entry.has("probability")) {
                    continue;
                }
                int zhi = entry.get("zhi").asInt();
                int situation = entry.get("situation").asInt();
                int p1 = entry.get("p1").asInt();
                int p2 = entry.get("p2").asInt();
                int probability = entry.get("probability").asInt();

                if (isValid(zhi, situation, p1, p2) && probability >= 0 && probability <= 100) {
                    String key = String.format("%d %d %d %d", zhi, situation, p1, p2);
                    loadedValues.put(key, probability);
                }
            }

            probabilityMap.putAll(loadedValues);

            DebugLogger.log(String.format("概率配置加载完成，成功加载 %d 条规则", loadedValues.size()));

            if (loadedValues.size() < getRuleCount()) {
                DebugLogger.log("配置项不完整，缺失规则已使用默认概率补齐");
            }

        } catch (IOException e) {
            DebugLogger.error("加载配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 初始化所有组合为默认值
     */
    private void initWithDefaultValues() {
        probabilityMap.clear();
        for (int zhi = ZHI_MIN; zhi <= ZHI_MAX; zhi++) {
            for (int situation = SITUATION_MIN; situation <= SITUATION_MAX; situation++) {
                for (int p1 = P1_MIN; p1 <= P1_MAX; p1++) {
                    for (int p2 = P2_MIN; p2 <= P2_MAX; p2++) {
                        String key = String.format("%d %d %d %d", zhi, situation, p1, p2);
                        probabilityMap.put(key, GameConstants.DEFAULT_PROBABILITY);
                    }
                }
            }
        }
    }

    /**
     * 生成默认配置文件（JSON格式）
     */
    public void saveDefaultConfig(String outputPath) {
        File outputFile = new File(outputPath);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            DebugLogger.error("无法创建概率配置目录: " + parent);
            return;
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("{");
            writer.println("  \"description\": \"猫猎co概率配置文件\",");
            writer.println("  \"probabilities\": [");

            int count = 0;
            int total = (ZHI_MAX - ZHI_MIN + 1) * (SITUATION_MAX - SITUATION_MIN + 1)
                    * (P1_MAX - P1_MIN + 1) * (P2_MAX - P2_MIN + 1);

            for (int zhi = ZHI_MIN; zhi <= ZHI_MAX; zhi++) {
                for (int situation = SITUATION_MIN; situation <= SITUATION_MAX; situation++) {
                    for (int p1 = P1_MIN; p1 <= P1_MAX; p1++) {
                        for (int p2 = P2_MIN; p2 <= P2_MAX; p2++) {
                            writer.printf("    {\"zhi\":%d,\"situation\":%d,\"p1\":%d,\"p2\":%d,\"probability\":%d}",
                                    zhi, situation, p1, p2, GameConstants.DEFAULT_PROBABILITY);
                            count++;
                            if (count < total) writer.println(",");
                            else writer.println();
                        }
                    }
                }
            }

            writer.println("  ]");
            writer.println("}");

            DebugLogger.log("默认配置文件已生成: " + outputPath);
            DebugLogger.log("总配置项数: " + total);

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

    /** 当前内存中可用的合法组合数量。 */
    int getRuleCount() {
        return probabilityMap.size();
    }
}
