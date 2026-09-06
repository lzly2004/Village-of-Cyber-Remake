package com.vocr.core;

import com.vocr.domain.Role;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NonHumanConfigurationTest
{
    static Stream<Arguments> configurations()
    {
        return Stream.of(
                Arguments.of(peiyi.jianyi, 2, 3, Role.kuangren),
                Arguments.of(peiyi.tongchang, 3, 4, Role.kuangren),
                Arguments.of(peiyi.yaoohu, 3, 5, Role.kuangren),
                Arguments.of(peiyi.kuangxin, 3, 5, Role.kuangxinzhe),
                Arguments.of(peiyi.beide, 3, 6, Role.kuangren),
                Arguments.of(peiyi.maoyou, 4, 6, Role.kuangren),
                Arguments.of(peiyi.daxing, 4, 7, Role.kuangxinzhe)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configurations")
    void from_shouldDescribeEachSupportedConfiguration(
            peiyi peiyi, int wolfCount, int nonHumanCount, Role leaderRole)
    {
        NonHumanConfiguration configuration = NonHumanConfiguration.from(peiyi, 4);

        assertEquals(wolfCount, configuration.initialWolfCount());
        assertEquals(nonHumanCount, configuration.initialNonHumanCount());
        assertEquals(leaderRole, configuration.leaderRole());
        assertEquals(4, configuration.aggression());
        assertEquals(leaderRole == Role.kuangxinzhe, configuration.usesFanatic());
    }

    @Test
    void configuration_shouldValidateAllConstructionInputs()
    {
        assertThrows(IllegalArgumentException.class,
                () -> NonHumanConfiguration.from(null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> NonHumanConfiguration.from(peiyi.NONE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> NonHumanConfiguration.from(peiyi.jianyi, -1));
        assertThrows(IllegalArgumentException.class,
                () -> NonHumanConfiguration.from(peiyi.jianyi, 6));
        assertThrows(IllegalArgumentException.class,
                () -> new NonHumanConfiguration(0, 3, Role.kuangren, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new NonHumanConfiguration(3, 3, Role.kuangren, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new NonHumanConfiguration(2, 3, Role.renlang, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new NonHumanConfiguration(2, 3, null, 0));

        assertFalse(NonHumanConfiguration.from(peiyi.jianyi, 0).usesFanatic());
        assertTrue(NonHumanConfiguration.from(peiyi.daxing, 5).usesFanatic());

        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        GameContext wrongCount = new GameContext(
                status, new NonHumanConfiguration(2, 4, Role.kuangren, 0));
        GameContext wrongLeader = new GameContext(
                status, new NonHumanConfiguration(2, 3, Role.kuangxinzhe, 0));
        assertThrows(IllegalStateException.class, wrongCount::validateRoleRoster);
        assertThrows(IllegalStateException.class, wrongLeader::validateRoleRoster);
    }
}
