package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.config.GameConstants;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;

import java.util.List;

/** 明确发生咒杀时，协调非人、占卜师与灵媒师的首日 CO。 */
final class CurseKillClaimCoordinator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final ResultEventGenerator eventGenerator;
    private final Runnable gylogic;

    CurseKillClaimCoordinator(GameContext ctx, SuspicionSystem suspicion, Runnable gylogic)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.gylogic = gylogic;
    }

    void coordinate(List<Integer> nightDeaths)
    {
        DebugLogger.log("非人的初始工作：明确咒杀");
        DebugLogger.log("非人的初始工作：人狼上占");
        boolean hasWolfSeer = false;
        for(int i=1;i<=ctx.getInitialWolfCount();i++)
        {
            if(ctx.getNonHumanPlan(ctx.getRlIndex(i)) != NonHumanPlan.CLAIM_SEER) continue;
            if(nightDeaths.contains(ctx.getPlannedDivinationResult(ctx.getRlIndex(i))))
            {
                hasWolfSeer = true;
                ctx.addClaimCandidate(1, ctx.getRlIndex(i));
                ctx.recordSkillTarget(ctx.getRlIndex(i), 1, ctx.getPlannedDivinationResult(ctx.getRlIndex(i)));
                continue;
            }
            if(nightDeaths.contains(ctx.getPreliminaryDivinationTarget(ctx.getRlIndex(i))))
            {
                hasWolfSeer = true;
                ctx.addClaimCandidate(1, ctx.getRlIndex(i));
                ctx.recordSkillTarget(ctx.getRlIndex(i), 1, ctx.getPreliminaryDivinationTarget(ctx.getRlIndex(i)));
                continue;
            }
            ctx.clearNonHumanPlan(ctx.getRlIndex(i));
        }
        if(!hasWolfSeer)
        {
            for(int i=1;i<=ctx.getInitialWolfCount();i++)
            {
                if(ctx.getNonHumanPlan(ctx.getRlIndex(i)) == NonHumanPlan.CLAIM_SEER) continue;
                if(nightDeaths.contains(ctx.getPreliminaryDivinationTarget(ctx.getRlIndex(i))))
                {
                    hasWolfSeer = true;
                    ctx.addClaimCandidate(1, ctx.getRlIndex(i));
                    ctx.recordSkillTarget(ctx.getRlIndex(i), 1, ctx.getPreliminaryDivinationTarget(ctx.getRlIndex(i)));
                    ctx.setNonHumanPlan(ctx.getRlIndex(i), NonHumanPlan.CLAIM_SEER);
                }
            }
        }

        DebugLogger.log("非人的初始工作：狂人狂信上占");
        int leader = ctx.getNonHumanLeader();
        if(ctx.isAlive(leader) && nightDeaths.contains(ctx.getPreliminaryDivinationTarget(leader)))
        {
            ctx.setNonHumanPlan(leader, NonHumanPlan.CLAIM_SEER);
            ctx.recordSkillTarget(leader, 1, ctx.getPreliminaryDivinationTarget(leader));
            ctx.addClaimCandidate(1, leader);
        }
        else if(ctx.getNonHumanPlan(leader) == NonHumanPlan.CLAIM_SEER)
            ctx.clearNonHumanPlan(leader);
        DebugLogger.log("非人的初始工作：真占co");
        if(ctx.isAlive(ctx.getActualRoleIndex(1)))
            ctx.addClaimCandidate(1, ctx.getActualRoleIndex(1));
        DebugLogger.log("非人的初始工作：占文随机排序");
        ctx.shuffleZhans();
        for(int i=0;i<ctx.getZhans().size();i++)
        {
            int zhan = ctx.getZhans().get(i);
            ctx.queueEvent(new Event(EventName.zs14,ctx.getCharacterName(zhan),
                    ctx.getCharacterName(ctx.getSkillTarget(zhan, 1))));
            ctx.registerClaim(zhan, 1);
            ctx.addLazySuspicionValue(zhan, GameConstants.SUSPICION_DECREASE_SEER_CO);
            for(int j=0;j<i;j++)
                suspicion.adjustMutualSuspicion(zhan,ctx.getZhans().get(j),GameConstants.INF,GameConstants.INF);
        }
        DebugLogger.log("非人的初始工作：灵能co");
        DebugLogger.log("非人的初始工作：遍历每个人狼，判断是否有计划上灵的人狼");
        for(int i=1;i<=ctx.getInitialWolfCount();i++)
        {
            if(ctx.getNonHumanPlan(ctx.getRlIndex(i)) != NonHumanPlan.CLAIM_MEDIUM) continue;
            ctx.addClaimCandidate(2, ctx.getRlIndex(i));
            break;
        }
        DebugLogger.log("非人的初始工作：狂灵信灵");
        if(ctx.getNonHumanLeader() > 0 && ctx.isAlive(ctx.getNonHumanLeader())
                && ctx.getNonHumanPlan(ctx.getNonHumanLeader()) == NonHumanPlan.CLAIM_MEDIUM)
            ctx.addClaimCandidate(2, ctx.getNonHumanLeader());
        DebugLogger.log("非人的初始工作：真灵");
        if(ctx.isAlive(ctx.getActualRoleIndex(2)))
            ctx.addClaimCandidate(2, ctx.getActualRoleIndex(2));
        DebugLogger.log("非人的初始工作：灵能随机排序");
        ctx.shuffleLings();
        for(int i=0;i<ctx.getLings().size();i++)
        {
            int ling = ctx.getLings().get(i);
            eventGenerator.addEvent(EventName.lnco18, ling);
            ctx.registerClaim(ling, 2);
            ctx.addLazySuspicionValue(ling, GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
            for(int j=0;j<i;j++)
                suspicion.adjustMutualSuspicion(ling,ctx.getLings().get(j),GameConstants.INF,GameConstants.INF);
        }
        DebugLogger.log("非人的初始工作：共逻辑");
        gylogic.run();
    }
}
