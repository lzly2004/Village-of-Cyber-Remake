import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class GameLogicUtils
{
    public static boolean isDayDie(whyDie why)
    {
        return why != null && why.isDayDeath();
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
        Collections.shuffle(shuffledList);
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
        ArrayList<int[]> pairs = new ArrayList<>();
        for (int i = 1; i <= validLen; i++)
            pairs.add(new int[]{i, array[i]});

        pairs.sort(maxFirst ? Comparator.comparingInt(a -> -a[1]) : Comparator.comparingInt(a -> a[1]));

        for (int i = 0; i < array.length; i++)
            result.add(0);

        for (int i = 0; i < pairs.size(); i++)
        {
            int originalIndex = pairs.get(i)[0];
            int rank = (i > 0 && pairs.get(i)[1] == pairs.get(i-1)[1])
                    ? result.get(pairs.get(i-1)[0]) : (i + 1);
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

    public static String buildSkillScheduleLog(String title, int n, int gd, boolean[][] schedule)
    {
        StringBuilder sb = new StringBuilder(title);
        for (int i = 1; i <= n; i++)
            if (schedule[i][gd])
                sb.append(i).append(" ");
        return sb.toString();
    }
}