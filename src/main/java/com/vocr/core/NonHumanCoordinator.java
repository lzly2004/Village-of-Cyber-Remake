package com.vocr.core;

import com.vocr.logging.DebugLogger;

import java.util.List;

/** 协调非人开局策略，并将首日 CO 分派到普通或明确咒杀场景。 */
class NonHumanCoordinator
{
    private final NonHumanStrategyPlanner strategyPlanner;
    private final RegularClaimCoordinator regularClaimCoordinator;
    private final CurseKillClaimCoordinator curseKillClaimCoordinator;

    public NonHumanCoordinator(GameModule module)
    {
        GameContext ctx = module.getCtx();
        SuspicionSystem suspicion = module.getSuspicion();
        Runnable gylogic = module.getGylogic();
        this.strategyPlanner = new NonHumanStrategyPlanner(ctx, suspicion);
        this.regularClaimCoordinator = new RegularClaimCoordinator(ctx, module.getCoManager(), gylogic);
        this.curseKillClaimCoordinator = new CurseKillClaimCoordinator(ctx, suspicion, gylogic);
    }

    public void coordinate(List<Integer> diebody)
    {
        strategyPlanner.plan(diebody);
        DebugLogger.log("白天临时改变策略,以及占灵共的行动");
        if(diebody.size() > 1)
            curseKillClaimCoordinator.coordinate(diebody);
        else
            regularClaimCoordinator.coordinate(diebody);
    }
}
