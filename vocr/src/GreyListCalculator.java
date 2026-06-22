import java.util.ArrayList;
import java.util.List;

/**
 * 灰名单计算器 —— 从 GameSceneVoteHandler 提取的纯逻辑。
 * 无UI组件依赖，可直接单元测试。
 */
class GreyListCalculator {

    static class Result {
        final List<Integer> cxList;
        final List<Integer> beiZhan1;

        Result(List<Integer> cxList, List<Integer> beiZhan1) {
            this.cxList = cxList;
            this.beiZhan1 = beiZhan1;
        }
    }

    static Result compute(UI ui) {
        GameContextView ctx = ui.ctx;
        int playerSum = ctx.getPlayerSum();
        List<Integer> cxList = new ArrayList<>();
        List<Integer> beiZhan1 = new ArrayList<>();

        for (int j = 1; j < ctx.getGameDay(); ++j) {
            for (int k = 1; k <= playerSum; ++k) {
                if (ctx.getClaimedRole(k) == 1) {
                    int num = ctx.getSkillTarget(k, j);
                    if (num > playerSum) num -= playerSum;
                    if (!beiZhan1.contains(num)) { beiZhan1.add(num); DebugLogger.log(num); }
                }
            }
        }
        for (int i = 1; i <= playerSum; ++i) {
                DebugLogger.log("已进入灰循环" + ui.getJobText(i)
                        + " " + ctx.getDeathReason(i) + " " + ctx.getClaimedRole(i));
            if (ctx.getDeathReason(i) == whyDie.NONE
                    && (ctx.getClaimedRole(i) == 0 || ctx.getClaimedRole(i) == 6)) {
                if (beiZhan1.contains(i)) {
                        DebugLogger.log(ui.getJobText(i) + "被占卜过了，不是灰");
                    continue;
                }
                DebugLogger.log(ui.getJobText(i) + "是灰");
                cxList.add(i);
            }
        }
        return new Result(cxList, beiZhan1);
    }
}