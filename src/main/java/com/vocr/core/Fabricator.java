package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.CharacterKanjiName;

import com.vocr.config.GameConstants;

import java.util.List;
import java.util.Random;

public class Fabricator
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

    /**
     * 在夜间死亡结算前，为仍可能晚期猎人 CO 的非人保存当夜假护卫记录。
     * 假占、假灵最迟会在第二个白天 CO，因此它们使用技能历史时不会与第 2 夜起的护卫记录冲突。
     */
    public void prepareFakeHunterDiaries()
    {
        int gameDay = ctx.getGameDay();
        for (int player = 1; player <= ctx.getPlayerSum(); player++)
        {
            if (!shouldMaintainFakeHunterDiary(player)
                    || ctx.getSkillTarget(player, gameDay) != 0) continue;
            fabricateHunterEntry(player, gameDay);
        }
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

    private void fabricateHunterEntry(int num, int gameDay)
    {
        DebugLogger.log("进入假猎人撒谎环节，假猎人下标：" + num);
        Random diaryRandom = new Random(fakeHunterSeed(num, gameDay));
        int selectedTarget = selectFakeHunterTarget(num, List.of(),
                (minimum, maximum) -> minimum + diaryRandom.nextInt(maximum - minimum + 1));
        ctx.recordSkillTarget(num, gameDay, selectedTarget);
        DebugLogger.log("假猎" + CharacterKanjiName.values()[ctx.getCharacterNumber(num)]
                + ",假猎目标" + CharacterKanjiName.values()[ctx.getCharacterNumber(selectedTarget)]);
    }

    /**
     * 旧实现会在死亡结算后生成假猎记录，并因此推进主随机流。
     * 新日记不采用这份已看到死亡信息的结果，但暂时保留相同的随机消耗，避免无关对局行为漂移。
     */
    private void preserveLegacyHunterRandomProgression(List<Integer> nightDeaths, int num)
    {
        if (ctx.isNonHumanMarked(num)) return;
        selectFakeHunterTarget(num, nightDeaths, ConstNum::randomInt);
    }

    private int selectFakeHunterTarget(int num, List<Integer> knownNightDeaths, RandomRange random)
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
