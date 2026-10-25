package com.vocr.application;

import com.vocr.domain.Role;
import java.util.List;
import java.util.Objects;

/** 经WEB-013审计的结构化公开信息；不携带核心分数或私有技能数组。 */
public record PublicInformation(List<SkillReport> skillReports, SuspicionSummary suspicion,
                                List<DeathDay> deathHistory)
{
    public PublicInformation
    {
        skillReports = List.copyOf(skillReports);
        Objects.requireNonNull(suspicion);
        deathHistory = List.copyOf(deathHistory);
    }

    public enum ReportColor { WHITE, BLACK, NOT_APPLICABLE }
    public enum Comparison { CLOSE, SLIGHTLY_GREATER, GREATER, MUCH_GREATER }

    public record SkillResult(int skillDay, int targetPlayerNumber, ReportColor color)
    {
        public SkillResult {
            if (skillDay < 1 || targetPlayerNumber < 1) throw new IllegalArgumentException("非法公开技能结果");
            Objects.requireNonNull(color);
        }
    }

    public record SkillReport(int playerNumber, Role claimedRole, int claimedRoleOrder,
                              boolean exposed, List<SkillResult> results)
    {
        public SkillReport {
            if (playerNumber < 1 || claimedRoleOrder < 1
                    || !List.of(Role.zhan, Role.ling, Role.lie).contains(claimedRole))
                throw new IllegalArgumentException("公开报告必须属于公开CO技能职业");
            results = List.copyOf(results);
            if (results.stream().anyMatch(result -> (claimedRole == Role.lie)
                    != (result.color() == ReportColor.NOT_APPLICABLE)))
                throw new IllegalArgumentException("护卫无黑白球，其他报告必须有球");
        }
    }

    public record SuspicionSummary(boolean available, List<SuspicionRow> rows)
    {
        public SuspicionSummary {
            rows = List.copyOf(rows);
            if (!available && !rows.isEmpty()) throw new IllegalArgumentException("未公开的怀疑摘要必须为空");
        }
    }

    public record SuspicionRow(int playerNumber, List<Integer> targets, List<Comparison> comparisons)
    {
        public SuspicionRow {
            targets = List.copyOf(targets);
            comparisons = List.copyOf(comparisons);
            if (playerNumber < 1 || targets.size() > 3 || targets.stream().anyMatch(target -> target < 1)
                    || targets.stream().distinct().count() != targets.size()
                    || comparisons.size() != Math.max(0, targets.size() - 1))
                throw new IllegalArgumentException("非法公开怀疑摘要");
        }
    }

    public record Execution(int playerNumber, PublicDeathReason cause, List<Integer> prefixPlayers)
    {
        public Execution {
            prefixPlayers = List.copyOf(prefixPlayers);
            if (playerNumber < 1 || !List.of(PublicDeathReason.EXECUTION, PublicDeathReason.DAY_CAT_CURSE,
                    PublicDeathReason.DAY_PURSUED).contains(cause) || prefixPlayers.size() > 1
                    || prefixPlayers.stream().anyMatch(player -> player < 1))
                throw new IllegalArgumentException("非法公开白天死亡记录");
        }
    }

    public record DeathDay(int day, List<Execution> executions, List<Integer> nightDeaths)
    {
        public DeathDay {
            if (day < 1) throw new IllegalArgumentException("非法死亡历史日期");
            executions = List.copyOf(executions);
            nightDeaths = List.copyOf(nightDeaths);
            if (nightDeaths.stream().anyMatch(player -> player < 1)) throw new IllegalArgumentException("非法夜间死者");
        }
    }
}
