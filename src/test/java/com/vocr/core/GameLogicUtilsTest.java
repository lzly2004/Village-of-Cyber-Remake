package com.vocr.core;

import com.vocr.domain.whyDie;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameLogicUtilsTest {

    // ========== zhenying() 阵营判断 ==========

    @Test
    void zhenying_villageRoles_shouldReturn0() {
        assertEquals(0, GameLogicUtils.zhenying(1), "占卜师应为村侧");
        assertEquals(0, GameLogicUtils.zhenying(2), "灵媒师应为村侧");
        assertEquals(0, GameLogicUtils.zhenying(3), "猎人应为村侧");
        assertEquals(0, GameLogicUtils.zhenying(4), "共有者应为村侧");
        assertEquals(0, GameLogicUtils.zhenying(5), "猫又应为村侧");
        assertEquals(0, GameLogicUtils.zhenying(6), "村人应为村侧");
    }

    @Test
    void zhenying_nonHumanWolfFaction_shouldReturn1() {
        assertEquals(1, GameLogicUtils.zhenying(7), "人狼应为非人(人狼阵营)");
        assertEquals(1, GameLogicUtils.zhenying(8), "狂人应为非人(人狼阵营)");
        assertEquals(1, GameLogicUtils.zhenying(9), "狂信者应为非人(人狼阵营)");
    }

    @Test
    void zhenying_nonHumanFoxFaction_shouldReturnMinus1() {
        assertEquals(-1, GameLogicUtils.zhenying(10), "妖狐应为非人(妖狐阵营)");
        assertEquals(-1, GameLogicUtils.zhenying(11), "背德者应为非人(妖狐阵营)");
    }

    // ========== feiren() 非人类型 ==========

    @Test
    void feiren_wolf_shouldReturn1() {
        assertEquals(1, GameLogicUtils.feiren(7), "人狼类型应为1");
    }

    @Test
    void feiren_otherNonHuman_shouldReturnMinus1() {
        assertEquals(-1, GameLogicUtils.feiren(8), "狂人应为非人(人狼阵营)");
        assertEquals(-1, GameLogicUtils.feiren(9), "狂信者应为非人(人狼阵营)");
        assertEquals(-1, GameLogicUtils.feiren(10), "妖狐应为非人(妖狐阵营)");
        assertEquals(-1, GameLogicUtils.feiren(11), "背德者应为非人(妖狐阵营)");
    }

    // ========== whyDie.isDayDeath() ==========

    @Test
    void isDayDie_chuxing_shouldReturnTrue() {
        assertTrue(whyDie.chuxing.isDayDeath(), "处刑应为白天死亡");
    }

    @Test
    void isDayDie_beiyao_shouldReturnFalse() {
        assertFalse(whyDie.beiyao.isDayDeath(), "被咬应为夜晚死亡");
    }

}
