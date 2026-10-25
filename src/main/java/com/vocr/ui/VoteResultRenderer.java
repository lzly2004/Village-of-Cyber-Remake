package com.vocr.ui;

import com.vocr.application.PlayerViewSnapshot;
import com.vocr.application.VisiblePlayer;
import com.vocr.application.VoteLineSnapshot;
import com.vocr.application.VoteResultSnapshot;
import com.vocr.application.VoteRoundSnapshot;
import com.vocr.application.VotingMethod;
import com.vocr.logging.DebugLogger;

/** 只负责把应用层已经计票完成的权威投票快照渲染到 Swing。 */
class VoteResultRenderer
{
    static boolean renderPiao(
            UI ui,
            PlayerViewSnapshot view,
            VoteResultSnapshot result,
            String title,
            int round,
            GameSceneVoteHandler handler)
    {
        ui.piaoText.setVisible(true);
        ui.piaoText1.setVisible(true);
        VoteRoundSnapshot roundResult = round(result, round);
        StringBuilder leftPiao = new StringBuilder(title);
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = buildVotePanels(ui, view, roundResult, leftPiao, rightPiao);
        for (int index = 0; index < 10 - leftCnt; index++)
            leftPiao.append("\n");
        if (roundResult.tied())
        {
            leftPiao.append(GameStrings.VOTE_TIE);
        }
        else
        {
            int executedPlayer = roundResult.soleLeader().orElseThrow();
            leftPiao.append(String.format(
                    GameStrings.VOTE_RESULT_FORMAT,
                    roundResult.maximumVotes(),
                    playerName(ui, view, executedPlayer)));
            ui.chuxingWho = executedPlayer;
        }
        renderVoteResult(ui, handler, leftPiao, rightPiao, 900, 530);
        return roundResult.tied();
    }

    static void renderDayPiao(
            UI ui,
            PlayerViewSnapshot view,
            VoteResultSnapshot result,
            int round,
            GameSceneVoteHandler handler)
    {
        ui.piaoText.setVisible(true);
        ui.piaoText1.setVisible(true);
        VoteRoundSnapshot roundResult = round(result, round);
        String extraText = buildDailyVotingRuleText(ui, view, result);
        StringBuilder leftPiao = new StringBuilder(
                GameStrings.getVoteHistoryTitle(result.day(), round, extraText));
        StringBuilder rightPiao = new StringBuilder("\n\n");
        buildVotePanels(ui, view, roundResult, leftPiao, rightPiao);
        renderVoteResult(ui, handler, leftPiao, rightPiao, 1000, 400);
    }

    private static VoteRoundSnapshot round(VoteResultSnapshot result, int round)
    {
        if (round <= 0 || round > result.rounds().size())
            throw new IllegalArgumentException("投票轮次超出权威结果范围: " + round);
        return result.rounds().get(round - 1);
    }

    private static int buildVotePanels(
            UI ui,
            PlayerViewSnapshot view,
            VoteRoundSnapshot round,
            StringBuilder leftPiao,
            StringBuilder rightPiao)
    {
        int leftCnt = 0;
        for (VoteLineSnapshot line : round.lines())
        {
            String text = String.format(
                    GameStrings.VOTE_LINE_FORMAT,
                    playerName(ui, view, line.playerNumber()),
                    line.receivedVotes(),
                    playerName(ui, view, line.targetPlayerNumber()));
            if (leftCnt >= 10)
                rightPiao.append(text);
            else
            {
                leftPiao.append(text);
                leftCnt++;
            }
        }
        return leftCnt;
    }

    private static String buildDailyVotingRuleText(
            UI ui, PlayerViewSnapshot view, VoteResultSnapshot result)
    {
        StringBuilder extraText = new StringBuilder();
        if (result.method() == VotingMethod.FREE)
            return GameStrings.VOTE_FREE;
        if (result.method() == VotingMethod.GREY)
            extraText.append(GameStrings.VOTE_GREY);
        else
            extraText.append(GameStrings.VOTE_DESIGNATED);
        for (int candidate : result.candidates())
        {
            extraText.append(playerName(ui, view, candidate));
            if (result.method() == VotingMethod.DESIGNATED)
                extraText.append(",");
        }
        return extraText.toString();
    }

    private static String playerName(UI ui, PlayerViewSnapshot view, int playerNumber)
    {
        VisiblePlayer player = view.players().stream()
                .filter(candidate -> candidate.playerNumber() == playerNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "玩家视图缺少投票玩家: " + playerNumber));
        return ui.uiComponentFactory.getJobText(player.character().ordinal());
    }

    private static void renderVoteResult(
            UI ui,
            GameSceneVoteHandler handler,
            StringBuilder leftPiao,
            StringBuilder rightPiao,
            int leftWidth,
            int rightX)
    {
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);
        handler.stylePiaoTextArea(
                ui.piaoText, leftPiao.toString(), 40, 228, leftWidth, 430);
        handler.stylePiaoTextArea(
                ui.piaoText1, rightPiao.toString(), rightX, 228, 450, 430);
        ui.resizeComponents();
    }
}
