import java.io.FileWriter;
import java.io.PrintWriter;

public class ProbabilityGenerator {
    public static void main(String[] args) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("config/probability.txt"));

            writer.println("# 猫猎co概率配置文件");
            writer.println("# 格式: zhi situation p1 p2 probability");
            writer.println("# zhi: 0人狼 1狂人 2狂信 3妖狐 4背德");
            writer.println("# situation: 0被指定 1接黒 2被询问co 3询问职业 4共有全死co猫 5猫村双死co猫 6平和co猎(废弃)");
            writer.println("# p1: 0完全怂狼 1单狼上职 2双狼上职 3三狼上职");
            writer.println("# p2: 0孤狼剩余 1双狼剩余 2三狼剩余 3四狼俱在");
            writer.println();

            int totalCount = 0;
            int minProb = 100, maxProb = 0;
            int[] probDistribution = new int[101]; // 记录概率分布

            // 遍历所有560种组合
            for (int zhi = 0; zhi <= 4; zhi++) {
                for (int situation = 0; situation <= 6; situation++) {
                    for (int p1 = 0; p1 <= 3; p1++) {
                        for (int p2 = 0; p2 <= 3; p2++) {
                            int probability = calculateProbability(zhi, situation, p1, p2);
                            writer.printf("%d %d %d %d %d%n", zhi, situation, p1, p2, probability);

                            // 统计信息
                            totalCount++;
                            probDistribution[probability]++;
                            if (probability < minProb) minProb = probability;
                            if (probability > maxProb) maxProb = probability;
                        }
                    }
                }
            }

            writer.close();

            // 打印统计信息
            DebugLogger.log("配置文件已生成: config/probability.txt");
            DebugLogger.log("总条目数: " + totalCount + " (应为560)");
            DebugLogger.log("最小概率: " + minProb + "%");
            DebugLogger.log("最大概率: " + maxProb + "%");

            // 计算平均概率
            int sum = 0;
            for (int i = 0; i <= 100; i++) {
                sum += i * probDistribution[i];
            }
            double average = (double) sum / totalCount;
            DebugLogger.log("平均概率: " + String.format("%.2f", average) + "%");

            // 打印概率分布
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
     * 目标：降低整体概率，提高狂信倾向
     */
    private static int calculateProbability(int zhi, int situation, int p1, int p2) {
        // 平和co猎(情景6)已废弃，概率为0
        if (situation == 6) {
            return 0;
        }

        int probability = 0;

        // 1. 职业基础值（降低整体，提高狂信）
        switch(zhi) {
            case 0: // 人狼
                probability = 18;  // 降低4
                break;
            case 1: // 狂人
                probability = 14;  // 降低4
                break;
            case 2: // 狂信 - 改为高倾向
                probability = 30;  // 提高2
                break;
            case 3: // 妖狐
                probability = 8;   // 降低4
                break;
            case 4: // 背德
                probability = 16;  // 降低4
                break;
        }

        // 2. 情境修正（降低幅度）
        switch(situation) {
            case 0: // 被指定处刑
                probability += 25;  // 降低10
                break;
            case 1: // 接黒
                probability += 10;  // 降低5
                break;
            case 2: // 被询问co
                probability += 5;   // 降低3
                break;
            case 3: // 询问职业
                probability += 3;   // 降低2
                break;
            case 4: // 共有全死co猫
                probability += 15;  // 降低5
                break;
            case 5: // 猫村双死co猫
                probability += 17;  // 降低5
                break;
        }

        // 3. p1修正（进一步降低修正幅度）
        probability -= p1 * 2;  // 降低1

        // 4. p2修正（进一步降低修正幅度）
        probability += p2 * 1;  // 降低1

        // 5. 特殊交互调整（降低调整幅度）

        // 妖狐在接黒/被询问时更可能跳猫猎自保
        if (zhi == 3 && (situation == 1 || situation == 2)) {
            probability += 6;  // 降低2
        }

        // 背德在接黒/被指定时更可能跳猫猎保护妖狐
        if (zhi == 4 && (situation == 1 || situation == 0)) {
            probability += 8;  // 降低2
        }

        // 人狼在游戏前期更保守
        if (zhi == 0 && p2 == 0) {
            probability -= 5;  // 降低3
        }

        // 狂信知道狼队友，会积极跳猫猎保护狼队
        if (zhi == 2 && p2 == 0) {
            probability += 3;  // 降低1
        }

        // 共有全死时，所有非人跳猫倾向略微增加
        if (situation == 4) {
            if (zhi == 3 || zhi == 4) {  // 妖狐/背德
                probability += 4;  // 降低2
            } else if (zhi == 1) {  // 狂人
                probability += 3;  // 降低1
            }
        }

        // 猫村双死时，非人占卜师的支持者更可能跳猫
        if (situation == 5) {
            // 如果是人狼或狂信，而且狼占多（p1值大），则更可能跳猫支撑假占
            if ((zhi == 0 || zhi == 2) && p1 >= 2) {
                probability += 6;  // 降低2
            }
        }

        // 最终概率范围限制
        if (probability < 2) probability = 2;    // 最低2%
        if (probability > 70) probability = 70;  // 最高70%（进一步限制）

        return probability;
    }
}