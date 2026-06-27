import java.util.ArrayList;

class SuspicionSystem
{
    private GameContext ctx;
    private int[] lazySuspicionValue;

    public SuspicionSystem(GameContext ctx)
    {
        this.ctx = ctx;
        int n = ctx.getPlayerSum();
        this.lazySuspicionValue = new int[n + 1];
        
        for (int i = 1; i <= n; i++)
        {
            for (int j = 1; j <= n; j++)
            {
                if (i == j)
                    ctx.setSuspicionValue(i, j, -GameConstants.INF);
                else
                    ctx.setSuspicionValue(i, j, 50);
            }
        }
    }

    public int[] getLazySuspicionValue() { return lazySuspicionValue; }
    public void addLazySuspicionValue(int player, int delta) { lazySuspicionValue[player] += delta; }
    public void setLazySuspicionValue(int player, int value) { lazySuspicionValue[player] = value; }

    public void updateTop3Aux2(int num1, int num2, int w1, int w2)
    {
        ctx.addSuspicionValue(num1, num2, w1);
        ctx.addSuspicionValue(num2, num1, w2);
    }

    public boolean isAckWhite(int num, ArrayList<Integer> maos,
                              boolean[] isDoubleDeathOccurred, int[] claimedRoleaskday)
    {
        if (num < 1) return false;
        if (ctx.getClaimedRole(num) == 4) return true;
        if (ctx.getActualRole(num) != 5 || claimedRoleaskday[5] < 1) return false;

        ArrayList<Integer> mao = new ArrayList<>(maos);
        for (int i = mao.size() - 1; i >= 0; i--)
        {
            if (ctx.isNonHumanMarked(mao.get(i)))
                mao.remove(i);
        }
        if (mao.size() > 1 || (mao.size() == 1 && mao.get(0) != num)) return false;

        int firsttimemoredie = -1;
        for (int i = 1; i <= ctx.getGameDay(); i++)
        {
            if (isDoubleDeathOccurred[i])
            {
                firsttimemoredie = i;
                break;
            }
        }
        if (firsttimemoredie != -1 && firsttimemoredie < claimedRoleaskday[5]) return false;

        return true;
    }

    public void markAckWhite(int num)
    {
        if (num < 1) return;
        if (ctx.getClaimedRole(num) == 4) return;
        lazySuspicionValue[num] -= GameConstants.INF;
        DebugLogger.log("玩家" + num + " 确定猫");
    }

    public int getOne(int[] val)
    {
        int playerNum = ctx.getPlayerSum();
        if (playerNum < 1)
            throw new IllegalArgumentException("玩家数量不能小于1，当前数量：" + playerNum);
        if (val == null || val.length < playerNum + 1)
            throw new IllegalArgumentException("权重数组为空或长度不足，需至少包含" + (playerNum + 1) + "个元素");

        int maxVal = Integer.MIN_VALUE;
        int maxIdx = -1;
        int secondVal = Integer.MIN_VALUE;
        int secondIdx = -1;

        for (int i = 1; i <= playerNum; i++)
        {
            int currWeight = val[i];
            if (currWeight > maxVal)
            {
                secondVal = maxVal;
                secondIdx = maxIdx;
                maxVal = currWeight;
                maxIdx = i;
            }
            else if (currWeight > secondVal)
            {
                secondVal = currWeight;
                secondIdx = i;
            }
        }

        int[] tempWeights = new int[playerNum + 1];
        System.arraycopy(val, 1, tempWeights, 1, playerNum);

        if (maxVal <= 0)
        {
            if (secondVal < -GameConstants.INFJ)
                return maxIdx;
            int delta = 5 - secondVal;
            tempWeights[maxIdx] = maxVal + delta;
            tempWeights[secondIdx] = 5;
        }

        int totalWeight = 0;
        for (int i = 1; i <= playerNum; i++)
        {
            if (tempWeights[i] > 0)
                totalWeight += tempWeights[i];
        }
        if (totalWeight <= 0)
        {
            DebugLogger.error("getOne: 无有效正权重玩家，降级返回maxIdx=" + maxIdx);
            return maxIdx >= 1 ? maxIdx : 1;
        }

        int randomNum = ConstNum.randomInt(0, totalWeight - 1);
        int cumulativeWeight = 0;
        for (int i = 1; i <= playerNum; i++)
        {
            int weight = tempWeights[i];
            if (weight > 0)
            {
                cumulativeWeight += weight;
                if (cumulativeWeight > randomNum)
                    return i;
            }
        }

        return maxIdx;
    }

