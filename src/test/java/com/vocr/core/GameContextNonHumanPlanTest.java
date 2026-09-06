package com.vocr.core;

import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextNonHumanPlanTest
{
    @Test
    void plans_shouldStartEmptyAndSupportEveryValidMeaning()
    {
        GameContext context = createContext();

        assertEquals(NonHumanPlan.NONE, context.getNonHumanPlan(1));
        for (NonHumanPlan plan : NonHumanPlan.values())
        {
            context.setNonHumanPlan(1, plan);
            assertEquals(plan, context.getNonHumanPlan(1));
        }

        context.clearNonHumanPlan(1);
        assertEquals(NonHumanPlan.NONE, context.getNonHumanPlan(1));
    }

    @Test
    void plans_shouldRejectInvalidPlayersAndNullBeforeMutation()
    {
        GameContext context = createContext();
        context.setNonHumanPlan(1, NonHumanPlan.CLAIM_SEER);

        assertThrows(IllegalArgumentException.class, () -> context.getNonHumanPlan(0));
        assertThrows(IllegalArgumentException.class,
                () -> context.getNonHumanPlan(context.getPlayerSum() + 1));
        assertThrows(IllegalArgumentException.class,
                () -> context.setNonHumanPlan(0, NonHumanPlan.CLAIM_MEDIUM));
        assertThrows(IllegalArgumentException.class, () -> context.setNonHumanPlan(1, null));

        assertEquals(NonHumanPlan.CLAIM_SEER, context.getNonHumanPlan(1));
    }

    @Test
    void claimPlans_shouldMapOnlySupportedPublicRoles()
    {
        assertEquals(0, NonHumanPlan.NONE.legacyCode());
        assertEquals(1, NonHumanPlan.CLAIM_SEER.legacyCode());
        assertEquals(2, NonHumanPlan.CLAIM_MEDIUM.legacyCode());
        assertEquals(3, NonHumanPlan.CLAIM_HUNTER.legacyCode());
        assertEquals(4, NonHumanPlan.STAY_HIDDEN.legacyCode());
        assertEquals(5, NonHumanPlan.CLAIM_CAT.legacyCode());

        assertEquals(NonHumanPlan.CLAIM_SEER, NonHumanPlan.claimRole(1));
        assertEquals(NonHumanPlan.CLAIM_MEDIUM, NonHumanPlan.claimRole(2));
        assertEquals(NonHumanPlan.CLAIM_HUNTER, NonHumanPlan.claimRole(3));
        assertEquals(NonHumanPlan.CLAIM_CAT, NonHumanPlan.claimRole(5));
        assertTrue(NonHumanPlan.CLAIM_SEER.claimsRole(1));
        assertTrue(NonHumanPlan.CLAIM_MEDIUM.isSeerOrMediumClaim());
        assertFalse(NonHumanPlan.STAY_HIDDEN.isClaimPlan());

        assertThrows(IllegalArgumentException.class, () -> NonHumanPlan.claimRole(4));
        assertThrows(IllegalArgumentException.class, () -> NonHumanPlan.claimRole(6));
        assertThrows(IllegalStateException.class, NonHumanPlan.STAY_HIDDEN::claimedRole);
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status);
    }
}
