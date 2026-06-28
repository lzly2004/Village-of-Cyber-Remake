import java.math.BigDecimal;
import java.math.RoundingMode;

public class GameRecord
{
    public static final int CURRENT_VERSION = 2;
    
    public int version = CURRENT_VERSION;
    public int[] playcnt;
    public double[] winrate;
    public int[] villageWincnt;
    public int[] wolfWincnt;
    public int[] foxWincnt;
    public int[] maxStreak;
    public int[] currentStreak;
    public int[] wincnt;
    public int totalPlayCnt;
    public double totalWinRate;
    public int totalVillageWin;
    public int totalWolfWin;
    public int totalFoxWin;
    public int totalMaxStreak;

    GameRecord()
    {
        playcnt = new int[8];
        winrate = new double[8];
        villageWincnt = new int[8];
        wolfWincnt = new int[8];
        foxWincnt = new int[8];
        maxStreak = new int[8];
        currentStreak = new int[8];
        wincnt = new int[8];
        totalPlayCnt = 0;
        totalWinRate = 0.0;
        totalVillageWin = 0;
        totalWolfWin = 0;
        totalFoxWin = 0;
        totalMaxStreak = 0;
    }

    void update(int peiyiIndex, int result)
    {
        DebugLogger.info("[GameRecord.update] peiyiIndex=" + peiyiIndex + ", result=" + result);
        if (peiyiIndex < 1 || peiyiIndex > 7) {
            DebugLogger.warn("[GameRecord.update] peiyiIndex无效，跳过更新");
            return;
        }

        playcnt[peiyiIndex]++;

        boolean isWin = (result == 1);

        if (isWin) {
            currentStreak[peiyiIndex]++;
            if (currentStreak[peiyiIndex] > maxStreak[peiyiIndex]) {
                maxStreak[peiyiIndex] = currentStreak[peiyiIndex];
            }
        } else {
            currentStreak[peiyiIndex] = 0;
        }

        switch (result) {
            case 1: villageWincnt[peiyiIndex]++; break;
            case 2: wolfWincnt[peiyiIndex]++; break;
            case 3: foxWincnt[peiyiIndex]++; break;
        }

        if (playcnt[peiyiIndex] > 0) {
            winrate[peiyiIndex] = calculateWinRate(villageWincnt[peiyiIndex], playcnt[peiyiIndex]);
        }

        aggregateTotal();
    }

    void aggregateTotal()
    {
        totalPlayCnt = 0; totalVillageWin = 0;
        totalWolfWin = 0; totalFoxWin = 0; totalMaxStreak = 0;
        for (int i = 1; i < peiyi.values().length; i++) {
            totalPlayCnt += playcnt[i];
            totalVillageWin += villageWincnt[i];
            totalWolfWin += wolfWincnt[i];
            totalFoxWin += foxWincnt[i];
            if (maxStreak[i] > totalMaxStreak) totalMaxStreak = maxStreak[i];
        }
        if (totalPlayCnt > 0) {
            totalWinRate = calculateWinRate(totalVillageWin, totalPlayCnt);
        }
    }

    private double calculateWinRate(int wins, int total)
    {
        return BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 5, RoundingMode.HALF_UP)
                .doubleValue();
    }
}