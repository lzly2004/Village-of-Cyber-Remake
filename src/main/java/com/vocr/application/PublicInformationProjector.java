package com.vocr.application;

import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.whyDie;
import java.util.ArrayList;
import java.util.List;
import static com.vocr.application.PublicInformation.*;

/** 只投影桌面实际展示的内容。日期/CO筛选与WEB-013审计保持一致。 */
final class PublicInformationProjector
{
    private PublicInformationProjector() { }

    static PublicInformation project(GameContextView context, BusinessPhase phase)
    {
        boolean running = phase == BusinessPhase.DAY_DECISION;
        return new PublicInformation(running ? reports(context) : List.of(),
                suspicion(context, running && context.getGameDay() > 2), deaths(context));
    }

    private static List<SkillReport> reports(GameContextView context)
    {
        List<SkillReport> reports = new ArrayList<>();
        int count = context.getPlayerSum();
        for (int player = 1; player <= count; player++)
        {
            int role = context.getClaimedRole(player);
            if (role < 1 || role > 3) continue;
            List<SkillResult> results = new ArrayList<>();
            int deathDay = context.getDeathDay(player);
            for (int day = role == 1 ? 1 : 2; day < context.getGameDay(); day++)
            {
                if (deathDay > 0 && day >= deathDay) break;
                int raw = context.getSkillTarget(player, day);
                if (raw <= 0) continue;
                int target = raw > count ? raw - count : raw;
                if (target < 1 || target > count) throw new IllegalStateException("公开技能目标越界");
                results.add(new SkillResult(day, target, role == 3 ? ReportColor.NOT_APPLICABLE
                        : raw > count ? ReportColor.BLACK : ReportColor.WHITE));
            }
            reports.add(new SkillReport(player, Role.values()[role], context.getClaimedRoleOrder(player),
                    context.isNonHumanMarked(player), results));
        }
        return reports;
    }

    private static SuspicionSummary suspicion(GameContextView context, boolean available)
    {
        if (!available) return new SuspicionSummary(false, List.of());
        List<SuspicionRow> rows = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            if (!context.isAlive(player)) continue;
            List<Integer> targets = new ArrayList<>();
            for (int rank = 1; rank <= 3; rank++)
            {
                int target = context.getTop3SuspectedPlayer(player, rank, context.getGameDay());
                if (target != 0) targets.add(target);
            }
            List<Comparison> comparisons = new ArrayList<>();
            for (int index = 0; index + 1 < targets.size(); index++)
            {
                int gap = context.getSuspicionValue(player, targets.get(index))
                        - context.getSuspicionValue(player, targets.get(index + 1));
                comparisons.add(gap <= 2 ? Comparison.CLOSE : gap <= 5 ? Comparison.SLIGHTLY_GREATER
                        : gap <= 10 ? Comparison.GREATER : Comparison.MUCH_GREATER);
            }
            rows.add(new SuspicionRow(player, targets, comparisons));
        }
        return new SuspicionSummary(true, rows);
    }

    private static List<DeathDay> deaths(GameContextView context)
    {
        List<DeathDay> history = new ArrayList<>();
        for (int day = 1; day < context.getGameDay(); day++)
        {
            List<Execution> executions = new ArrayList<>();
            List<Integer> nightDeaths = new ArrayList<>();
            for (int player = 1; player <= context.getPlayerSum(); player++)
            {
                if (context.getDeathDay(player) != day) continue;
                whyDie reason = context.getDeathReason(player);
                switch (reason)
                {
                    case chuxing -> {
                        // 与桌面处刑历史的狐/猫特殊拼接条件相同；不传出实际职业或核心索引。
                        int actualRole = context.getActualRole(player);
                        int deviant = actualRole == 10 ? context.getDeviant() : 0;
                        if (actualRole != 5 && (actualRole != 10 || (deviant > 0 && !context.isAlive(deviant)
                                && context.getDeathDay(deviant) < day)))
                            executions.add(new Execution(player, PublicDeathReason.EXECUTION, List.of()));
                    }
                    case daymaozhou, dayhouzhui -> {
                        int prefix = reason == whyDie.daymaozhou ? context.getCat() : context.getFox();
                        executions.add(new Execution(player, reason == whyDie.daymaozhou
                                ? PublicDeathReason.DAY_CAT_CURSE : PublicDeathReason.DAY_PURSUED,
                                prefix > 0 ? List.of(prefix) : List.of()));
                    }
                    default -> {
                        if (reason.isNightDeath()) nightDeaths.add(player);
                    }
                }
            }
            history.add(new DeathDay(day, executions, nightDeaths));
        }
        return history;
    }
}
