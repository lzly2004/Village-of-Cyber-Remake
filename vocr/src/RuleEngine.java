import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

/**
 * 权重规则引擎 — 标准化 "初始化 → 规则修正 → 加权选择" 决策管道。
 *
 * 规则分为两种：
 *   - 简单规则：对每个玩家 i，若条件成立则 weight[i] += 修正值
 *   - 复杂规则：对权重数组做任意修改（如多玩家交互修正）
 *
 * 自动处理权重收束：
 *   MAXN(100) < w < INFJ(500) → 收束为 MAXN
 *   INFJ(500) ≤ w < INF(999) → 收束为 INF
 */
public class RuleEngine
{
    private final List<WeightRule> rules = new ArrayList<>();

    public void addSimpleRule(String name, IntPredicate condition, IntUnaryOperator adjustment)
    {
        rules.add(WeightRule.simple(name, condition, adjustment));
    }

    public void addComplexRule(String name, Consumer<int[]> action)
    {
        rules.add(WeightRule.complex(name, action));
    }

    public void apply(int[] weights)
    {
        for (WeightRule rule : rules)
        {
            if (rule.isSimple())
            {
                for (int i = 1; i < weights.length; i++)
                {
                    if (rule.appliesTo(i))
                        weights[i] += rule.getAdjustment(i);
                }
            }
            else
            {
                rule.applyComplex(weights);
            }
        }
        clamp(weights);
    }

    private void clamp(int[] weights)
    {
        for (int i = 1; i < weights.length; i++)
        {
            int w = weights[i];
            if (w > GameConstants.MAXN && w < GameConstants.INFJ)
                weights[i] = GameConstants.MAXN;
            else if (w >= GameConstants.INFJ && w < GameConstants.INF)
                weights[i] = GameConstants.INF;
        }
    }

    public int ruleCount() { return rules.size(); }
}