    public void updateTop3SuspectedPlayers(ArrayList<Integer> zhans,
                                            ArrayList<Integer> lings,
                                            ArrayList<Integer> lies)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();
        for (int i = 1; i <= n; i++)
        {
            if (ctx.isDead(i)) continue;
            for (int j = 1; j <= n; j++)
            {
                if (ctx.isDead(j)) continue;
                int p0 = ConstNum.randomInt(1, 100);
                if (i == j)
                    ctx.setSuspicionValue(i, j, -GameConstants.INF);
                else
                {
                    switch (ctx.getClaimedRole(j))
                    {
                        case 1:
                            ctx.addSuspicionValue(i, j, 5 + 5 * zhans.size());
                            break;
                        case 2:
                            ctx.addSuspicionValue(i, j, 10 + 10 * lings.size());
                            break;
                        case 3:
                            ctx.addSuspicionValue(i, j, 15 + 5 * lies.size());
                            break;
                    }
                    ctx.addSuspicionValue(i, j, lazySuspicionValue[j]);
                    if (GameLogicUtils.zhenying(ctx.getActualRole(i)) == 0)
                    {
                        int fr = GameLogicUtils.feiren(ctx.getActualRole(j));
                        if (fr == 0)
                        {
                            if (p0 <= 10) ctx.addSuspicionValue(i, j, -15);
                            else if (p0 <= 12) ctx.addSuspicionValue(i, j, 15);
                            else ctx.addSuspicionValue(i, j, ConstNum.randomInt(-3, 2));
                        }
                        else if (fr == -1)
                        {
                            if (p0 <= 5) ctx.addSuspicionValue(i, j, -15);
                            else if (p0 <= 10) ctx.addSuspicionValue(i, j, 15);
                            else ctx.addSuspicionValue(i, j, ConstNum.randomInt(-2, 2));
                        }
                        else
                        {
                            if (p0 <= 2) ctx.addSuspicionValue(i, j, -15);
                            else if (p0 <= 12) ctx.addSuspicionValue(i, j, 15);
                            else ctx.addSuspicionValue(i, j, ConstNum.randomInt(-2, 3));
                        }
                    }
                    else
                    {
                        if (p0 <= 5) ctx.addSuspicionValue(i, j, -15);
                        else if (p0 <= 10) ctx.addSuspicionValue(i, j, 15);
                        else ctx.addSuspicionValue(i, j, ConstNum.randomInt(-2, 2));
                    }
                    int sv = ctx.getSuspicionValue(i, j);
                    if (sv > GameConstants.INFJ)
                        ctx.setSuspicionValue(i, j, GameConstants.INF);
                    else if (sv < -GameConstants.INFJ)
                        ctx.setSuspicionValue(i, j, -GameConstants.INF);
                    else if (sv > GameConstants.MAXN)
                        ctx.setSuspicionValue(i, j, GameConstants.MAXN);
                    else if (sv < 0)
                        ctx.setSuspicionValue(i, j, 0);
                }
            }
        }
        for (int i = 0; i < zhans.size(); i++)
        {
            int zhan = zhans.get(i);
            for (int j = 1; j <= gd; j++)
            {
                int target = ctx.getSkillTarget(zhan, j);
                if (target <= n && target > 0 && ctx.isAlive(target))
                    for (int k = 1; k <= n; k++)
                    {
                        if (k == zhan) continue;
                        ctx.addSuspicionValue(k, target, 1);
                        if (ctx.getClaimedRole(k) == 1)
                            ctx.addSuspicionValue(k, target, 1);
                    }
            }
        }

        for (int i = 1; i <= n; i++)
            lazySuspicionValue[i] = 0;

        for (int i = 1; i <= n; i++)
        {
            if (ctx.isDead(i)) continue;
            for (int j = 1; j <= 3; j++)
            {
                int maxindex = 0;
                for (int k = 1; k <= n; k++)
                {
                    if (i == k || ctx.isDead(k)) continue;
                    if (maxindex == 0 || ctx.getSuspicionValue(i, maxindex) < ctx.getSuspicionValue(i, k))
                    {
                        boolean selected = false;
                        for (int l = 1; l <= j - 1; l++)
                            if (k == ctx.getTop3SuspectedPlayer(i, l, gd))
                                selected = true;
                        if (selected) continue;
                        maxindex = k;
                    }
                }
                ctx.setTop3SuspectedPlayer(i, j, gd, maxindex);
            }
        }
    }
}