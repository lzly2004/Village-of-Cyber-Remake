package com.vocr.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.IntPredicate;

class GameLogicUtils
{
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

    public static String buildSkillScheduleLog(String title, int n, IntPredicate isScheduled)
    {
        StringBuilder sb = new StringBuilder(title);
        for (int i = 1; i <= n; i++)
            if (isScheduled.test(i))
                sb.append(i).append(" ");
        return sb.toString();
    }

}
