import java.util.function.*;

/**
 * 单条权重规则 — 描述 "满足某条件时，对权重做何种修正"。
 */
public class WeightRule
{
    private final String name;
    private final IntPredicate condition;
    private final IntUnaryOperator adjustment;
    private final Consumer<int[]> complexAction;

    private WeightRule(String name, IntPredicate condition, IntUnaryOperator adjustment,
                       Consumer<int[]> complexAction)
    {
        this.name = name;
        this.condition = condition;
        this.adjustment = adjustment;
        this.complexAction = complexAction;
    }

    public static WeightRule simple(String name, IntPredicate condition,
                                    IntUnaryOperator adjustment)
    {
        return new WeightRule(name, condition, adjustment, null);
    }

    public static WeightRule complex(String name, Consumer<int[]> action)
    {
        return new WeightRule(name, null, null, action);
    }

    public boolean isSimple() { return condition != null; }
    public String getName() { return name; }
    public boolean appliesTo(int i) { return condition.test(i); }
    public int getAdjustment(int i) { return adjustment.applyAsInt(i); }
    public void applyComplex(int[] weights) { complexAction.accept(weights); }
}
