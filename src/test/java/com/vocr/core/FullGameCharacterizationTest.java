package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.Test;
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
            peiyi.jianyi, "04c36c8ec0e35dae35e729588349f8fdce84042ea75a50876cc7837b8586e3ab",
            peiyi.tongchang, "406d56900acecd2b5c0de374118b0d6d48c47290a958455145ee5dadc2ef163b",
            peiyi.yaoohu, "24bf491cd2036520178d4cb05cad0639811de55f4ed4c0eab521a4bf8d1bd3eb",
            peiyi.kuangxin, "ac0486689e71c493c19444e34473c47fb8183e6d3d73c7aa6df38c930394dfe5",
            peiyi.beide, "0484354a0f4d6ab12c5f4e9e1b000a0b0d4e1e617ea5c63496e00aedb03ff62c",
            peiyi.maoyou, "13ce25f949b4dc92ad7969967c2b342317ac36f55214e25997d274c197a44de2",
            peiyi.daxing, "29e7a2a524961b2f558c9c7b0ed60c42c17ff2b4c01044e53557ba15f189dd1e"
    );

    @TempDir
    Path tempDir;

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
        SessionRun game = new SessionRun(configuration, seed, "single");
        while (!game.isComplete()) game.advanceOneDay();
        String digest = game.finishAndDigest();
        System.out.printf("FULL_GAME %s seed=%d days=%d result=%s digest=%s random=%s%n",
                configuration, seed, game.dayCount, game.context.getEndResult(), digest,
                game.random.getRecordedChecksum());

        assertEquals(EXPECTED_TRACES.get(configuration), digest, "完整对局行为轨迹发生变化");
    }

    @Test
    void interleavedGames_shouldKeepTheirCompleteRandomTrajectoriesIndependent()
    {
        SessionRun first = new SessionRun(peiyi.jianyi, 4201L, "interleaved-a");
        SessionRun second = new SessionRun(peiyi.tongchang, 4202L, "interleaved-b");

        while (!first.isComplete() || !second.isComplete())
        {
            if (!first.isComplete()) first.advanceOneDay();
            if (!second.isComplete()) second.advanceOneDay();
        }

        assertEquals(EXPECTED_TRACES.get(first.configuration), first.finishAndDigest(),
                "另一局的推进不得污染第一局随机轨迹");
        assertEquals(EXPECTED_TRACES.get(second.configuration), second.finishAndDigest(),
                "另一局的推进不得污染第二局随机轨迹");
    }

    private final class SessionRun
    {
        private final peiyi configuration;
        private final long seed;
        private final List<Event> events = new ArrayList<>();
        private final GameRecordManager records;
        private final GameRandom random;
        private final MainLogic logic;
        private final GameStatus status;
        private final GameContextView context;
        private final StringBuilder decisions = new StringBuilder();
        private int dayCount;

        private SessionRun(peiyi configuration, long seed, String label)
        {
            this.configuration = configuration;
            this.seed = seed;
            this.records = new GameRecordManager(
                    tempDir.resolve(configuration + "-" + label + "-record.json").toString());
            this.random = new GameRandom(seed);
            this.logic = new MainLogic(events::add, records, random);
            random.startRecording();
            this.status = logic.start(configuration, records);
            this.context = logic.getGameContext();
        }

        private boolean isComplete()
        {
            return context.getEndResult() != GameResult.NONE;
        }

        private void advanceOneDay()
        {
            if (dayCount >= GameConstants.MAX_GAME_DAYS - 2)
                fail("固定策略未能在最大日期前结束对局: " + configuration);
            int target = selectExecutionTarget(context);
            decisions.append(context.getGameDay()).append("->").append(target).append(',');
            assertTrue(logic.shokei(2, List.of(target), false), "指定投票应完成当日流程");
            dayCount++;
        }

        private String finishAndDigest()
        {
            random.stopRecording();
            assertNotEquals(GameResult.NONE, context.getEndResult());
            assertTrue(dayCount > 0);
            assertEquals(status.getPlayerSum(), context.getAliveCounter() + context.getDeathCounter());
            return sha256(buildTrace(
                    configuration, seed, dayCount, decisions, events, logic, context, random));
        }
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

    private static String buildTrace(peiyi configuration, long seed, int dayCount,
                                     StringBuilder decisions, List<Event> events,
                                     MainLogic logic, GameContextView context, GameRandom random)
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
            trace.append("event=").append(event.eventname()).append('|')
                    .append(event.ch1()).append('|').append(event.ch2()).append('|').append(event.ch3()).append('\n');
        }
        for (String record : logic.getRecorder().getRecords())
        {
            if (record.startsWith("# Time:")) continue;
            trace.append("record=").append(normalizeNewlines(record)).append('\n');
        }
        trace.append("random=").append(random.getRecordedChecksum()).append('\n');
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
