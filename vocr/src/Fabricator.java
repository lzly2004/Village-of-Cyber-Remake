import java.util.ArrayList;
import java.util.List;

public class Fabricator
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final ZhanFabricator zhanFabricator;
    private final LingFabricator lingFabricator;
    private final HunterGuarder hunterGuarder;

    public Fabricator(GameContext ctx, SuspicionSystem suspicion,
                      SeerDiviner seerDiviner, HunterGuarder hunterGuarder)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
        this.zhanFabricator = new ZhanFabricator(ctx, suspicion, seerDiviner);
        this.lingFabricator = new LingFabricator(ctx, suspicion);
        this.hunterGuarder = hunterGuarder;
    }

    public void fabricate(ArrayList<Integer> diebody)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        for (int i = 1; i <= n; i++)
        {
            if (ctx.getActualRole(i) < 7 || !ctx.isAlive(i)) continue;
            if (ctx.getClaimedRole(i) == 6 || (ctx.getClaimedRole(i) == 0 && ctx.nonHumanPlan[i] == 0)) continue;
            if (ctx.getClaimedRole(i) == 1 || (ctx.getClaimedRole(i) == 0 && ctx.nonHumanPlan[i] == 1))
            {
                zhanFabricator.fabricate(diebody, i);
            }
            else if (ctx.getClaimedRole(i) == 2 || (ctx.getClaimedRole(i) == 0 && ctx.nonHumanPlan[i] == 2))
            {
                lingFabricator.fabricate(i);
                DebugLogger.log("假灵能结果：" + ctx.getSkillTarget(i, gd));
            }
            else
                frlyingLie(diebody, i);
        }
    }

    private void frlyingLie(ArrayList<Integer> diebody, int num)
    {
        if (ctx.isNonHumanMarked(num)) return;
        int gd = ctx.getGameDay();
        DebugLogger.log("进入假猎人撒谎环节，假猎人下标：" + num);
        int[] target = new int[2];
        ArrayList<Integer> weight = new ArrayList<>(List.of(50, 50));
        for (int i = 0; i < 2; i++)
        {
            target[i] = hunterGuarder.guard(num);
            if (ctx.getActualRole(num) == 7 || ctx.getActualRole(num) == 9)
            {
                if (ctx.getActualRole(target[i]) == 7) break;
                if (GameLogicUtils.probabilityJudge(10))
                {
                    target[i] = hunterGuarder.guard(num);
                    continue;
                }
                break;
            }
            if (diebody.size() == 1 && target[i] == diebody.get(0))
                weight.set(i, 1);
        }
        if (target[1] == 0) target[1] = target[0];
        int op = GameLogicUtils.getEventIndexByProbability(weight);
        ctx.setSkillTarget(num, gd, target[op]);
        DebugLogger.log("假猎" + CharacterKanjiName.values()[ctx.getCharacterNumber(num)]
                + ",假猎目标" + CharacterKanjiName.values()[ctx.getCharacterNumber(target[op])]);
    }
}