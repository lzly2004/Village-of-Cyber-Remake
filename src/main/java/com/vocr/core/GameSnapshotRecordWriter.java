package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.config.GameConstants;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.IntFunction;

/** 将游戏状态与投票状态编码为 Replay 使用的文本记录。 */
final class GameSnapshotRecordWriter
{
    private final List<String> records;

    GameSnapshotRecordWriter(List<String> records)
    {
        this.records = records;
    }

    void writeDailySnapshot(int day, GameContext ctx)
    {
        int playerSum = ctx.getPlayerSum();
        int aliveCount = 0;
        for (int player = 1; player <= playerSum; player++)
        {
            if (ctx.isAlive(player)) aliveCount++;
        }
        records.add("DAILY_SNAPSHOT|day=" + day + "|alive=" + aliveCount);

        String stateLine = buildStateLine(day, playerSum, player -> {
            int characterNumber = ctx.getCharacterNumber(player);
            if (characterNumber == 0)
                DebugLogger.warn("[GameRecorder] ⚠️ 玩家" + player + "的characterNumber=0!");
            return String.format("%d:n=%d,r=%d,cr=%d,cro=%d,dd=%d,wd=%d,nm=%d,cod=%d,st=%d,sh=%s",
                    player, characterNumber, ctx.getActualRole(player), ctx.getClaimedRole(player),
                    ctx.getClaimedRoleOrder(player), ctx.getDeathDay(player),
                    ctx.getDeathReason(player).ordinal(), ctx.isNonHumanMarked(player) ? 1 : 0,
                    ctx.getComingOutDay(player), ctx.getSkillTarget(player, day - 1),
                    buildSkillHistory(ctx, player, day));
        });
        records.add(stateLine);
        DebugLogger.info("[GameRecorder] STATE行: " + stateLine);
        DebugLogger.info("[GameRecorder] 每日快照已记录: day=" + day + " alive=" + aliveCount);
    }

    /**
     * 快照同时携带截至当日开始前的完整技能历史。
     *
     * 旧的 st 字段只保存 day - 1 的单个结果，无法表达晚 CO 对更早猎人日记的回填；
     * sh 保留按技能日索引的完整历史，且不移除 st，以继续兼容旧 Replay。
     */
    private String buildSkillHistory(GameContext ctx, int player, int snapshotDay)
    {
        StringJoiner history = new StringJoiner("/");
        history.add("0"); // 保留下标 0，使数组下标与游戏日期一致。
        for (int skillDay = 1; skillDay < snapshotDay; skillDay++)
            history.add(Integer.toString(ctx.getSkillTarget(player, skillDay)));
        return history.toString();
    }

    void writeVoteData(int day, GameContext ctx)
    {
        int dailyVotingRule = ctx.getVotingRule(day);
        List<Integer> votingCandidates = ctx.getVotingCandidates(day);
        StringBuilder method = new StringBuilder("VOTE_METHOD|day=" + day + "|rule=" + dailyVotingRule);
        if (!votingCandidates.isEmpty())
        {
            method.append("|grey=");
            for (int candidate : votingCandidates) method.append(candidate).append(",");
            method.setLength(method.length() - 1);
        }

        StringBuilder designatedTargets = new StringBuilder();
        for (int player = 1; player <= ctx.getPlayerSum(); player++)
        {
            if (ctx.isSelectedVoteTarget(player, day))
                designatedTargets.append(ctx.getCharacterNumber(player)).append(",");
        }
        if (designatedTargets.length() > 0)
        {
            designatedTargets.setLength(designatedTargets.length() - 1);
            method.append("|design=").append(designatedTargets);
        }
        records.add(method.toString());
        DebugLogger.info("[GameRecorder] " + method);

        int playerSum = ctx.getPlayerSum();
        for (int round = 1; round <= GameConstants.MAX_VOTE_ROUNDS; round++)
        {
            boolean hasVote = false;
            int validVoteCount = 0;
            StringBuilder votes = new StringBuilder("VOTE_DETAIL|day=" + day + "|round=" + round + "|");
            for (int player = 1; player <= playerSum; player++)
            {
                int target = ctx.getVoteTarget(player, day, round);
                if (target > 0 || ctx.getDeathDay(player) == day)
                {
                    votes.append(player).append("->").append(target).append(",");
                    hasVote = true;
                    if (target > 0) validVoteCount++;
                }
            }
            if (hasVote && validVoteCount > 0)
            {
                votes.setLength(votes.length() - 1);
                records.add(votes.toString());
                DebugLogger.info("[GameRecorder] VOTE_DETAIL: " + votes);
            }
        }
    }

    private String buildStateLine(int day, int playerSum, IntFunction<String> playerDataProvider)
    {
        StringBuilder state = new StringBuilder("STATE|day=" + day + "|");
        for (int player = 1; player <= playerSum; player++)
            state.append("{").append(playerDataProvider.apply(player)).append("}");
        return state.toString();
    }
}
