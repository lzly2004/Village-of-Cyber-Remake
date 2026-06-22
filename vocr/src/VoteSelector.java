public class VoteSelector
{
    private final GameContext ctx;
    private final SuspicionSystem suspicion;

    public VoteSelector(GameContext ctx, SuspicionSystem suspicion)
    {
        this.ctx = ctx;
        this.suspicion = suspicion;
    }

    public int select(int shokeinum, boolean[] votable)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();

        for (int i = 1; i <= n; i++)
        {
            if (ctx.isDead(i)) continue;
            int[] weight = new int[n + 1];
            for (int j = 1; j <= n; j++)
            {
                if (ctx.isDead(j) || i == j
                        || (ctx.getActualRole(i) == 4 && ctx.getActualRole(j) == 4))
                {
                    weight[j] = -GameConstants.INF;
                    continue;
                }
                if (!votable[j])
                    weight[j] = -GameConstants.INFJ;
                else
                    weight[j] = ctx.getSuspicionValue(i, j);
                if (ctx.getActualRole(j) == 5 && weight[j] < -GameConstants.INFJ)
                    weight[j] = 5;
            }

            StringBuilder sb = new StringBuilder("玩家"
                    + CharacterKanjiName.values()[ctx.getCharacterNumber(i)] + "投票权重:");
            for (int j = 1; j <= n; j++)
            {
                if (ctx.isDead(j)) continue;
                sb.append("玩家").append(CharacterKanjiName.values()[ctx.getCharacterNumber(j)])
                        .append(",").append(weight[j]).append(";");
            }
            DebugLogger.log(sb.toString());
            int shokeitaregt = suspicion.getOne(weight);
            ctx.setVoteTarget(i, gd, shokeinum, shokeitaregt);
            suspicion.updateTop3Aux2(i, shokeitaregt, 1, 1);
            if (ctx.getVoteTarget(shokeitaregt, gd, shokeinum) == i)
                suspicion.updateTop3Aux2(i, shokeitaregt, 1, 1);
        }
        for (int i = 1; i <= n; i++)
        {
            if (ctx.isDead(i)) continue;
            for (int j = 1; j <= n; j++)
            {
                if (i == j) continue;
                for (int k = 1; k <= n; k++)
                {
                    if (ctx.isDead(k)) continue;
                    if (i == k || j == k) continue;
                    if (ctx.getVoteTarget(i, gd, shokeinum) == j
                            && ctx.getVoteTarget(j, gd, shokeinum) == k)
                        suspicion.updateTop3Aux2(i, k, -1, 0);
                }
            }
        }
        int maxcnt = -1, shokeicnt[] = new int[n + 1];
        for (int i = 1; i <= n; i++)
        {
            shokeicnt[ctx.getVoteTarget(i, gd, shokeinum)]++;
        }
        for (int i = 1; i <= n; i++)
        {
            maxcnt = Math.max(maxcnt, shokeicnt[i]);
        }
        int topsum = 0;
        int topnum = 0;
        for (int i = 1; i <= n; i++)
        {
            if (shokeicnt[i] == maxcnt)
            {
                topsum++;
                topnum = i;
                votable[i] = true;
            }
            else
                votable[i] = false;
        }
        if (topsum == 1) return topnum;
        else return 0;
    }
}