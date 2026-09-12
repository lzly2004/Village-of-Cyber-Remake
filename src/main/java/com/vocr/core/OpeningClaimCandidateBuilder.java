package com.vocr.core;

import com.vocr.logging.DebugLogger;

/** 根据开局非人计划与真实职业建立首日占卜师、灵媒师 CO 候补。 */
final class OpeningClaimCandidateBuilder
{
    private final GameContext ctx;

    OpeningClaimCandidateBuilder(GameContext ctx)
    {
        this.ctx = ctx;
    }

    void build()
    {
        DebugLogger.log("非人的初始工作：占卜co");
        buildSeerCandidates();
        DebugLogger.log("非人的初始工作：灵能co");
        buildMediumCandidates();
    }

    private void buildSeerCandidates()
    {
        DebugLogger.log("非人的初始工作：狼占");
        for (int wolfOrder=1;wolfOrder<=ctx.getInitialWolfCount();wolfOrder++)
        {
            int wolf = ctx.getRlIndex(wolfOrder);
            if(ctx.isDead(wolf)) continue;
            if(ctx.getNonHumanPlan(wolf) == NonHumanPlan.CLAIM_SEER)
            {
                ctx.addClaimCandidate(1, wolf);
                ctx.recordSkillTarget(wolf, 1, ctx.getPlannedDivinationResult(wolf));
            }
        }
        DebugLogger.log("非人的初始工作：狂占信占");
        addPlannedSeerCandidate(ctx.getNonHumanLeader());
        DebugLogger.log("非人的初始工作：狐占");
        addPlannedSeerCandidate(ctx.getFox());
        DebugLogger.log("非人的初始工作：背占");
        addPlannedSeerCandidate(ctx.getDeviant());
        DebugLogger.log("非人的初始工作：真占");
        int seer = ctx.getActualRoleIndex(1);
        if(ctx.isAlive(seer))
            ctx.addClaimCandidate(1, seer);
    }

    private void addPlannedSeerCandidate(int player)
    {
        if(player <= 0 || ctx.getNonHumanPlan(player) != NonHumanPlan.CLAIM_SEER) return;
        ctx.addClaimCandidate(1, player);
        ctx.recordSkillTarget(player, 1, ctx.getPlannedDivinationResult(player));
    }

    private void buildMediumCandidates()
    {
        DebugLogger.log("非人的初始工作：狼灵");
        for(int wolfOrder=1;wolfOrder<=ctx.getInitialWolfCount();wolfOrder++)
        {
            int wolf = ctx.getRlIndex(wolfOrder);
            if(ctx.isDead(wolf)) continue;
            if(ctx.getNonHumanPlan(wolf) == NonHumanPlan.CLAIM_MEDIUM)
            {
                ctx.addClaimCandidate(2, wolf);
                break;
            }
        }
        DebugLogger.log("非人的初始工作：狂灵信灵");
        int leader = ctx.getNonHumanLeader();
        if(leader > 0 && ctx.isAlive(leader)
                && ctx.getNonHumanPlan(leader) == NonHumanPlan.CLAIM_MEDIUM)
            ctx.addClaimCandidate(2, leader);
        DebugLogger.log("非人的初始工作：狐灵");
        addPlannedMediumCandidate(ctx.getFox());
        DebugLogger.log("非人的初始工作：背灵");
        addPlannedMediumCandidate(ctx.getDeviant());
        DebugLogger.log("非人的初始工作：真灵");
        int medium = ctx.getActualRoleIndex(2);
        if(ctx.isAlive(medium))
            ctx.addClaimCandidate(2, medium);
    }

    private void addPlannedMediumCandidate(int player)
    {
        if(player > 0 && ctx.getNonHumanPlan(player) == NonHumanPlan.CLAIM_MEDIUM)
            ctx.addClaimCandidate(2, player);
    }
}
