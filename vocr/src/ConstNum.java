import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.ArrayList;

class ConstNum
{
    //常量类，定义游戏常量以及静态方法
    public static int CharacterSum = 44;// 游戏角色数量
    public static int N = 50;//最大游戏天数
    public static int M = 20;//职业状态数量

    // ==================== 可复现随机数系统(第0步重构) ====================
    private static java.util.Random rng = new java.util.Random();
    private static boolean useFixedSeed = false;
    private static long fixedSeed = 0;

    // ==================== 随机数序列录制(回归测试) ====================
    private static boolean recordRandom = false;
    private static final ArrayList<Integer> recordedSequence = new ArrayList<>();

    /** 开始录制随机数序列 */
    public static void startRandomRecording()
    {
        recordedSequence.clear();
        recordRandom = true;
    }

    /** 停止录制并返回序列副本 */
    public static ArrayList<Integer> stopRandomRecording()
    {
        recordRandom = false;
        return new ArrayList<>(recordedSequence);
    }

    /** 获取当前已录制的随机数序列 */
    public static ArrayList<Integer> getRecordedSequence()
    {
        return new ArrayList<>(recordedSequence);
    }

    /** 获取随机数序列的校验和(大小+累加和) */
    public static String getRandomChecksum()
    {
        long sum = 0;
        for (int val : recordedSequence) sum += val;
        return "size=" + recordedSequence.size() + " sum=" + sum;
    }

    /** 设置固定随机种子,用于回归测试。调用后所有randomInt()结果可复现。 */
    public static void setRandomSeed(long seed)
    {
        rng = new java.util.Random(seed);
        useFixedSeed = true;
        fixedSeed = seed;
        DebugLogger.info("随机种子已固定: " + seed);
    }

    /** 恢复使用系统时间的随机种子(游戏正常运行时使用) */
    public static void resetRandomSeed()
    {
        rng = new java.util.Random();
        useFixedSeed = false;
        DebugLogger.info("随机种子已重置为系统时间");
    }

    /** 获取当前使用的随机种子(仅当种子被固定时有效) */
    public static long getCurrentSeed() { return useFixedSeed ? fixedSeed : -1; }

    /** 是否正在使用固定种子 */
    public static boolean isUsingFixedSeed() { return useFixedSeed; }

    public static int randomInt(int min,int max)
    {//生成[min,max]的随机整数
        if(min == max) return min;
        int result = min + rng.nextInt(max - min + 1);
        if (recordRandom) recordedSequence.add(result);
        return result;
    }

    public static ImageIcon scaleIcon(ImageIcon originalIcon, int width, int height)
    {
        if (originalIcon == null) return null;
        Image scaledImage = originalIcon.getImage().getScaledInstance(
                width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
}