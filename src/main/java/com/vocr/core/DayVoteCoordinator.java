package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.config.GameConstants;
import com.vocr.domain.CharacterKanjiName;

import java.util.List;

/** 准备当日投票候补、执行多轮投票并记录结果。 */
final class DayVoteCoordinator
{
    private final GameContext ctx;
    private final VoteSelector voteSelector;
    private final GameRecorder recorder;

    DayVoteCoordinator(GameContext ctx, VoteSelector voteSelector, GameRecorder recorder)
    {
        this.ctx = ctx;
        this.voteSelector = voteSelector;
        this.recorder = recorder;
    }

    void logSkillSchedule(int playerSum, int gameDay)
    {
        for (int claimedSeer : ctx.getZhans())
        {
            DebugLogger.log(GameLogicUtils.buildSkillScheduleLog(
                    "占卜师候补编号：" + ctx.getClaimedRoleOrder(claimedSeer) + "预告情况：",
                    playerSum, target -> ctx.isClaimedRoleScheduled(claimedSeer, target, gameDay)));
        }
        for (int claimedHunter : ctx.getLies())
        {
            DebugLogger.log(GameLogicUtils.buildSkillScheduleLog(
                    "猎人候补编号：" + ctx.getClaimedRoleOrder(claimedHunter) + "指定护卫情况：",
                    playerSum, target -> ctx.isClaimedRoleScheduled(claimedHunter, target, gameDay)));
        }
        DebugLogger.log(GameLogicUtils.buildSkillScheduleLog("潜伏占预告情况：", playerSum,
                target -> ctx.isHiddenSeerScheduled(target, gameDay)));
        DebugLogger.log(GameLogicUtils.buildSkillScheduleLog("潜伏猎人指定护卫情况：", playerSum,
                target -> ctx.isHiddenHunterScheduled(target, gameDay)));
    }

    boolean[] prepareVotablePlayers(int playerSum, int gameDay,
                                     int dailyVotingRule, List<Integer> executionCandidates)
    {
        boolean[] votable = new boolean[playerSum + 1];
        boolean haveVotable = false;
        if (dailyVotingRule == 1)
        {
            for (int player = 1; player <= playerSum; player++)
            {
                if (ctx.isAlive(player)
                        && (ctx.getClaimedRole(player) == 6 || ctx.getClaimedRole(player) == 0))
                    votable[player] = true;
            }
            for (int claimedSeer : ctx.getZhans())
            {
                for (int day = 1; day < gameDay; day++)
                {
                    int target = ctx.getSkillTarget(claimedSeer, day);
                    if (target > 0)
                        votable[ctx.getActualTarget(target)] = false;
                }
            }
            for (int player = 1; player <= playerSum; player++)
            {
                if (votable[player]) haveVotable = true;
            }
        }
        else if (dailyVotingRule == 2)
        {
            for (int player : executionCandidates)
            {
                if (ctx.isAlive(player))
                {
                    votable[player] = true;
                    haveVotable = true;
                }
            }
        }
        else if (!haveVotable)
        {
            for (int player = 1; player <= playerSum; player++)
            {
                if (ctx.isAlive(player))
                {
                    votable[player] = true;
                    haveVotable = true;
                }
            }
        }
        return votable;
    }

    int conductVoting(int playerSum, int gameDay,
                      int dailyVotingRule, List<Integer> executionCandidates)
    {
        boolean[] votable = prepareVotablePlayers(
                playerSum, gameDay, dailyVotingRule, executionCandidates);
        int votingRound = 1;
        int executedPlayer;
        while (true)
        {
            executedPlayer = voteSelector.select(votingRound, votable);
            if (executedPlayer != 0) break;
            if (votingRound < GameConstants.MAX_VOTE_ROUNDS)
                votingRound++;
        }
        ctx.completeVoting(gameDay, dailyVotingRule, executionCandidates, votingRound);
        logVotingResult(playerSum, gameDay, votingRound);
        recordReplayVoteData(gameDay, votingRound);
        return executedPlayer;
    }

    private void logVotingResult(int playerSum, int gameDay, int votingRound)
    {
        DebugLogger.log("投票结束，一共投票了" + votingRound + "轮" + "\n票型：\n");
        for (int round = 1; round <= votingRound; round++)
        {
            DebugLogger.log("第" + round + "轮投票结果：");
            int[] receivedVotes = new int[playerSum + 1];
            for (int player = 1; player <= playerSum; player++)
                receivedVotes[ctx.getVoteTarget(player, gameDay, round)]++;
            for (int player = 1; player <= playerSum; player++)
            {
                if (ctx.isDead(player)) continue;
                DebugLogger.log(CharacterKanjiName.values()[ctx.getCharacterNumber(player)]
                        + "" + receivedVotes[player] + "票 投票先 "
                        + CharacterKanjiName.values()[ctx.getCharacterNumber(
                                ctx.getVoteTarget(player, gameDay, round))]);
            }
        }
    }

    private void recordReplayVoteData(int gameDay, int votingRound)
    {
        try
        {
            if (recorder != null)
            {
                recorder.recordVoteData(gameDay, ctx);
                DebugLogger.info("[Replay] 投票数据已记录: day=" + gameDay + ", rounds=" + votingRound);
            }
        }
        catch (Exception e)
        {
            DebugLogger.warn("[Replay] 记录投票数据失败: " + e.getMessage());
        }
    }
}
