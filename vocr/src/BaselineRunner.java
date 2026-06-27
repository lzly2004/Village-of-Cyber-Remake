/**
 * 回归测试基线运行器 — 第0步验证工具。
 *
 * 使用固定随机种子运行所有7种配役的游戏初始化阶段,
 * 录制完整的游戏事件和随机数序列到 baseline/ 目录,
 * 作为后续重构的"黄金标准"对照基线。
 *
 * 运行方式:
 *   java -cp "out/production/vocr;bin/*" BaselineRunner
 *
 * 验证方式:
 *   重构后重新运行此脚本,使用 diff 对比 baseline/ 目录下的新旧输出:
 *     diff baseline/peiyi_1_seed_42.txt baseline_new/peiyi_1_seed_42.txt
 *   若输出完全一致(除时间戳行外),则重构未引入功能变化。
 *   同时校验随机数序列: 比较checksum行,确保size和sum一致。
 */
public class BaselineRunner
{
    public static void main(String[] args)
    {
        // 7种配役 + 对应固定种子
        long[] seeds = {42, 123, 456, 789, 101, 202, 303};

        DebugLogger.getInstance().startFileLogging("baseline_new/full_output.log");

        for (int p = 1; p <= 7; p++)
        {
            peiyi peiyiValue = peiyi.values()[p];
            long seed = seeds[p - 1];
            String label = "peiyi_" + p + "_" + peiyiValue.name() + "_seed_" + seed;

            DebugLogger.info("========== 开始基线录制: " + label + " ==========");

            // 固定随机种子
            ConstNum.setRandomSeed(seed);

            // 开始录制随机数序列
            ConstNum.startRandomRecording();

            // 初始化游戏
            Game game = Game.getInstance();
            game.init();

            // 创建录像器 (需要GameRecordManager)
            GameRecorder recorder = new GameRecorder(label, game.getGameRecordManager());

            MainLogic mainLogic = (MainLogic) game.getMainLogic();
            GameStatus gs = mainLogic.start(peiyiValue);

            // 开始录制
            recorder.startGame(peiyiValue, seed, gs.getPlayerSum(), gs);

            // 录制初始事件
            java.util.LinkedList<Event> events = game.getUI().getEvents();
            int initialEventCount = events.size();
            DebugLogger.info("初始事件数: " + initialEventCount);

            for (int i = 0; i < initialEventCount; i++)
            {
                Event evt = events.poll();
                if (evt != null)
                {
                    recorder.recordGameEvent(1, evt);
                }
            }

            // 录制角色初始状态
            recorder.recordStateSnapshot(1, gs);

            // 停止录制随机数序列并保存校验和
            ConstNum.stopRandomRecording();
            String checksum = ConstNum.getRandomChecksum();
            recorder.recordEvent("RANDOM_CHECKSUM", checksum);
            DebugLogger.info("随机数序列校验: " + checksum);

            // 保存录制文件
            recorder.save("baseline_new/" + label + ".txt");

            // 恢复随机种子
            ConstNum.resetRandomSeed();

            DebugLogger.info("========== " + label + " 录制完成 ==========");
        }

        DebugLogger.getInstance().stopFileLogging();

        // 输出汇总
        System.out.println("\n========================================");
        System.out.println("  基线录制完成!");
        System.out.println("  全部7种配役均已录制到 baseline/ 目录");
        System.out.println("========================================");
        System.out.println("\n验证方法:");
        System.out.println("  重构后运行: java -cp \"out/production/vocr;bin/*\" BaselineRunner");
        System.out.println("  然后对比:   diff -r baseline/ baseline_new/");
        System.out.println("  若除时间戳外完全一致,则重构成功.");
    }
}