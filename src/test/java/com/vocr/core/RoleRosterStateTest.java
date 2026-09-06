package com.vocr.core;

import com.vocr.domain.Role;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleRosterStateTest
{
    @Test
    void roster_shouldBuildAllIndexesInPlayerOrder()
    {
        int[] roles = {
                0,
                Role.gong.ordinal(),
                Role.renlang.ordinal(),
                Role.zhan.ordinal(),
                Role.renlang.ordinal(),
                Role.wu.ordinal(),
                Role.gong.ordinal()
        };

        RoleRosterState roster = new RoleRosterState(roles);

        assertEquals(3, roster.getPlayerByRole(Role.zhan.ordinal()));
        assertEquals(4, roster.getPlayerByRole(Role.renlang.ordinal()));
        assertEquals(0, roster.getPlayerByRole(Role.ling.ordinal()));
        assertEquals(1, roster.getSharedPlayer(1));
        assertEquals(6, roster.getSharedPlayer(2));
        assertEquals(2, roster.getWolfPlayer(1));
        assertEquals(4, roster.getWolfPlayer(2));
        assertEquals(0, roster.getWolfPlayer(6));
        assertEquals(2, roster.getSharedCount());
        assertEquals(2, roster.getWolfCount());
    }

    @Test
    void roster_shouldBeIndependentFromConstructorInput()
    {
        int[] roles = {0, Role.zhan.ordinal(), Role.renlang.ordinal()};
        RoleRosterState roster = new RoleRosterState(roles);

        roles[1] = Role.ling.ordinal();

        assertEquals(1, roster.getPlayerByRole(Role.zhan.ordinal()));
        assertEquals(0, roster.getPlayerByRole(Role.ling.ordinal()));
        assertEquals(2, roster.getWolfPlayer(1));
    }

    @Test
    void roster_shouldRejectInvalidConstructionAndQueries()
    {
        assertThrows(IllegalArgumentException.class, () -> new RoleRosterState(null));
        assertThrows(IllegalArgumentException.class, () -> new RoleRosterState(new int[1]));
        assertThrows(IllegalArgumentException.class, () -> new RoleRosterState(new int[]{0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> new RoleRosterState(new int[]{0, Role.values().length}));
        assertThrows(IllegalArgumentException.class, () -> new RoleRosterState(new int[]{
                0, Role.gong.ordinal(), Role.gong.ordinal(), Role.gong.ordinal()
        }));

        RoleRosterState roster = new RoleRosterState(
                new int[]{0, Role.zhan.ordinal(), Role.renlang.ordinal()});
        assertThrows(IllegalArgumentException.class, () -> roster.getPlayerByRole(0));
        assertThrows(IllegalArgumentException.class,
                () -> roster.getPlayerByRole(Role.values().length));
        assertThrows(IllegalArgumentException.class, () -> roster.getSharedPlayer(0));
        assertThrows(IllegalArgumentException.class, () -> roster.getSharedPlayer(3));
        assertThrows(IllegalArgumentException.class, () -> roster.getWolfPlayer(0));
        assertThrows(IllegalArgumentException.class, () -> roster.getWolfPlayer(3));
    }
}
