import java.util.ArrayList;

class GameLogicUtils
{
    public static boolean isDayDie(whyDie why)
    {
        return why == whyDie.chuxing || why == whyDie.dayhouzhui || why == whyDie.daymaozhou;
    }

    public static int getEventIndexByProbability(ArrayList<Integer> probabilities)
    {
        if (probabilities == null || probabilities.isEmpty())
            throw new IllegalArgumentException("概率数组不能为空！");

        int total = 0;
        for (Integer prob : probabilities)
        {
            if (prob == null || prob < 0)
                throw new IllegalArgumentException("概率数组元素必须为非负整数！");
            total += prob;
        }
        if (total == 0)
            throw new IllegalArgumentException("概率数组元素总和必须为正整数！");

        int randomValue = ConstNum.randomInt(0, total - 1);
        int cumulativeProb = 0;
        for (int i = 0; i < probabilities.size(); i++)
        {
            cumulativeProb += probabilities.get(i);
            if (randomValue < cumulativeProb)
                return i;
        }
        throw new IllegalStateException("概率计算异常，未匹配到有效索引！");
    }

    public static int zhenying(int actualRole)
    {
        switch (actualRole)
        {
            case 7: case 8: case 9: return 1;
            case 10: case 11: return -1;
            default: return 0;
        }
    }

    public static int feiren(int actualRole)
    {
        switch (actualRole)
        {
            case 7: return 1;
            case 10: case 11: case 8: case 9: return -1;
            default: return 0;
        }
    }

    public static <T> ArrayList<T> shuffleList(ArrayList<T> originalList)
    {
        if (originalList == null || originalList.isEmpty())
            return new ArrayList<>();

        ArrayList<T> shuffledList = new ArrayList<>(originalList);
        int listSize = shuffledList.size();

        for (int i = listSize - 1; i > 0; i--)
        {
            int randomIndex = ConstNum.randomInt(0, i);
            T temp = shuffledList.get(i);
            shuffledList.set(i, shuffledList.get(randomIndex));
            shuffledList.set(randomIndex, temp);
        }

        return shuffledList;
    }

    public static ArrayList<Integer> getPriority(int[] array, boolean maxFirst)
    {
        ArrayList<Integer> result = new ArrayList<>();

        if (array == null || array.length < 2)
        {
            result.add(0);
            return result;
        }

        int validLen = array.length - 1;
        int[] valArr = new int[validLen];
        int[] idxArr = new int[validLen];
        for (int i = 0; i < validLen; i++)
        {
            valArr[i] = array[i + 1];
            idxArr[i] = i + 1;
        }

        for (int i = 0; i < validLen - 1; i++)
        {
            for (int j = 0; j < validLen - 1 - i; j++)
            {
                boolean needSwap = maxFirst ? valArr[j] < valArr[j + 1] : valArr[j] > valArr[j + 1];
                if (needSwap)
                {
                    int tempVal = valArr[j];
                    valArr[j] = valArr[j + 1];
                    valArr[j + 1] = tempVal;
                    int tempIdx = idxArr[j];
                    idxArr[j] = idxArr[j + 1];
                    idxArr[j + 1] = tempIdx;
                }
            }
        }

        int[] rankArr = new int[validLen];
        int currentRank = 1;
        int currentVal = valArr[0];
        rankArr[0] = currentRank;
        for (int i = 1; i < validLen; i++)
        {
            if (valArr[i] != currentVal)
            {
                currentRank = i + 1;
                currentVal = valArr[i];
            }
            rankArr[i] = currentRank;
        }

        for (int i = 0; i < array.length; i++)
            result.add(0);
        for (int i = 0; i < validLen; i++)
        {
            int originalIndex = idxArr[i];
            int rank = rankArr[i];
            result.set(originalIndex, rank);
        }

        return result;
    }

    public static boolean probabilityJudge(int p)
    {
        if (p >= 100) return true;
        if (p <= 0) return false;
        int p0 = ConstNum.randomInt(1, 100);
        return p0 <= p;
    }
}