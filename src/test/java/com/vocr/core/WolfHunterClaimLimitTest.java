package com.vocr.core;

import com.vocr.config.ProbabilityCalculator;
import com.vocr.domain.IntPair;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WolfHunterClaimLimitTest
{
    @Test
    void sequentialExecutionAvoidance_shouldChooseCatAfterFirstWolfHunter()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.setNonHumanPlan(first, NonHumanPlan.CLAIM_HUNTER);
        ctx.setNonHumanPlan(second, NonHumanPlan.NONE);
        ExecutionManager execution = execution(ctx);

        assertFalse(execution.execute(2, List.of(first), true));
        assertFalse(execution.execute(2, List.of(second), true));

        assertEquals(3, ctx.getClaimedRole(first));
        assertEquals(5, ctx.getClaimedRole(second));
        assertEquals(1, wolfHunterCount(ctx));
    }

    @Test
    void sameExecutionBatch_shouldNotHonorSecondPlannedWolfHunter()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.setNonHumanPlan(first, NonHumanPlan.CLAIM_HUNTER);
        ctx.setNonHumanPlan(second, NonHumanPlan.CLAIM_HUNTER);

        assertFalse(execution(ctx).execute(2, List.of(first, second), true));

        assertEquals(3, ctx.getClaimedRole(first));
        assertEquals(0, ctx.getClaimedRole(second));
        assertEquals(1, ctx.getPendingEventCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void roleInquiry_shouldReservePlannedAndSpontaneousWolfResponsesTogether(boolean plannedFirst)
    {
        GameContext ctx = fresh(peiyi.jianyi);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        int earlier = Math.min(first, second), later = Math.max(first, second);
        ctx.setNonHumanPlan(plannedFirst ? earlier : later, NonHumanPlan.CLAIM_HUNTER);
        ctx.setNonHumanPlan(plannedFirst ? later : earlier, NonHumanPlan.NONE);
        List<IntPair> responses = new ClaimResponsePlanner(ctx, alwaysCo()).planResponsesForRole(Role.lie);
        assertEquals(1, plannedWolfHunters(ctx, responses));
        assertTrue(responses.contains(new IntPair(ctx.getActualRoleIndex(3), 3)));
        COManager manager = manager(ctx);
        for (IntPair response : responses)
            manager.processActualCo(response.first(), response.second(), List.of());
        assertEquals(1, wolfHunterCount(ctx));
    }

    @Test
    void roleInquiry_shouldRejectPlannedHunterAfterExistingWolfHunter()
    {
        GameContext ctx = fresh(peiyi.jianyi);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.registerClaim(first, 3);
        ctx.setNonHumanPlan(second, NonHumanPlan.CLAIM_HUNTER);
        ctx.recordSkillTarget(second, 1, first);
        List<IntPair> responses = new ClaimResponsePlanner(ctx, alwaysCo()).planResponsesForRole(Role.lie);
        assertEquals(first, ctx.getSkillTarget(second, 1), "拒绝预定回应不改写技能历史");
        assertEquals(0, plannedWolfHunters(ctx, responses));
        assertTrue(responses.contains(new IntPair(ctx.getActualRoleIndex(3), 3)));
    }

    @Test
    void playerInquiry_shouldReserveSpontaneousHunterWithinBatch()
    {
        GameContext ctx = fresh(peiyi.jianyi);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.setNonHumanPlan(first, NonHumanPlan.NONE);
        ctx.setNonHumanPlan(second, NonHumanPlan.NONE);
        List<IntPair> responses = new ClaimResponsePlanner(ctx, alwaysCo())
                .planResponsesForPlayers(List.of(first, second));
        assertEquals(1, plannedWolfHunters(ctx, responses));
    }

    @Test
    void playerInquiry_shouldReservePlannedHunterWithinBatch()
    {
        GameContext ctx = fresh(peiyi.jianyi);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.setNonHumanPlan(first, NonHumanPlan.CLAIM_HUNTER);
        ctx.setNonHumanPlan(second, NonHumanPlan.CLAIM_HUNTER);
        List<IntPair> responses = new ClaimResponsePlanner(ctx, alwaysCo())
                .planResponsesForPlayers(List.of(first, second));
        assertEquals(List.of(new IntPair(first, 3)), responses);
    }

    @Test
    void rejectedActualClaim_shouldNotChangeIdentityEventsOrSuspicion()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        COManager manager = manager(ctx);
        manager.processActualCo(first, 3, List.of());
        ctx.clearPendingEvents();
        int before = ctx.getLazySuspicionValue(second);
        int mutual = ctx.getSuspicionValue(first, second);
        manager.processActualCo(second, 3, List.of());
        assertEquals(0, ctx.getClaimedRole(second));
        assertEquals(-1, ctx.getComingOutDay(second));
        assertEquals(0, ctx.getClaimedRoleOrder(second));
        assertEquals(1, ctx.getClaimedRoleOrderCount(3));
        assertEquals(before, ctx.getLazySuspicionValue(second));
        assertEquals(mutual, ctx.getSuspicionValue(first, second));
        assertEquals(0, ctx.getPendingEventCount());
    }

    @Test
    void atomicRegistration_shouldRejectSecondWolfEvenAfterFirstDiesAndLeavesCandidates()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.registerClaim(first, 3);
        ctx.registerClaim(first, 3); // 原有幂等登记仍支持。
        ctx.recordDeath(first, whyDie.chuxing);
        ctx.removeClaimCandidate(3, first);
        manager(ctx).processActualCo(second, 3, List.of());
        assertFalse(ctx.hasPendingEvents());
        assertThrows(IllegalStateException.class, () -> ctx.registerClaim(second, 3));
        assertEquals(0, ctx.getClaimedRole(second));
        assertEquals(1, ctx.getClaimedRoleOrderCount(3));
        assertTrue(ctx.getLies().isEmpty());
    }

    @Test
    void firstPlannedWolf_shouldStillClaimWhenLegacyPlanningFlagAlreadyReserved()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int wolf = ctx.getRlIndex(1);
        ctx.setNonHumanPlan(wolf, NonHumanPlan.CLAIM_HUNTER);
        ctx.markWolfClaimedRole(3); // 既有白天接黑逻辑会在实际CO前标记计划。
        manager(ctx).processActualCo(wolf, 3, List.of());
        assertEquals(3, ctx.getClaimedRole(wolf));
    }

    @Test
    void exhaustedAvoidanceRoles_shouldEmitNothing()
    {
        GameContext ctx = fresh(peiyi.jianyi); // 没有猫。
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.registerClaim(first, 3);
        manager(ctx).processMaoLieCo(second, true, List.of());
        assertEquals(0, ctx.getClaimedRole(second));
        assertFalse(ctx.hasPendingEvents());
    }

    @Test
    void catDefinitivelyOut_shouldNotFallbackToCatOrDuplicateHunter()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.registerClaim(first, 3);
        ctx.recordDeath(ctx.getCat(), whyDie.chuxing);
        manager(ctx).processMaoLieCo(second, true, List.of());
        assertEquals(0, ctx.getClaimedRole(second));
        assertFalse(ctx.hasPendingEvents());
    }

    @Test
    void trueHunterAndNonWolfFakeHunter_shouldRemainAllowedAndNewGameShouldReset()
    {
        GameContext ctx = fresh(peiyi.daxing);
        ctx.registerClaim(ctx.getRlIndex(1), 3);
        int hunter = ctx.getActualRoleIndex(3);
        int nonWolf = 0;
        for (int i = 1; i <= ctx.getPlayerSum(); i++)
            if (ctx.getActualRole(i) > 7) { nonWolf = i; break; }
        assertTrue(nonWolf > 0);
        manager(ctx).processActualCo(hunter, 3, List.of());
        manager(ctx).processActualCo(nonWolf, 3, List.of());
        assertEquals(3, ctx.getClaimedRole(hunter));
        assertEquals(3, ctx.getClaimedRole(nonWolf));
        GameContext next = fresh(peiyi.daxing);
        manager(next).processActualCo(next.getRlIndex(2), 3, List.of());
        assertEquals(1, wolfHunterCount(next));
    }

    @Test
    void unavailableCat_shouldLeaveHunterAvailableForFirstWolf()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int wolf = ctx.getRlIndex(1);
        ctx.recordDeath(ctx.getCat(), whyDie.chuxing);
        manager(ctx).processMaoLieCo(wolf, true, List.of());
        assertEquals(3, ctx.getClaimedRole(wolf));
    }

    @Test
    void catAlreadyAsked_shouldNotAllowSecondWolfAvoidanceClaim()
    {
        GameContext ctx = fresh(peiyi.daxing);
        int first = ctx.getRlIndex(1), second = ctx.getRlIndex(2);
        ctx.registerClaim(first, 3);
        ctx.tryRecordClaimRequest(5, ctx.getGameDay());
        manager(ctx).processMaoLieCo(second, true, List.of());
        assertEquals(0, ctx.getClaimedRole(second));
        assertFalse(ctx.hasPendingEvents());
    }

    private static long plannedWolfHunters(GameContext ctx, List<IntPair> responses)
    {
        return responses.stream().filter(r -> ctx.getActualRole(r.first()) == 7 && r.second() == 3).count();
    }

    private static int wolfHunterCount(GameContext ctx)
    {
        int count = 0;
        for (int i = 1; i <= ctx.getPlayerSum(); i++)
            if (ctx.getActualRole(i) == 7 && ctx.getClaimedRole(i) == 3) count++;
        return count;
    }

    private static GameContext fresh(peiyi configuration)
    {
        GameStatus status = new GameStatus(new GameRandom(20261111L));
        status.startGame(configuration);
        GameContext ctx = new GameContext(status, NonHumanConfiguration.from(configuration, 0));
        ctx.incrementGameDay();
        return ctx;
    }

    private static ProbabilityCalculator alwaysCo()
    {
        return new ProbabilityCalculator() {
            @Override public int maolieco(int role, int situation, int p1, int p2) { return 100; }
        };
    }

    private static COManager manager(GameContext ctx)
    {
        SuspicionSystem suspicion = new SuspicionSystem(ctx);
        return new COManager(ctx, suspicion, new ResultPresenter(ctx, suspicion), alwaysCo());
    }

    private static ExecutionManager execution(GameContext ctx)
    {
        return new ExecutionManager(new GameModule.Builder().ctx(ctx).coManager(manager(ctx))
                .probabilityCalculator(alwaysCo()).deliverEvents(() -> {}).build());
    }
}
