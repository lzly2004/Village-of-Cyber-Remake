package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.CharacterKanjiName;

import com.vocr.config.GameConstants;

import java.util.List;
import java.util.Random;

class Fabricator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final ZhanFabricator zhanFabricator;
    private final LingFabricator lingFabricator;
    private final HunterGuarder hunterGuarder;

    public Fabricator(GameContext ctx, SuspicionSystem suspicion,
                      SeerDiviner seerDiviner, HunterGuarder hunterGuarder)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.zhanFabricator = new ZhanFabricator(ctx, suspicion, seerDiviner);
        this.lingFabricator = new LingFabricator(ctx, suspicion);
        this.hunterGuarder = hunterGuarder;
    }

    public void fabricate(List<Integer> diebody)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getActualRole(i) < 7 || !ctx.isAlive(i)) continue;
            NonHumanPlan plan = ctx.getNonHumanPlan(i);
            if (ctx.getClaimedRole(i) == 6
                    || (ctx.getClaimedRole(i) == 0 && plan == NonHumanPlan.NONE)) continue;
            if (ctx.getClaimedRole(i) == 1
                    || (ctx.getClaimedRole(i) == 0 && plan == NonHumanPlan.CLAIM_SEER))
            {
                zhanFabricator.fabricate(diebody, i);
            }
            else if (ctx.getClaimedRole(i) == 2
                    || (ctx.getClaimedRole(i) == 0 && plan == NonHumanPlan.CLAIM_MEDIUM))
            {
                lingFabricator.fabricate(i);
                DebugLogger.log("假灵能结果：" + ctx.getSkillTarget(i, gd));
            }
            else
                preserveLegacyHunterRandomProgression(diebody, i);
        }
    }

    /** 结算前只捕获本夜需要日记的演员；不选择目标或写技能历史。 */
    public List<Integer> prepareFakeHunterDiaries()
    {
        if (ctx.getGameDay() < 2) return List.of();
        java.util.ArrayList<Integer> actors = new java.util.ArrayList<>();
        for (int player = 1; player <= ctx.getPlayerSum(); player++)
            if (shouldMaintainFakeHunterDiary(player)
                    && ctx.getSkillTarget(player, ctx.getGameDay()) == 0)
                actors.add(player);
        return List.copyOf(actors);
    }

    /** 死亡结算后、终局/次日快照前完成本夜报告；不重写已经保存的历史。 */
    public void completeFakeHunterDiaries(List<Integer> actors, List<Integer> nightDeaths)
    {
        int day = ctx.getGameDay();
        if (day < 2) return;
        for (int player : actors)
            if (ctx.getSkillTarget(player, day) == 0)
                fabricateHunterEntry(player, day, nightDeaths);
    }

    private boolean shouldMaintainFakeHunterDiary(int player)
    {
        if (ctx.getActualRole(player) < 7 || !ctx.isAlive(player) || ctx.isNonHumanMarked(player))
            return false;

        int claimedRole = ctx.getClaimedRole(player);
        if (claimedRole == 3) return true;
        if (claimedRole != 0) return false;
        return !ctx.getNonHumanPlan(player).isSeerOrMediumClaim();
    }

    private void fabricateHunterEntry(int num, int gameDay, List<Integer> nightDeaths)
    {
        DebugLogger.log("进入假猎人撒谎环节，假猎人下标：" + num);
        Random diaryRandom = new Random(fakeHunterSeed(num, gameDay));
        int selectedTarget = selectFakeHunterTarget(num, nightDeaths,
                (minimum, maximum) -> minimum + diaryRandom.nextInt(maximum - minimum + 1));
        if (selectedTarget == 0) return; // 没有合法活目标，不伪造护卫对象。
        ctx.recordSkillTarget(num, gameDay, selectedTarget);
        DebugLogger.log("假猎" + CharacterKanjiName.values()[ctx.getCharacterNumber(num)]
                + ",假猎目标" + CharacterKanjiName.values()[ctx.getCharacterNumber(selectedTarget)]);
    }

    /**
     * 旧实现会在死亡结算后生成假猎记录，并因此推进主随机流。
     * 恢复后的日记仍使用独立随机源；此处保留旧主随机消耗，避免无关行为漂移。
     */
    private void preserveLegacyHunterRandomProgression(List<Integer> nightDeaths, int num)
    {
        if (ctx.isNonHumanMarked(num)) return;
        selectLegacyFakeHunterTarget(num, nightDeaths, ctx.getRandom()::randomInt);
    }

    // 仅维持旧主随机消耗，不决定日记；这里的历史控制流刻意不随策略修复改变。
    private int selectLegacyFakeHunterTarget(int num, List<Integer> knownNightDeaths, RandomRange random)
    {
        int[] target = new int[2];
        int[] weight = {GameConstants.WEIGHT_FAKE_HUNTER_WHITE, GameConstants.WEIGHT_FAKE_HUNTER_BLACK};
        for (int i = 0; i < 2; i++)
        {
            target[i] = hunterGuarder.guard(num,
                    bound -> random.nextInt(0, bound - 1));
            if (ctx.getActualRole(num) == 7 || ctx.getActualRole(num) == 9)
            {
                if (ctx.getActualRole(target[i]) == 7) break;
                if (random.nextInt(1, 100) <= GameConstants.PROB_FAKE_HUNTER_REROLL)
                {
                    target[i] = hunterGuarder.guard(num,
                            bound -> random.nextInt(0, bound - 1));
                    continue;
                }
                break;
            }
            if (knownNightDeaths.size() == 1 && target[i] == knownNightDeaths.get(0))
                weight[i] = 1;
        }
        if (target[1] == 0) target[1] = target[0];
        int randomValue = random.nextInt(0, weight[0] + weight[1] - 1);
        return target[randomValue < weight[0] ? 0 : 1];
    }

    /** 恢复原内层重抽循环，完成两个候选后再按单死体软回避权重选择。 */
    private int selectFakeHunterTarget(int num, List<Integer> knownNightDeaths, RandomRange random)
    {
        int[] target = new int[2];
        int[] weight = {GameConstants.WEIGHT_FAKE_HUNTER_WHITE, GameConstants.WEIGHT_FAKE_HUNTER_BLACK};
        for (int i = 0; i < 2; i++)
        {
            target[i] = hunterGuarder.guardForDiary(num, bound -> random.nextInt(0, bound - 1));
            if (target[i] == 0) return 0;
            while (ctx.getActualRole(num) == 7 || ctx.getActualRole(num) == 9)
            {
                if (ctx.getActualRole(target[i]) == 7) break;
                if (random.nextInt(1, 100) <= GameConstants.PROB_FAKE_HUNTER_REROLL)
                {
                    target[i] = hunterGuarder.guardForDiary(num, bound -> random.nextInt(0, bound - 1));
                    if (target[i] == 0) return 0;
                    continue;
                }
                break;
            }
            if (knownNightDeaths.size() == 1 && target[i] == knownNightDeaths.get(0))
                weight[i] = 1;
        }
        int value = random.nextInt(0, weight[0] + weight[1] - 1);
        return target[value < weight[0] ? 0 : 1];
    }

    private long fakeHunterSeed(int player, int gameDay)
    {
        long seed = 0xcbf29ce484222325L;
        seed = mixSeed(seed, ctx.getPeiyi().ordinal());
        seed = mixSeed(seed, player);
        seed = mixSeed(seed, gameDay);
        for (int current = 1; current <= ctx.getPlayerSum(); current++)
        {
            seed = mixSeed(seed, ctx.getCharacterNumber(current));
            seed = mixSeed(seed, ctx.getActualRole(current));
        }
        return seed;
    }

    private long mixSeed(long seed, int value)
    {
        return (seed ^ value) * 0x100000001b3L;
    }

    @FunctionalInterface
    private interface RandomRange
    {
        int nextInt(int minimum, int maximum);
    }
}
