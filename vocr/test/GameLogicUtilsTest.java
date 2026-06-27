import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;

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

    // ========== probabilityJudge() ==========

    @RepeatedTest(100)
    void probabilityJudge_100_shouldAlwaysReturnTrue() {
        assertTrue(GameLogicUtils.probabilityJudge(100), "100%概率应始终返回true");
    }

    @RepeatedTest(100)
    void probabilityJudge_0_shouldAlwaysReturnFalse() {
        assertFalse(GameLogicUtils.probabilityJudge(0), "0%概率应始终返回false");
    }

    // ========== getEventIndexByProbability() ==========

    @Test
    void getEventIndexByProbability_singleItem_shouldReturn0() {
        ArrayList<Integer> probs = new ArrayList<>();
        probs.add(50);
        int result = GameLogicUtils.getEventIndexByProbability(probs);
        assertEquals(0, result, "单项应返回索引0");
    }

    @Test
    void getEventIndexByProbability_zeroWeight_shouldThrow() {
        ArrayList<Integer> probs = new ArrayList<>();
        probs.add(0);
        assertThrows(IllegalArgumentException.class,
            () -> GameLogicUtils.getEventIndexByProbability(probs),
            "零权重应抛IllegalArgumentException");
    }

    // ========== isDayDie() ==========

    @Test
    void isDayDie_chuxing_shouldReturnTrue() {
        assertTrue(GameLogicUtils.isDayDie(whyDie.chuxing), "处刑应为白天死亡");
    }

    @Test
    void isDayDie_beiyao_shouldReturnFalse() {
        assertFalse(GameLogicUtils.isDayDie(whyDie.beiyao), "被咬应为夜晚死亡");
    }
}