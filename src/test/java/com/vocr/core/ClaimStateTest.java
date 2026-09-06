package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.Role;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimStateTest
{
    @Test
    void candidates_shouldRemainSeparatedOrderedAndReadOnly()
    {
        ClaimState state = new ClaimState(4);

        assertTrue(state.hasCandidateList(Role.zhan.ordinal()));
        assertTrue(state.hasCandidateList(Role.ling.ordinal()));
        assertTrue(state.hasCandidateList(Role.lie.ordinal()));
        assertTrue(state.hasCandidateList(Role.mao.ordinal()));
        assertFalse(state.hasCandidateList(Role.gong.ordinal()));
        assertFalse(state.hasCandidateList(Role.wu.ordinal()));

        state.addCandidate(Role.zhan.ordinal(), 2);
        state.addCandidate(Role.zhan.ordinal(), 2);
        state.addCandidate(Role.zhan.ordinal(), 3);
        state.addCandidate(Role.ling.ordinal(), 1);

        assertEquals(List.of(2, 3), state.getCandidates(Role.zhan.ordinal()));
        assertEquals(List.of(1), state.getCandidates(Role.ling.ordinal()));
        assertTrue(state.getCandidates(Role.lie.ordinal()).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> state.getCandidates(Role.zhan.ordinal()).add(4));

        state.removeCandidate(Role.zhan.ordinal(), 2);
        assertEquals(List.of(3), state.getCandidates(Role.zhan.ordinal()));
    }

    @Test
    void metadata_shouldTrackIndependentOrdersAndFirstRequestDay()
    {
        ClaimState state = new ClaimState(4);

        assertEquals(1, state.nextOrder(Role.zhan.ordinal()));
        assertEquals(2, state.nextOrder(Role.zhan.ordinal()));
        assertEquals(1, state.nextOrder(Role.ling.ordinal()));
        assertEquals(2, state.getOrderCount(Role.zhan.ordinal()));
        assertEquals(1, state.getOrderCount(Role.ling.ordinal()));
        assertEquals(0, state.getOrderCount(Role.NONE.ordinal()));
        assertEquals(0, state.getOrderCount(Role.beidezhe.ordinal()));

        assertTrue(state.tryRecordRequest(Role.lie.ordinal(), 2));
        assertFalse(state.tryRecordRequest(Role.lie.ordinal(), 3));
        assertEquals(2, state.getRequestDay(Role.lie.ordinal()));
        assertEquals(0, state.getRequestDay(Role.NONE.ordinal()));
        assertEquals(0, state.getRequestDay(Role.beidezhe.ordinal()));
    }

    @Test
    void nonHumanSummaryAndWolfClaimFlags_shouldReflectCandidateState()
    {
        ClaimState state = new ClaimState(5);
        state.addCandidate(Role.zhan.ordinal(), 1);
        state.addCandidate(Role.zhan.ordinal(), 2);
        state.addCandidate(Role.ling.ordinal(), 3);
        state.addCandidate(Role.lie.ordinal(), 1);
        state.addCandidate(Role.lie.ordinal(), 4);
        state.addCandidate(Role.mao.ordinal(), 1);
        state.addCandidate(Role.mao.ordinal(), 4);
        state.addCandidate(Role.mao.ordinal(), 5);

        assertEquals(4, state.getNonHumanCandidateCount());
        assertFalse(state.isWolfClaimedHunter());
        assertFalse(state.isWolfClaimedCat());

        state.markWolfClaimedRole(Role.lie.ordinal());
        state.markWolfClaimedRole(Role.mao.ordinal());

        assertTrue(state.isWolfClaimedHunter());
        assertTrue(state.isWolfClaimedCat());
    }

    @Test
    void invalidOperations_shouldFailBeforeChangingState()
    {
        assertThrows(IllegalArgumentException.class, () -> new ClaimState(0));
        ClaimState state = new ClaimState(3);

        assertThrows(IllegalArgumentException.class, () -> state.getRequestDay(-1));
        assertThrows(IllegalArgumentException.class,
                () -> state.getOrderCount(Role.values().length));
        assertThrows(IllegalArgumentException.class,
                () -> state.tryRecordRequest(Role.renlang.ordinal(), 2));
        assertThrows(IllegalArgumentException.class,
                () -> state.tryRecordRequest(Role.zhan.ordinal(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> state.tryRecordRequest(Role.zhan.ordinal(), GameConstants.MAX_GAME_DAYS + 1));
        assertThrows(IllegalArgumentException.class,
                () -> state.nextOrder(Role.gong.ordinal()));
        assertThrows(IllegalArgumentException.class,
                () -> state.getCandidates(Role.wu.ordinal()));
        assertThrows(IllegalArgumentException.class,
                () -> state.addCandidate(Role.zhan.ordinal(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> state.addCandidate(Role.zhan.ordinal(), 4));
        assertThrows(IllegalArgumentException.class,
                () -> state.addCandidate(Role.gong.ordinal(), 1));
        assertThrows(IllegalArgumentException.class,
                () -> state.removeCandidate(Role.zhan.ordinal(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> state.markWolfClaimedRole(Role.zhan.ordinal()));

        assertTrue(state.getCandidates(Role.zhan.ordinal()).isEmpty());
        assertEquals(0, state.getOrderCount(Role.zhan.ordinal()));
        assertEquals(0, state.getRequestDay(Role.zhan.ordinal()));
        assertFalse(state.isWolfClaimedHunter());
        assertFalse(state.isWolfClaimedCat());
    }
}
