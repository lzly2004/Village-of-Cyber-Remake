package com.vocr.ui;

import com.vocr.domain.CharacterKanjiName;
import com.vocr.replay.DaySnapshot;
import com.vocr.replay.ReplaySave;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将 Replay 快照转换为复盘界面文本，不持有或操作任何 Swing 组件。 */
final class ReplayTextFormatter
{
    private static final int[] ROLE_DISPLAY_ORDER = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    private ReplayTextFormatter() {}

    static String buildNormalInfo(ReplaySave replay, int day, int playerSum)
    {
        StringBuilder seerResults = new StringBuilder();
        StringBuilder mediumResults = new StringBuilder();
        StringBuilder executions = new StringBuilder();
        StringBuilder nightDeaths = new StringBuilder();
        StringBuilder hunterResults = new StringBuilder();

        DaySnapshot firstSnapshot = findSnapshot(replay, day + 1);
        if (firstSnapshot == null) firstSnapshot = findSnapshot(replay, day);
        if (firstSnapshot != null && firstSnapshot.players != null)
        {
            for (int index = 0; index < playerSum; index++)
            {
                DaySnapshot.PlayerStatus player = firstSnapshot.players[index];
                if (player == null) continue;
                switch (player.claimedRole)
                {
                    case 1 -> appendSkillResults(
                            replay, seerResults, player, index, playerSum, 1, day, true);
                    case 2 -> appendSkillResults(
                            replay, mediumResults, player, index, playerSum, 1, day, true);
                    case 3 -> appendSkillResults(
                            replay, hunterResults, player, index, playerSum, 2, day, false);
                    default -> { }
                }
            }
        }

        appendDeathHistory(replay, day, playerSum, executions, nightDeaths);

        return new StringBuilder()
                .append(GameStrings.SECTION_SEER).append(seerResults)
                .append(GameStrings.SECTION_MEDIUM).append(mediumResults)
                .append(GameStrings.SECTION_EXECUTION).append(executions)
                .append('\n').append(GameStrings.SECTION_BODY).append(nightDeaths)
                .append('\n').append(GameStrings.SECTION_GUARD).append(hunterResults)
                .toString();
    }

    static String buildRoleInfo(DaySnapshot snapshot, int playerSum)
    {
        StringBuilder result = new StringBuilder("一役職一\n");
        Map<Integer, List<String>> playersByRole = new LinkedHashMap<>();
        for (int index = 0; index < playerSum; index++)
        {
            DaySnapshot.PlayerStatus player = snapshot.players[index];
            if (player == null || player.characterNumber == 0) continue;
            playersByRole.computeIfAbsent(player.actualRole, ignored -> new ArrayList<>())
                    .add(shortName(player.characterNumber));
        }

        for (int role : ROLE_DISPLAY_ORDER)
        {
            List<String> players = playersByRole.get(role);
            if (players == null || players.isEmpty()) continue;
            result.append('[').append(actualRoleName(role)).append("]\n")
                    .append(String.join("　", players)).append('\n');
        }
        return result.toString();
    }

    static VoteText buildVoteInfo(DaySnapshot snapshot, int day, int requestedRound, int playerSum)
    {
        if (snapshot == null || snapshot.voteRounds == null || snapshot.voteRounds.isEmpty())
            throw new IllegalArgumentException("当前日期没有投票记录");

        int roundIndex = Math.min(requestedRound, snapshot.voteRounds.size() - 1);
        DaySnapshot.VoteRound voteRound = snapshot.voteRounds.get(roundIndex);
        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder("\n\n");
        StringBuilder extraText = new StringBuilder();

        if (snapshot.dailyVotingRule >= 0)
        {
            switch (snapshot.dailyVotingRule)
            {
                case 0 -> extraText.append(GameStrings.VOTE_FREE);
                case 1 -> {
                    extraText.append(GameStrings.VOTE_GREY);
                    for (int playerIndex : snapshot.greyTargetCharNums)
                        extraText.append(shortName(characterNumber(snapshot, playerIndex)));
                }
                case 2 -> {
                    extraText.append(GameStrings.VOTE_DESIGNATED);
                    for (int characterNumber : snapshot.designatedTargetCharNums)
                        extraText.append(shortName(characterNumber)).append(',');
                    if (!snapshot.designatedTargetCharNums.isEmpty())
                        extraText.setLength(extraText.length() - 1);
                }
                default -> { }
            }
        }

        left.append(GameStrings.getVoteHistoryTitle(day, voteRound.round, extraText.toString()));
        appendVotes(snapshot, day, playerSum, voteRound, left, right);
        return new VoteText(left.toString(), right.toString(), roundIndex);
    }

