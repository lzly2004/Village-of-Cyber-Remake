package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecord;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullGameCharacterizationTest
{
    private static final Map<peiyi, String> EXPECTED_TRACES = Map.of(
            peiyi.jianyi, "da74533762d7e2d87f75c3649702edb1a3e2658cc97a338171294dcc833851d2",
            peiyi.tongchang, "79ec602c12639f73a2da20dcfbbc714b0e2f476555a49a75ff14d842244a9fa5",
            peiyi.yaoohu, "e64d25c688f8babe83a8369bb37657e8d4bb02c4b8c6428deb2a92a7d12efc48",
            peiyi.kuangxin, "6aa7a3aa43b784c1236b8f884cc88b6aebed6a2ca871a6038e4a1954b5dbbce9",
            peiyi.beide, "30cf1da8f30262f22ab50598ede34f81d6005d04e4b652ba4098505a764bfb49",
            peiyi.maoyou, "cc37978ce4533d33d066cccacc087f991db02f3de1a05d23b0eca831f12c0c41",
            peiyi.daxing, "c1a8b66851da1962c647f2c2d7fba01e536cf39c3256b6342b3491e5429d42bd"
    );

    @TempDir
    Path tempDir;

    @AfterEach
    void restoreRandomSource()
    {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

    Stream<Arguments> configurations()
    {
        return Stream.of(
                Arguments.of(peiyi.jianyi, 4201L),
                Arguments.of(peiyi.tongchang, 4202L),
                Arguments.of(peiyi.yaoohu, 4203L),
                Arguments.of(peiyi.kuangxin, 4204L),
                Arguments.of(peiyi.beide, 4205L),
                Arguments.of(peiyi.maoyou, 4206L),
                Arguments.of(peiyi.daxing, 4207L)
        );
    }

    @ParameterizedTest(name = "完整对局 {0} / seed={1}")
    @MethodSource("configurations")
    void fixedPolicy_shouldReachStableCompleteGame(peiyi configuration, long seed)
    {
        List<Event> events = new ArrayList<>();
        GameRecordManager records = new GameRecordManager(
                tempDir.resolve(configuration + "-record.json").toString());
        MainLogic logic = new MainLogic(events::add, records);

        ConstNum.setRandomSeed(seed);
        ConstNum.startRandomRecording();
        GameStatus status = logic.start(configuration, records);
        GameContextView context = logic.getGameContext();
        StringBuilder decisions = new StringBuilder();

        int dayCount = 0;
        while (context.getEndResult() == GameResult.NONE)
        {
            if (dayCount >= GameConstants.MAX_GAME_DAYS - 2)
                fail("固定策略未能在最大日期前结束对局: " + configuration);
            int target = selectExecutionTarget(context);
            decisions.append(context.getGameDay()).append("->").append(target).append(',');
            assertTrue(logic.shokei(2, List.of(target), false), "指定投票应完成当日流程");
            dayCount++;
        }
        ConstNum.stopRandomRecording();

        assertNotEquals(GameResult.NONE, context.getEndResult());
        assertTrue(dayCount > 0);
        assertEquals(status.getPlayerSum(), context.getAliveCounter() + context.getDeathCounter());
        assertRecordedResult(records.getRecord(), configuration, context.getEndResult());

        String digest = sha256(buildTrace(configuration, seed, dayCount, decisions, events, logic, context));
        System.out.printf("FULL_GAME %s seed=%d days=%d result=%s digest=%s random=%s%n",
                configuration, seed, dayCount, context.getEndResult(), digest, ConstNum.getRandomChecksum());

        assertEquals(EXPECTED_TRACES.get(configuration), digest, "完整对局行为轨迹发生变化");
    }

    private static int selectExecutionTarget(GameContextView context)
    {
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            if (context.isAlive(player) && context.getActualRole(player) != Role.gong.ordinal())
                return player;
        }
        throw new IllegalStateException("没有可执行的非共有者玩家");
    }

    private static void assertRecordedResult(GameRecord record, peiyi configuration, GameResult result)
    {
        int index = configuration.ordinal();
        assertEquals(1, record.playcnt[index]);
        assertEquals(1, record.totalPlayCnt);
        assertEquals(result == GameResult.VILLAGE_WIN ? 1 : 0, record.villageWincnt[index]);
        assertEquals(result == GameResult.WOLF_WIN ? 1 : 0, record.wolfWincnt[index]);
        assertEquals(result == GameResult.FOX_WIN ? 1 : 0, record.foxWincnt[index]);
    }

    private static String buildTrace(peiyi configuration, long seed, int dayCount,
                                     StringBuilder decisions, List<Event> events,
                                     MainLogic logic, GameContextView context)
    {
        StringBuilder trace = new StringBuilder(32_768);
        trace.append("game=").append(configuration).append('|').append(seed).append('|')
                .append(dayCount).append('|').append(context.getGameDay()).append('|')
                .append(context.getEndResult()).append('|').append(context.getAliveCounter()).append('|')
                .append(context.getDeathCounter()).append('\n');
        trace.append("decisions=").append(decisions).append('\n');

        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            trace.append("player=").append(player).append('|').append(context.getCharacterNumber(player)).append('|')
                    .append(context.getActualRole(player)).append('|').append(context.getClaimedRole(player)).append('|')
                    .append(context.getClaimedRoleOrder(player)).append('|').append(context.getComingOutDay(player)).append('|')
                    .append(context.getDeathReason(player)).append('|').append(context.getDeathDay(player)).append('|')
                    .append(context.isNonHumanMarked(player)).append('\n');
        }

        int lastDay = Math.min(GameConstants.MAX_GAME_DAYS, context.getGameDay() + 1);
        for (int day = 1; day <= lastDay; day++)
        {
            trace.append("day=").append(day).append("|rule=").append(context.getVotingRule(day))
                    .append("|rounds=").append(context.getVoteRoundCount(day))
                    .append("|candidates=").append(context.getVotingCandidates(day)).append('\n');
            for (int round = 1; round <= context.getVoteRoundCount(day); round++)
            {
                trace.append("votes=").append(day).append('|').append(round).append('|');
                for (int player = 1; player <= context.getPlayerSum(); player++)
                    trace.append(player).append("->").append(context.getVoteTarget(player, day, round)).append(',');
                trace.append('\n');
            }
        }

        for (Event event : events)
        {
            trace.append("event=").append(event.eventname).append('|')
                    .append(event.ch1).append('|').append(event.ch2).append('|').append(event.ch3).append('\n');
        }
        for (String record : logic.getRecorder().getRecords())
        {
            if (record.startsWith("# Time:")) continue;
            trace.append("record=").append(normalizeNewlines(record)).append('\n');
        }
        trace.append("random=").append(ConstNum.getRandomChecksum()).append('\n');
        return trace.toString();
    }

    private static String normalizeNewlines(String value)
    {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String sha256(String value)
    {
        try
        {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte item : hash) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("无法计算完整对局摘要", e);
        }
    }
}
