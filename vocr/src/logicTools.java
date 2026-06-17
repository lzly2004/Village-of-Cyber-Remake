class logicTools
{
    /** @deprecated 使用 DebugLogger.log() 替代 */
    public static void log(String message)
    {
        DebugLogger.log(message);
    }

    public static int min(int x, int y)
    {
        if (x < y) return x;
        return y;
    }

    public static boolean probabilityJudge(int p)
    {
        if (p >= 100) return true;
        if (p <= 0) return false;
        int p0 = ConstNum.randomInt(1, 100);
        return p0 <= p;
    }
}