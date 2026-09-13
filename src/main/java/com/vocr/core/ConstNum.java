package com.vocr.core;

import com.vocr.logging.DebugLogger;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ConstNum
{
    //常量类，定义游戏常量以及静态方法

    // ==================== 可复现随机数系统(第0步重构) ====================
    private static java.util.Random rng = new java.util.Random();

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

    /** 获取随机数序列的强校验和，避免不同序列因长度和总和相同而被误判为一致。 */
    public static String getRandomChecksum()
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            for (int value : recordedSequence)
            {
                buffer.clear();
                buffer.putInt(value);
                digest.update(buffer.array());
            }
            return "size=" + recordedSequence.size() + " sha256=" + toHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("当前Java运行环境不支持SHA-256", e);
        }
    }

    /** 设置固定随机种子,用于回归测试。调用后所有randomInt()结果可复现。 */
    public static void setRandomSeed(long seed)
    {
        rng = new java.util.Random(seed);
        DebugLogger.info("随机种子已固定: " + seed);
    }

    /** 恢复使用系统时间的随机种子(游戏正常运行时使用) */
    public static void resetRandomSeed()
    {
        rng = new java.util.Random();
        DebugLogger.info("随机种子已重置为系统时间");
    }

    public static int randomInt(int min,int max)
    {//生成[min,max]的随机整数
        if (min > max)
            throw new IllegalArgumentException("随机数下界不能大于上界: " + min + " > " + max);
        if(min == max) return min;
        int result = min + rng.nextInt(max - min + 1);
        if (recordRandom) recordedSequence.add(result);
        return result;
    }

    /** 使用项目统一随机源进行原地Fisher-Yates洗牌。 */
    public static <T> void shuffle(List<T> values)
    {
        if (values == null)
            throw new IllegalArgumentException("待洗牌列表不能为空");
        for (int i = values.size() - 1; i > 0; i--)
        {
            int target = randomInt(0, i);
            Collections.swap(values, i, target);
        }
    }

    /** 返回使用项目统一随机源洗牌后的副本，不修改调用方列表。 */
    public static <T> ArrayList<T> shuffledCopy(List<T> values)
    {
        if (values == null || values.isEmpty()) return new ArrayList<>();
        ArrayList<T> copy = new ArrayList<>(values);
        shuffle(copy);
        return copy;
    }

    private static String toHex(byte[] bytes)
    {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes)
            result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

}
