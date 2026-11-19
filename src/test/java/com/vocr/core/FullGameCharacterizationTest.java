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
            peiyi.jianyi, "2488b2ec83f04d81d24c385ed2b57d2c47058a966b9fcdc1759b7f98b71ce7a7",
            peiyi.tongchang, "1ac309bdbacda4c7a69fa56a186a44adf31e8d68e768f86da833630289040751",
            peiyi.yaoohu, "4c17c0bfc779bba3d85e824de9991110921c4a056502b85944ef144369bfe02c",
            peiyi.kuangxin, "ed478cd65345e7b1ff3b2793036bc5cd83f621cd6173bc32934b1a9e4a97f0c5",
            peiyi.beide, "8cc88486630823aa6186f936c30bf0a50c2c1f2f1bb1007323b655b92c8f8017",
            peiyi.maoyou, "150193e1bef65859b258243be750141adac9efd128a3740fa45a5729dc17dfc6",
            peiyi.daxing, "b05a1621af07f5ca72d84e39f74c073d8ac7935276ae9ceb3638fbe081c85d31"
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