    private static void appendDeathHistory(ReplaySave replay, int day, int playerSum,
                                           StringBuilder executions, StringBuilder nightDeaths)
    {
        for (int deathDay = 1; deathDay < day; deathDay++)
        {
            DaySnapshot snapshot = findSnapshot(replay, deathDay + 1);
            if (snapshot == null || snapshot.players == null) continue;

            int nightDeathCount = 0;
            List<Integer> nightDeathCharacters = new ArrayList<>();
            boolean hasExecution = false;
            for (int index = 0; index < playerSum; index++)
            {
                DaySnapshot.PlayerStatus player = snapshot.players[index];
                if (player == null) continue;
                switch (player.deathReason)
                {
                    case 1 -> {
                        if (player.actualRole == 10)
                        {
                            int deviantIndex = findRoleIndex(snapshot, 11, playerSum);
                            if (deviantIndex >= 0 && snapshot.players[deviantIndex] != null
                                    && snapshot.players[deviantIndex].deathDay > 0
                                    && player.deathDay == deathDay
                                    && snapshot.players[deviantIndex].deathDay < player.deathDay)
                            {
                                executions.append(shortName(player.characterNumber));
                                hasExecution = true;
                            }
                        }
                        else if (player.actualRole != 5 && player.deathDay == deathDay)
                        {
                            executions.append(shortName(player.characterNumber));
                            hasExecution = true;
                        }
                    }
                    case 2 -> {
                        if (player.deathDay == deathDay)
                        {
                            String cat = findRole(snapshot, 5, playerSum);
                            if (!cat.isEmpty()) executions.append(cat).append('+');
                            executions.append(shortName(player.characterNumber)).append("(猫呪)");
                            hasExecution = true;
                        }
                    }
                    case 3 -> {
                        if (player.deathDay == deathDay)
                        {
                            String fox = findRole(snapshot, 10, playerSum);
                            if (!fox.isEmpty()) executions.append(fox).append('+');
                            executions.append(shortName(player.characterNumber)).append("(後追)");
                            hasExecution = true;
                        }
                    }
                    default -> {
                        if (player.deathReason >= 4 && player.deathDay == deathDay)
                        {
                            nightDeathCount++;
                            nightDeathCharacters.add(player.characterNumber);
                        }
                    }
                }

                if (index == playerSum - 1 && nightDeathCount == 0)
                    nightDeaths.append(GameStrings.PEACE_ARROW);
            }

            if (hasExecution) executions.append('→');
            if (nightDeathCount == 1)
                nightDeaths.append(shortName(nightDeathCharacters.get(0))).append('→');
            else if (nightDeathCount > 1)
            {
                for (int characterNumber : nightDeathCharacters)
                    nightDeaths.append(shortName(characterNumber)).append('+');
                nightDeaths.setLength(nightDeaths.length() - 1);
                nightDeaths.append('→');
            }
        }

        trimTrailingArrow(nightDeaths);
        trimTrailingArrow(executions);
    }

    private static void appendSkillResults(ReplaySave replay, StringBuilder result,
                                           DaySnapshot.PlayerStatus basePlayer, int playerArrayIndex,
                                           int playerSum, int startDay, int endDay,
                                           boolean showBallResult)
    {
        result.append(shortName(basePlayer.characterNumber)).append(" : ");
        boolean markedExposed = false;
        for (int skillDay = startDay; skillDay < endDay; skillDay++)
        {
            DaySnapshot snapshot = findSnapshot(replay, skillDay + 1);
            if (snapshot == null || snapshot.players == null
                    || playerArrayIndex >= snapshot.players.length) continue;
            DaySnapshot.PlayerStatus player = snapshot.players[playerArrayIndex];
            if (player == null) continue;
            if (player.deathDay > 0 && skillDay >= player.deathDay) break;

            int target = replay.getVisibleSkillTarget(player.playerIndex, skillDay, endDay);
            if (target != 0)
            {
                boolean blackResult = target > playerSum;
                int targetPlayer = blackResult ? target - playerSum : target;
                String targetName = shortName(characterNumber(snapshot, targetPlayer));
                result.append(targetName);
                if (showBallResult) result.append(blackResult ? '●' : '○');
                result.append('→');
            }

            DaySnapshot currentSnapshot = findSnapshot(replay, skillDay);
            if (!markedExposed && currentSnapshot != null && currentSnapshot.players != null
                    && playerArrayIndex < currentSnapshot.players.length)
            {
                DaySnapshot.PlayerStatus currentPlayer = currentSnapshot.players[playerArrayIndex];
                markedExposed = currentPlayer != null && currentPlayer.nonHumanMarked;
            }
        }

        if (!markedExposed)
        {
            DaySnapshot endSnapshot = findSnapshot(replay, endDay);
            if (endSnapshot != null && endSnapshot.players != null
                    && playerArrayIndex < endSnapshot.players.length)
            {
                DaySnapshot.PlayerStatus endPlayer = endSnapshot.players[playerArrayIndex];
                markedExposed = endPlayer != null && endPlayer.nonHumanMarked;
            }
        }
        if (markedExposed) result.append(GameStrings.MARKER_EXPOSED).append('→');
        trimTrailingArrow(result);
        result.append('\n');
    }

