package com.vocr.core;

import com.vocr.domain.IntPair;
import com.vocr.domain.peiyi;

import java.util.ArrayList;
import java.util.List;

/** 规划首个白天被占卜师判黑玩家的身份声明响应。 */
final class OpeningBlackResultResponsePlanner
{
    private final GameContext ctx;

    OpeningBlackResultResponsePlanner(GameContext ctx)
    {
        this.ctx = ctx;
    }

    void collectResponses(int playerCount, List<IntPair> responses)
    {
        boolean wolfSeerOrMediumResponseAdded = false;
        boolean wolfHunterOrCatResponseAdded = false;
        List<Integer> blackTargets = collectBlackTargets(playerCount);

        for(int blackTarget : blackTargets)
        {
            if(ctx.isDead(blackTarget)) continue;

            int actualRole = ctx.getActualRole(blackTarget);
            if(actualRole < 7)
            {
                responses.add(new IntPair(blackTarget, actualRole));
            }
            else if(actualRole == 7)
            {
                NonHumanPlan plan = ctx.getNonHumanPlan(blackTarget);
                if(plan.isClaimPlan())
                {
                    responses.add(new IntPair(blackTarget, plan.claimedRole()));
                }
                else
                {
                    int wolfSeerClaims = 0;
                    int wolfMediumClaims = 0;
                    int wolfSeerWeight = 10;
                    int wolfMediumWeight = 10;
                    int wolfHunterOrCatWeight = 10;
                    int stayHiddenWeight = 10;
                    for(int wolfOrder=1;wolfOrder<=ctx.getInitialWolfCount();wolfOrder++)
                    {
                        if(ctx.getClaimedRole(ctx.getRlIndex(wolfOrder)) == 1)
                            wolfSeerClaims++;
                        else if(ctx.getClaimedRole(ctx.getRlIndex(wolfOrder)) == 2)
                            wolfMediumClaims++;
                    }
                    if(wolfSeerClaims > 1) wolfSeerWeight = 0;
                    if(wolfMediumClaims > 0) wolfMediumWeight = 0;
                    if(wolfSeerOrMediumResponseAdded)
                    {
                        wolfSeerWeight = 0;
                        wolfMediumWeight = 0;
                    }
                    if(wolfHunterOrCatResponseAdded)
                        wolfHunterOrCatWeight = 0;
                    if(ctx.getPeiyi() == peiyi.jianyi
                            && (ctx.getZhans().contains(ctx.getRlIndex(1))
                            || ctx.getZhans().contains(ctx.getRlIndex(2))))
                    {
                        wolfSeerWeight = 0;
                        wolfMediumWeight = 0;
                        wolfHunterOrCatWeight = 0;
                    }

                    int option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                            wolfSeerWeight, wolfMediumWeight, wolfHunterOrCatWeight, stayHiddenWeight)));
                    switch(option)
                    {
                        case 0:
                            ctx.recordSkillTarget(blackTarget, 1, ctx.getPreliminaryDivinationTarget(blackTarget));
                            responses.add(new IntPair(blackTarget,1));
                            wolfSeerOrMediumResponseAdded = true;
                            break;
                        case 1:
                            responses.add(new IntPair(blackTarget,2));
                            wolfSeerOrMediumResponseAdded = true;
                            for(int wolfOrder=1;wolfOrder<=ctx.getInitialWolfCount();wolfOrder++)
                            {
                                NonHumanPlan wolfPlan = ctx.getNonHumanPlan(ctx.getRlIndex(wolfOrder));
                                // 兼容旧实现：这里比较计划编码而不是当前人狼编号，玩法正确性需另行审计。
                                if(wolfPlan == NonHumanPlan.CLAIM_MEDIUM
                                        && !blackTargets.contains(wolfPlan.legacyCode()))
                                    ctx.setNonHumanPlan(ctx.getRlIndex(wolfOrder), NonHumanPlan.STAY_HIDDEN);
                            }
                            break;
                        case 2:
                            int role = chooseHunterOrCatRole();
                            NonHumanPlan responsePlan = NonHumanPlan.claimRole(role);
                            ctx.setNonHumanPlan(blackTarget, responsePlan);
                            if(responsePlan == NonHumanPlan.CLAIM_CAT)
                                responses.add(new IntPair(blackTarget,5));
                            wolfHunterOrCatResponseAdded = true;
                            break;
                        case 3:
                            ctx.clearNonHumanPlan(blackTarget);
                            break;
                    }
                }
            }
            else
            {
                handleWhiteNonHumanResponse(blackTarget, responses);
            }
        }
    }

    private List<Integer> collectBlackTargets(int playerCount)
    {
        List<Integer> blackTargets = new ArrayList<>();
        for(int seer : ctx.getZhans())
        {
            if(ctx.getSkillTarget(seer,1) <= playerCount) continue;
            int blackTarget = ctx.getSkillTarget(seer,1) - playerCount;
            if(!blackTargets.contains(blackTarget))
                blackTargets.add(blackTarget);
        }
        return blackTargets;
    }

    private void handleWhiteNonHumanResponse(int blackTarget, List<IntPair> responses)
    {
        DebugLogger.log("白非接黒并且没有之前的计划->狂狐背");
        int option = 0;
        // 兼容旧实现：这里按被判黑玩家编号分配权重，而不是按实际职业分配。
        switch(blackTarget)
        {
            case 8: case 9:
                option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(95,5,0,0)));
                break;
            case 10:
                option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(80,5,10,5)));
                break;
            case 11:
                int aggression = ctx.getNonHumanAggression();
                option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                        45 + aggression, 30, 20, 5 - aggression)));
                break;
        }

        switch(option)
        {
            case 0:
                ctx.recordSkillTarget(blackTarget, 1, ctx.getPreliminaryDivinationTarget(blackTarget));
                responses.add(new IntPair(blackTarget,1));
                break;
            case 1:
                responses.add(new IntPair(blackTarget,2));
                break;
            case 2:
                int role = chooseHunterOrCatRole();
                NonHumanPlan responsePlan = NonHumanPlan.claimRole(role);
                ctx.setNonHumanPlan(blackTarget, responsePlan);
                if(responsePlan == NonHumanPlan.CLAIM_CAT)
                    responses.add(new IntPair(blackTarget,5));
                break;
            case 3:
                ctx.clearNonHumanPlan(blackTarget);
                break;
        }
    }

    private int chooseHunterOrCatRole()
    {
        int role = 5 - GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(
                50 + 50 * ctx.getLies().size(), 50 + 50 * ctx.getMaos().size()))) * 2;
        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut()) role = 3;
        return role;
    }
}
