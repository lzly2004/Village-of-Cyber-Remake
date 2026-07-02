import java.util.ArrayList;
import java.util.List;

/**
 * 投票结果渲染器 —— 从 GameSceneVoteHandler.createPiao()/createDayPiao() 提取。
 * 负责渲染投票结果面板（票数统计、处刑判定、再投票逻辑）。
 */
class VoteResultRenderer {

    static void renderPiao(UI ui, String str, int round, boolean[] isReVote, GameSceneVoteHandler handler) {
        ui.piaoText.setVisible(true);
        ui.piaoText1.setVisible(true);
        int gameDay = ui.ctx.getEffectiveGameDay();
        int playerSum = ui.ctx.getPlayerSum() + 1;
        int[][] voteTotal = buildVoteTotal(ui, playerSum, gameDay, round, true);
        StringBuilder leftPiao = new StringBuilder(str);
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = buildVotePanels(ui, playerSum, gameDay, round, voteTotal, leftPiao, rightPiao, true);
        int max = voteTotal[1][round];
        for (int i = 2; i < playerSum; ++i)
            if (voteTotal[i][round] > max) max = voteTotal[i][round];
        int maxCnt = 0;
        List<Integer> maxPos = new ArrayList<>();
        for (int i = 1; i < playerSum; ++i)
            if (voteTotal[i][round] == max) { maxCnt++; maxPos.add(i); }
        for (int i1 = 0; i1 < 10 - leftCnt; ++i1) leftPiao.append("\n");
        if (maxCnt == 1) {
            leftPiao.append(String.format(GameStrings.VOTE_RESULT_FORMAT, max, ui.getJobText(maxPos.get(0))));
            isReVote[0] = false;
            ui.chuxingWho = maxPos.get(0);
        } else {
            leftPiao.append(GameStrings.VOTE_TIE);
            isReVote[0] = true;
        }
        renderVoteResult(ui, handler, leftPiao, rightPiao, 900, 530);
    }

    static void renderDayPiao(UI ui, int round, int gameDay, int dailyVotingRule, GameSceneVoteHandler handler) {
        ui.piaoText.setVisible(true);
        ui.piaoText1.setVisible(true);
        int playerSum = ui.ctx.getPlayerSum() + 1;
        StringBuilder extraText = buildDailyVotingRuleText(ui, playerSum, gameDay, dailyVotingRule);
        int[][] voteTotal = buildVoteTotal(ui, playerSum, gameDay, round, false);
        StringBuilder leftPiao = new StringBuilder(GameStrings.getVoteHistoryTitle(gameDay, round, extraText.toString()));
        StringBuilder rightPiao = new StringBuilder("\n\n");
        buildVotePanels(ui, playerSum, gameDay, round, voteTotal, leftPiao, rightPiao, false);
        renderVoteResult(ui, handler, leftPiao, rightPiao, 1000, 400);
    }

    private static int[][] buildVoteTotal(UI ui, int playerSum, int gameDay, int round, boolean exactDayMatch) {
        int[][] voteTotal = new int[playerSum][4];
        for (int i = 1; i < playerSum; ++i) {
            boolean isParticipated = ui.ctx.isAlive(i) || 
                    (exactDayMatch ? ui.ctx.getDeathDay(i) == gameDay : ui.ctx.getDeathDay(i) >= gameDay);
            if (isParticipated) {
                voteTotal[ui.ctx.getVoteTarget(i, gameDay, round)][round]++;
            }
        }
        return voteTotal;
    }

    private static int buildVotePanels(UI ui, int playerSum, int gameDay, int round, int[][] voteTotal,
                                        StringBuilder leftPiao, StringBuilder rightPiao, boolean exactDayMatch) {
        int leftCnt = 0;
        for (int i = 1; i < playerSum; ++i) {
            boolean isParticipated = ui.ctx.isAlive(i) || 
                    (exactDayMatch ? ui.ctx.getDeathDay(i) == gameDay : ui.ctx.getDeathDay(i) >= gameDay);
            if (isParticipated) {
                String line = formatVoteLine(ui, i, round, gameDay, voteTotal);
                if (leftCnt >= 10) rightPiao.append(line);
                else { leftPiao.append(line); leftCnt++; }
            }
        }
        return leftCnt;
    }

    private static StringBuilder buildDailyVotingRuleText(UI ui, int playerSum, int gameDay, int dailyVotingRule) {
        StringBuilder extraText = new StringBuilder();
        switch (dailyVotingRule) {
            case 0: extraText.append(GameStrings.VOTE_FREE); break;
            case 1:
                extraText.append(GameStrings.VOTE_GREY);
                for (int i = 0; i < playerSum; ++i)
                    if (ui.greyCharas[i][gameDay] != 0)
                        extraText.append(ui.getJobText(ui.greyCharas[i][gameDay]));
                break;
            case 2:
                extraText.append(GameStrings.VOTE_DESIGNATED);
                for (int i = 0; i < playerSum; ++i)
                    if (ui.isSelectedVoteTargetCharas[i][gameDay] != 0)
                        extraText.append(ui.getJobText(ui.isSelectedVoteTargetCharas[i][gameDay])).append(",");
                break;
        }
        return extraText;
    }

    private static void renderVoteResult(UI ui, GameSceneVoteHandler handler, 
                                         StringBuilder leftPiao, StringBuilder rightPiao,
                                         int leftWidth, int rightX) {
        handler.stylePiaoTextArea(ui.piaoText, leftPiao.toString(), 40, 228, leftWidth, 430);
        handler.stylePiaoTextArea(ui.piaoText1, rightPiao.toString(), rightX, 228, 450, 430);
        ui.resizeComponents();
    }

    private static String formatVoteLine(UI ui, int i, int round, int gameDay, int[][] voteTotal) {
        return String.format(GameStrings.VOTE_LINE_FORMAT,
                ui.getJobText(i),
                voteTotal[i][round],
                ui.getJobText(ui.ctx.getVoteTarget(i, gameDay, round)));
    }
}