    private static void appendVotes(DaySnapshot snapshot, int day, int playerSum,
                                    DaySnapshot.VoteRound voteRound,
                                    StringBuilder left, StringBuilder right)
    {
        if (voteRound.votes == null) return;

        int[] voterTargets = new int[playerSum + 1];
        int[] voteCounts = new int[playerSum + 1];
        for (int[] vote : voteRound.votes)
        {
            if (vote == null || vote.length < 2) continue;
            int voter = vote[0];
            int target = vote[1];
            if (voter >= 1 && voter <= playerSum && target >= 1)
            {
                voterTargets[voter] = target;
                voteCounts[target]++;
            }
        }

        int leftCount = 0;
        for (int playerIndex = 1; playerIndex <= playerSum; playerIndex++)
        {
            if (snapshot.players == null || playerIndex > snapshot.players.length
                    || snapshot.players[playerIndex - 1] == null) continue;
            DaySnapshot.PlayerStatus player = snapshot.players[playerIndex - 1];
            boolean votingPlayer = player.deathDay == 0 || player.deathDay >= day;
            if (!votingPlayer && player.deathDay != day) continue;

            int targetIndex = voterTargets[playerIndex];
            String targetName;
            if (targetIndex >= 1 && targetIndex <= playerSum)
            {
                DaySnapshot.PlayerStatus target = targetIndex <= snapshot.players.length
                        ? snapshot.players[targetIndex - 1] : null;
                targetName = target != null ? shortName(target.characterNumber) : "?" + targetIndex;
            }
            else if (targetIndex == 0)
                targetName = "棄権";
            else
                continue;

            String line = String.format(GameStrings.VOTE_LINE_FORMAT,
                    shortName(player.characterNumber), voteCounts[playerIndex], targetName);
            if (leftCount >= 10) right.append(line);
            else
            {
                left.append(line);
                leftCount++;
            }
        }
    }

    private static DaySnapshot findSnapshot(ReplaySave replay, int day)
    {
        if (replay == null || replay.daySnapshots == null || replay.daySnapshots.isEmpty()) return null;
        for (DaySnapshot snapshot : replay.daySnapshots)
            if (snapshot != null && snapshot.dayNumber == day) return snapshot;

        int fallbackIndex = day - 1;
        if (fallbackIndex >= 0 && fallbackIndex < replay.daySnapshots.size())
            return replay.daySnapshots.get(fallbackIndex);
        return null;
    }

    private static String findRole(DaySnapshot snapshot, int role, int playerSum)
    {
        int index = findRoleIndex(snapshot, role, playerSum);
        return index >= 0 ? shortName(snapshot.players[index].characterNumber) : "";
    }

    private static int findRoleIndex(DaySnapshot snapshot, int role, int playerSum)
    {
        if (snapshot == null || snapshot.players == null) return -1;
        for (int index = 0; index < playerSum; index++)
        {
            DaySnapshot.PlayerStatus player = snapshot.players[index];
            if (player != null && player.actualRole == role) return index;
        }
        return -1;
    }

    private static int characterNumber(DaySnapshot snapshot, int playerIndex)
    {
        if (snapshot != null && snapshot.players != null
                && playerIndex >= 1 && playerIndex <= snapshot.players.length)
        {
            DaySnapshot.PlayerStatus player = snapshot.players[playerIndex - 1];
            if (player != null && player.characterNumber != 0) return player.characterNumber;
        }
        return playerIndex;
    }

    private static String shortName(int characterNumber)
    {
        if (characterNumber <= 0 || characterNumber >= CharacterKanjiName.values().length)
            return "?" + characterNumber;
        return CharacterKanjiName.values()[characterNumber].getShortName();
    }

    private static String actualRoleName(int role)
    {
        return switch (role)
        {
            case 1 -> "占卜师";
            case 2 -> "灵媒师";
            case 3 -> "猎人";
            case 4 -> "共有者";
            case 5 -> "猫又";
            case 6 -> "村人";
            case 7 -> "人狼";
            case 8 -> "狂人";
            case 9 -> "狂信者";
            case 10 -> "妖狐";
            case 11 -> "背德者";
            default -> "不明";
        };
    }

    private static void trimTrailingArrow(StringBuilder text)
    {
        if (text.length() > 2 && text.charAt(text.length() - 1) == '→')
            text.setLength(text.length() - 1);
    }

    record VoteText(String leftColumn, String rightColumn, int roundIndex) { }
}
