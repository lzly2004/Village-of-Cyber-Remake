import java.util.ArrayList;
import java.util.List;

/**
 * 投票结果渲染器 —— 从 GameSceneVoteHandler.createPiao()/createDayPiao() 提取。
 * 负责渲染投票结果面板（票数统计、处刑判定、再投票逻辑）。
 */
class VoteResultRenderer {

    static void renderPiao(UI ui, String str, int round, boolean[] isReVote, GameSceneVoteHandler handler) {
        DebugLogger.log("***当前gameDay等于" + ui.ctx.getGameDay() + "***");
        ui.piaoText.setVisible(true);
        ui.piaoText1.setVisible(true);
        int gameDay;
        if (ui.ctx.getEndResult() == 0) {
            gameDay = ui.ctx.getGameDay() - 1;
        } else {
            gameDay = ui.ctx.getGameDay();
        }
        int playerSum = ui.ctx.getPlayerSum() + 1;
        int[][] voteTotal = new int[playerSum][4];
        for (int i = 1; i < playerSum; ++i) {
            if (ui.ctx.isAlive(i) || ui.ctx.getDeathDay(i) == gameDay) {
                voteTotal[ui.ctx.getVoteTarget(i, gameDay, round)][round]++;
            }
        }
        StringBuilder leftPiao = new StringBuilder(str);
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        int max = voteTotal[1][round];
        for (int i = 2; i < playerSum; ++i) {
            if (voteTotal[i][round] > max) max = voteTotal[i][round];
        }
        int maxCnt = 0;
        List<Integer> maxPos = new ArrayList<>();
        for (int i = 1; i < playerSum; ++i) {
            if (voteTotal[i][round] == max) { maxCnt++; maxPos.add(i); }
        }
        for (int i = 1; i < playerSum; ++i) {
            if (ui.ctx.isAlive(i) || ui.ctx.getDeathDay(i) == gameDay) {
                String line = formatVoteLine(ui, i, round, gameDay, voteTotal);
                if (leftCnt >= 10) rightPiao.append(line);
                else { leftPiao.append(line); leftCnt++; }
            }
            if (i == playerSum - 1) {
                for (int i1 = 0; i1 < 10 - leftCnt; ++i1) leftPiao.append("\n");
                if (maxCnt == 1) {
                    leftPiao.append(String.format(GameStrings.VOTE_RESULT_FORMAT, max, ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(maxPos.get(0)))));
                    isReVote[0] = false;
                    ui.chuxingWho = maxPos.get(0);
                    maxPos.clear();
                } else {
                    leftPiao.append(GameStrings.VOTE_TIE);
                    isReVote[0] = true;
                    maxPos.clear();
                }
            }
        }
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);
        handler.stylePiaoTextArea(ui.piaoText, leftPiao.toString(), 40, 228, 900, 430);
        handler.stylePiaoTextArea(ui.piaoText1, rightPiao.toString(), 530, 228, 450, 430);
        ui.resizeComponents();
    }

    static void renderDayPiao(UI ui, int round, int gameDay, int dailyVotingRule, GameSceneVoteHandler handler) {
        ui.piaoText.setVisible(true);
        ui.piaoText1.setVisible(true);
        int playerSum = ui.ctx.getPlayerSum() + 1;
        int[][] voteTotal = new int[playerSum][4];
        for (int i = 1; i < playerSum; ++i) {
            if (ui.ctx.isAlive(i) || ui.ctx.getDeathDay(i) >= gameDay) {
                voteTotal[ui.ctx.getVoteTarget(i, gameDay, round)][round]++;
            }
        }
        StringBuilder extraText = new StringBuilder();
        switch (dailyVotingRule) {
            case 0:
                extraText.append(GameStrings.VOTE_FREE);
                break;
            case 1:
                extraText.append(GameStrings.VOTE_GREY);
                for (int i = 0; i < playerSum; ++i) {
                    if (ui.greyCharas[i][gameDay] != 0) {
                        extraText.append(ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(ui.greyCharas[i][gameDay])));
                    }
                }
                break;
            case 2:
                extraText.append(GameStrings.VOTE_DESIGNATED);
                for (int i = 0; i < playerSum; ++i) {
                    if (ui.isSelectedVoteTargetCharas[i][gameDay] != 0) {
                        extraText.append(ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(ui.isSelectedVoteTargetCharas[i][gameDay]))).append(",");
                    }
                }
                break;
        }
        StringBuilder leftPiao = new StringBuilder(String.format(GameStrings.VOTE_HISTORY_TITLE, gameDay, round, extraText.toString()));
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        for (int i = 1; i < playerSum; ++i) {
            if (ui.ctx.isAlive(i) || ui.ctx.getDeathDay(i) >= gameDay) {
                String line = formatVoteLine(ui, i, round, gameDay, voteTotal);
                if (leftCnt >= 10) rightPiao.append(line);
                else { leftPiao.append(line); leftCnt++; }
            }
        }
        DebugLogger.log(leftPiao);
            DebugLogger.log(rightPiao);
        handler.stylePiaoTextArea(ui.piaoText, leftPiao.toString(), 40, 228, 1000, 430);
        handler.stylePiaoTextArea(ui.piaoText1, rightPiao.toString(), 400, 228, 450, 430);
        ui.resizeComponents();
    }

    private static String formatVoteLine(UI ui, int i, int round, int gameDay, int[][] voteTotal) {
        return String.format(GameStrings.VOTE_LINE_FORMAT,
                ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(i)),
                voteTotal[i][round],
                ui.uiComponentFactory.getJobText(ui.ctx.getCharacterNumber(ui.ctx.getVoteTarget(i, gameDay, round))));
    }
}