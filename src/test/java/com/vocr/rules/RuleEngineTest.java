package com.vocr.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineTest
{
    @Test
    void apply_shouldPreserveRuleOrderAndClampFinalWeights()
    {
        RuleEngine engine = new RuleEngine();
        engine.addSimpleRule("simple", index -> index == 1 || index == 2 || index == 4,
                index -> switch (index)
                {
                    case 1 -> 20;
                    case 2 -> 10;
                    case 4 -> 5;
                    default -> 0;
                });
        engine.addComplexRule("complex", weights ->
        {
            weights[1] += 5;
            weights[2] += 10;
            weights[3] = 101;
            weights[4] *= 2;
        });

        int[] weights = {0, 90, 480, 0, 10};
        engine.apply(weights);

        assertArrayEquals(new int[]{0, 100, 999, 100, 30}, weights);
        assertEquals(2, engine.ruleCount());
    }

    @Test
    void registration_shouldRejectRulesWithoutExecutableBehavior()
    {
        RuleEngine engine = new RuleEngine();

        assertThrows(NullPointerException.class,
                () -> engine.addSimpleRule("missing condition", null, index -> 1));
        assertThrows(NullPointerException.class,
                () -> engine.addSimpleRule("missing adjustment", index -> true, null));
        assertThrows(NullPointerException.class,
                () -> engine.addComplexRule("missing action", null));
        assertEquals(0, engine.ruleCount());
    }
}
