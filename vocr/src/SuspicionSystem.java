import java.util.ArrayList;

class SuspicionSystem
{
    private GameStatus gs;
    private int[] lasySuspicionValue;
    private ArrayList<Integer> zhans, lings, lies, maos;
    private boolean[] isDoubleDeathOccurred;
    private int[] claimedRoleaskday;

    public SuspicionSystem(GameStatus gs, ArrayList<Integer> zhans, ArrayList<Integer> lings,
                           ArrayList<Integer> lies, ArrayList<Integer> maos,
                           boolean[] isDoubleDeathOccurred, int[] claimedRoleaskday)
    {
        this.gs = gs;
        this.lasySuspicionValue = new int[gs.getPlayerSum() + 1];
        this.zhans = zhans;
        this.lings = lings;
        this.lies = lies;
        this.maos = maos;
        this.isDoubleDeathOccurred = isDoubleDeathOccurred;
        this.claimedRoleaskday = claimedRoleaskday;
    }

    public int[] getLasySuspicionValue() { return lasySuspicionValue; }

    public void updateTop3Aux2(int num1, int num2, int w1, int w2)
    {
        gs.gc[num1].suspicionValue[num2] += w1;
        gs.gc[num2].suspicionValue[num1] += w2;
    }

    public void printTop3()
    {
        logicTools.log("当前怀疑值情况：");
    }

    public boolean isAckWhite(int num)
    {
        if (num < 1) return false;
        if (gs.gc[num].claimedRole == 4) return true;
        if (gs.gc[num].actualRole != 5 || claimedRoleaskday[5] < 1) return false;

        ArrayList<Integer> mao = new ArrayList<>(maos);
        for (int i = 0; i < mao.size(); i++)
        {
            if (gs.gc[mao.get(i)].nonHumanMarker)
                mao.remove(i);
        }
        if (mao.size() > 1 || (mao.size() == 1 && mao.get(0) != num)) return false;

        int firsttimemoredie = -1;
        for (int i = 1; i <= gs.gameDay; i++)
        {
            if (isDoubleDeathOccurred[i])
            {
                firsttimemoredie = i;
                break;
            }
        }
        if (firsttimemoredie != -1 && firsttimemoredie < claimedRoleaskday[5]) return false;

        lasySuspicionValue[num] -= GameConstants.INF;
        logicTools.log("玩家" + num + " 确定猫");
        return true;
    }

    public int getOne(int val[])
    {
        int playerNum = gs.getPlayerSum();
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
            throw new IllegalStateException("无有效正权重玩家，无法随机选择");

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

    public void updateTop3SuspectedPlayers()
    {
        for (int i = 1; i <= gs.getPlayerSum(); i++)
        {
            if (gs.gc[i].whyDie != whyDie.NONE) continue;
            for (int j = 1; j <= gs.getPlayerSum(); j++)
            {
                if (gs.gc[j].whyDie != whyDie.NONE) continue;
                int p0 = ConstNum.randomInt(1, 100);
                if (i == j)
                    gs.gc[i].suspicionValue[j] = -GameConstants.INF;
                else
                {
                    switch (gs.gc[j].claimedRole)
                    {
                        case 1:
                            gs.gc[i].suspicionValue[j] += 5 + 5 * zhans.size();
                            break;
                        case 2:
                            gs.gc[i].suspicionValue[j] += 10 + 10 * lings.size();
                            break;
                        case 3:
                            gs.gc[i].suspicionValue[j] += 15 + 5 * lies.size();
                            break;
                    }
                    gs.gc[i].suspicionValue[j] += lasySuspicionValue[j];
                    if (GameLogicUtils.zhenying(gs.gc[i]) == 0)
                    {
                        int fr = GameLogicUtils.feiren(gs.gc[j]);
                        if (fr == 0)
                        {
                            if (p0 <= 10) gs.gc[i].suspicionValue[j] -= 15;
                            else if (p0 <= 12) gs.gc[i].suspicionValue[j] += 15;
                            else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-3, 2);
                        }
                        else if (fr == -1)
                        {
                            if (p0 <= 5) gs.gc[i].suspicionValue[j] -= 15;
                            else if (p0 <= 10) gs.gc[i].suspicionValue[j] += 15;
                            else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-2, 2);
                        }
                        else
                        {
                            if (p0 <= 2) gs.gc[i].suspicionValue[j] -= 15;
                            else if (p0 <= 12) gs.gc[i].suspicionValue[j] += 15;
                            else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-2, 3);
                        }
                    }
                    else
                    {
                        if (p0 <= 5) gs.gc[i].suspicionValue[j] -= 15;
                        else if (p0 <= 10) gs.gc[i].suspicionValue[j] += 15;
                        else gs.gc[i].suspicionValue[j] += ConstNum.randomInt(-2, 2);
                    }
                    if (gs.gc[i].suspicionValue[j] > GameConstants.INFJ)
                        gs.gc[i].suspicionValue[j] = GameConstants.INF;
                    else if (gs.gc[i].suspicionValue[j] < -GameConstants.INFJ)
                        gs.gc[i].suspicionValue[j] = -GameConstants.INF;
                    else if (gs.gc[i].suspicionValue[j] > GameConstants.MAXN)
                        gs.gc[i].suspicionValue[j] = GameConstants.MAXN;
                    else if (gs.gc[i].suspicionValue[j] < 0)
                        gs.gc[i].suspicionValue[j] = 0;
                }
            }
        }
        for (int i = 0; i < zhans.size(); i++)
        {
            for (int j = 1; j <= gs.gameDay; j++)
                if (gs.gc[zhans.get(i)].skillTarget[j] <= gs.getPlayerSum()
                        && gs.gc[zhans.get(i)].skillTarget[j] > 0
                        && gs.gc[gs.gc[zhans.get(i)].skillTarget[j]].whyDie == whyDie.NONE)
                    for (int k = 1; k <= gs.getPlayerSum(); k++)
                    {
                        if (k == zhans.get(i)) continue;
                        gs.gc[k].suspicionValue[j]++;
                        if (gs.gc[k].claimedRole == 1)
                            gs.gc[k].suspicionValue[j]++;
                    }
        }

        for (int i = 1; i <= gs.getPlayerSum(); i++)
            lasySuspicionValue[i] = 0;

        for (int i = 1; i <= gs.getPlayerSum(); i++)
        {
            if (gs.gc[i].whyDie != whyDie.NONE) continue;
            for (int j = 1; j <= 3; j++)
            {
                int maxindex = 0;
                for (int k = 1; k <= gs.getPlayerSum(); k++)
                {
                    if (i == k || gs.gc[k].whyDie != whyDie.NONE) continue;
                    if (maxindex == 0 || gs.gc[i].suspicionValue[maxindex] < gs.gc[i].suspicionValue[k])
                    {
                        boolean selected = false;
                        for (int l = 1; l <= j - 1; l++)
                            if (k == gs.gc[i].top3SuspectedPlayers[l][gs.gameDay])
                                selected = true;
                        if (selected) continue;
                        maxindex = k;
                    }
                }
                gs.gc[i].top3SuspectedPlayers[j][gs.gameDay] = maxindex;
            }
        }
    }
}