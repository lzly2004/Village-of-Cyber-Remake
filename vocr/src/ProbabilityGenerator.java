import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProbabilityGenerator {
    public static void main(String[] args) {
        try {
            // 使用Jackson生成JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // 构建JSON结构
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("description", "猫猎co概率配置文件");
            root.put("format", "zhi, situation, p1, p2 -> probability");
            root.put("zhi_meanings", new LinkedHashMap<String, String>() {{
                put("0", "人狼");
                put("1", "狂人");
                put("2", "狂信");
                put("3", "妖狐");
                put("4", "背德");
            }});
            root.put("situation_meanings", new LinkedHashMap<String, String>() {{
                put("0", "被指定");
                put("1", "接黒");
                put("2", "被询问co");
                put("3", "询问职业");
                put("4", "共有全死co猫");
                put("5", "猫村双死co猫");
                put("6", "平和co猎(废弃)");
            }});
            root.put("p1_meanings", new LinkedHashMap<String, String>() {{
                put("0", "完全怂狼");
                put("1", "单狼上职");
                put("2", "双狼上职");
                put("3", "三狼上职");
            }});
            root.put("p2_meanings", new LinkedHashMap<String, String>() {{
                put("0", "孤狼剩余");
                put("1", "双狼剩余");
                put("2", "三狼剩余");
                put("3", "四狼俱在");
            }});

            // 生成概率条目
            List<Map<String, Integer>> probabilities = new ArrayList<>();
            int totalCount = 0;
            int minProb = 100, maxProb = 0;
            int[] probDistribution = new int[101];

            for (int zhi = 0; zhi <= 4; zhi++) {
                for (int situation = 0; situation <= 6; situation++) {
                    for (int p1 = 0; p1 <= 3; p1++) {
                        for (int p2 = 0; p2 <= 3; p2++) {
                            int probability = calculateProbability(zhi, situation, p1, p2);

                            Map<String, Integer> entry = new LinkedHashMap<>();
                            entry.put("zhi", zhi);
                            entry.put("situation", situation);
                            entry.put("p1", p1);
                            entry.put("p2", p2);
                            entry.put("probability", probability);
                            probabilities.add(entry);

                            totalCount++;
                            probDistribution[probability]++;
                            if (probability < minProb) minProb = probability;
                            if (probability > maxProb) maxProb = probability;
                        }
                    }
                }
            }

            root.put("probabilities", probabilities);

            // 写入JSON文件
            File outputFile = new File(PathConfig.PROBABILITY_CONFIG);
            mapper.writeValue(outputFile, root);

            // 打印统计信息
            DebugLogger.log("配置文件已生成: " + PathConfig.PROBABILITY_CONFIG);
            DebugLogger.log("总条目数: " + totalCount + " (应为560)");
            DebugLogger.log("最小概率: " + minProb + "%");
            DebugLogger.log("最大概率: " + maxProb + "%");

            int sum = 0;
            for (int i = 0; i <= 100; i++) {
                sum += i * probDistribution[i];
            }
            double average = (double) sum / totalCount;
            DebugLogger.log("平均概率: " + String.format("%.2f", average) + "%");

            DebugLogger.log("\n概率分布:");
            for (int i = 0; i <= 100; i += 5) {
                int count = 0;
                for (int j = i; j < i + 5 && j <= 100; j++) {
                    count += probDistribution[j];
                }
                if (count > 0) {
                    DebugLogger.printf("%2d-%2d: %3d 个 (%.1f%%)\n",
                            i, Math.min(i+4, 100), count, (double)count/totalCount*100);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 核心概率计算公式
     */
    private static int calculateProbability(int zhi, int situation, int p1, int p2) {
        // 平和co猎(情景6)已废弃，概率为0
        if (situation == 6) {
            return 0;
        }

        int probability = 0;

        // 1. 职业基础值
        switch(zhi) {
            case 0: probability = 18; break;
            case 1: probability = 14; break;
            case 2: probability = 30; break;
            case 3: probability = 8; break;
            case 4: probability = 16; break;
        }

        // 2. 情境修正
        switch(situation) {
            case 0: probability += 25; break;
            case 1: probability += 10; break;
            case 2: probability += 5; break;
            case 3: probability += 3; break;
            case 4: probability += 15; break;
            case 5: probability += 17; break;
        }

        // 3. p1修正
        probability -= p1 * 2;

        // 4. p2修正
        probability += p2 * 1;

        // 5. 特殊交互调整
        if (zhi == 3 && (situation == 1 || situation == 2)) {
            probability += 6;
        }
        if (zhi == 4 && (situation == 1 || situation == 0)) {
            probability += 8;
        }
        if (zhi == 0 && p2 == 0) {
            probability -= 5;
        }
        if (zhi == 2 && p2 == 0) {
            probability += 3;
        }
        if (situation == 4) {
            if (zhi == 3 || zhi == 4) {
                probability += 4;
            } else if (zhi == 1) {
                probability += 3;
            }
        }
        if (situation == 5) {
            if ((zhi == 0 || zhi == 2) && p1 >= 2) {
                probability += 6;
            }
        }

        // 最终概率范围限制
        if (probability < 2) probability = 2;
        if (probability > 70) probability = 70;

        return probability;
    }
}