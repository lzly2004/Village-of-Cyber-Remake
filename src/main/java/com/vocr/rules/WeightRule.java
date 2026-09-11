package com.vocr.rules;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * 单条权重规则 — 简单规则和复杂规则是互斥的合法形态。
 */
public sealed interface WeightRule permits SimpleWeightRule, ComplexWeightRule
{
    String name();

    static WeightRule simple(String name, IntPredicate condition, IntUnaryOperator adjustment)
    {
        return new SimpleWeightRule(name, condition, adjustment);
    }

    static WeightRule complex(String name, Consumer<int[]> action)
    {
        return new ComplexWeightRule(name, action);
    }

    default String getName() { return name(); }
}

record SimpleWeightRule(String name, IntPredicate condition,
                        IntUnaryOperator adjustment) implements WeightRule
{
    SimpleWeightRule
    {
        Objects.requireNonNull(condition, "简单规则条件不能为空");
        Objects.requireNonNull(adjustment, "简单规则权重修正不能为空");
    }
}

record ComplexWeightRule(String name, Consumer<int[]> action) implements WeightRule
{
    ComplexWeightRule
    {
        Objects.requireNonNull(action, "复杂规则动作不能为空");
    }
}
