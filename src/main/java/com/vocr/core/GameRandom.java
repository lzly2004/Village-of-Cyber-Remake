package com.vocr.core;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 单局游戏拥有的随机源。
 *
 * <p>所有消耗主游戏随机序列的选择都必须经过此对象，避免并行或交错运行的对局
 * 共享全局状态。少数明确不应推进主序列的派生日记可使用自己的确定性子随机源。
 * 录制能力只用于回归测试，不改变随机数的生成算法。</p>
 */
final class GameRandom
{
    private final Random random;
    private boolean recording;
    private final ArrayList<Integer> recordedSequence = new ArrayList<>();

    GameRandom()
    {
        this(new Random());
    }

    GameRandom(long seed)
    {
        this(new Random(seed));
    }

    private GameRandom(Random random)
    {
        this.random = Objects.requireNonNull(random, "随机数生成器不能为空");
    }

    void reseedForTest(long seed)
    {
        random.setSeed(seed);
    }

    /** 生成闭区间 [min, max] 内的随机整数。 */
    int randomInt(int min, int max)
    {
        if (min > max)
            throw new IllegalArgumentException("随机数下界不能大于上界: " + min + " > " + max);
        if (min == max) return min;
        int result = min + random.nextInt(max - min + 1);
        if (recording) recordedSequence.add(result);
        return result;
    }

    boolean probabilityJudge(int percentage)
    {
        if (percentage >= 100) return true;
        if (percentage <= 0) return false;
        return randomInt(1, 100) <= percentage;
    }

    int weightedIndex(List<Integer> weights)
    {
        if (weights == null || weights.isEmpty())
            throw new IllegalArgumentException("概率数组不能为空！");

        int total = 0;
        for (Integer weight : weights)
        {
            if (weight == null || weight < 0)
                throw new IllegalArgumentException("概率数组元素必须为非负整数！");
            total += weight;
        }
        if (total == 0)
            throw new IllegalArgumentException("概率数组元素总和必须为正整数！");

        int randomValue = randomInt(0, total - 1);
        int cumulativeWeight = 0;
        for (int i = 0; i < weights.size(); i++)
        {
            cumulativeWeight += weights.get(i);
            if (randomValue < cumulativeWeight)
                return i;
        }
        throw new IllegalStateException("概率计算异常，未匹配到有效索引！");
    }

    <T> void shuffle(List<T> values)
    {
        if (values == null)
            throw new IllegalArgumentException("待洗牌列表不能为空");
        for (int i = values.size() - 1; i > 0; i--)
        {
            int target = randomInt(0, i);
            Collections.swap(values, i, target);
        }
    }

    <T> ArrayList<T> shuffledCopy(List<T> values)
    {
        if (values == null || values.isEmpty()) return new ArrayList<>();
        ArrayList<T> copy = new ArrayList<>(values);
        shuffle(copy);
        return copy;
    }

    void startRecording()
    {
        recordedSequence.clear();
        recording = true;
    }

    ArrayList<Integer> stopRecording()
    {
        recording = false;
        return new ArrayList<>(recordedSequence);
    }

    String getRecordedChecksum()
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

    private static String toHex(byte[] bytes)
    {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes)
            result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
