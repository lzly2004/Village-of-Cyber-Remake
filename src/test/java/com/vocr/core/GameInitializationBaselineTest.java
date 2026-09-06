package com.vocr.core;

import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;
import com.vocr.history.GameRecordManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 七种配役的初始化特征测试。
 *
 * 它同时验证业务不变量和固定种子下的完整状态摘要。后续重构若改变行为，必须明确更新黄金摘要，
 * 不能在没有理解差异的情况下简单改期望值。
 */
class GameInitializationBaselineTest {
    private static final Map<peiyi, int[]> EXPECTED_ROLES = Map.of(
            peiyi.jianyi,   roles(1, 1, 1, 0, 0, 6, 2, 1, 0, 0, 0),
            peiyi.tongchang,roles(1, 1, 1, 2, 0, 7, 3, 1, 0, 0, 0),
            peiyi.yaoohu,   roles(1, 1, 1, 2, 0, 6, 3, 1, 0, 1, 0),
            peiyi.kuangxin, roles(1, 1, 1, 2, 0, 7, 3, 0, 1, 1, 0),
            peiyi.beide,    roles(1, 1, 1, 2, 0, 7, 3, 1, 0, 1, 1),
            peiyi.maoyou,   roles(1, 1, 1, 2, 1, 6, 4, 1, 0, 1, 0),
            peiyi.daxing,   roles(1, 1, 1, 2, 1, 7, 4, 0, 1, 1, 1)
    );

    private static final Map<peiyi, String> EXPECTED_SNAPSHOTS = new LinkedHashMap<>();

    static {
        EXPECTED_SNAPSHOTS.put(peiyi.jianyi, "cfc84394371f9d56631388437e811b4f01ddcb870891122964b464a0fec8b6ee");
        EXPECTED_SNAPSHOTS.put(peiyi.tongchang, "f823622a4a31d7ae5f6615f7ea2b9d57d2f94704f35683b05bd30df610567c24");
        EXPECTED_SNAPSHOTS.put(peiyi.yaoohu, "292e81989ee5a6415d623bc5d598706ae00c4454014b43ee1f596ee4d4097c77");
        EXPECTED_SNAPSHOTS.put(peiyi.kuangxin, "489ca7b2ccc31ee18940a762a6870ad6116b4e1e32eaebde5b4f1b420311d43e");
        EXPECTED_SNAPSHOTS.put(peiyi.beide, "bf6472200883d6426a0f109e6026b13af70fd3a6473db70f29b816fe0c768ec7");
        EXPECTED_SNAPSHOTS.put(peiyi.maoyou, "edd845cb3c0fd94effdda347be6a0f17971b4f43a9104add68603226604d99fb");
        EXPECTED_SNAPSHOTS.put(peiyi.daxing, "14edf2c451428305d314f4bddcf4afd63ddb29d787a3714fe4f3eea150a702d4");
    }

    @TempDir
    Path tempDir;

    static Stream<Arguments> configurations() {
        return Stream.of(
                Arguments.of(peiyi.jianyi, 42L),
                Arguments.of(peiyi.tongchang, 123L),
                Arguments.of(peiyi.yaoohu, 456L),
                Arguments.of(peiyi.kuangxin, 789L),
                Arguments.of(peiyi.beide, 101L),
                Arguments.of(peiyi.maoyou, 202L),
                Arguments.of(peiyi.daxing, 303L)
        );
    }

    @AfterEach
    void restoreRandomSource() {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

    @ParameterizedTest(name = "{0} / seed={1}")
    @MethodSource("configurations")
    void initialization_shouldRespectInvariantsAndGoldenSnapshot(peiyi configuration, long seed) {
        InitializationResult result = initialize(configuration, seed, "golden");
        GameStatus status = result.status;
        GameContextView context = result.logic.getGameContext();

        assertEquals(configuration, context.getPeiyi());
        assertEquals(status.getPlayerSum(), context.getAliveCounter() + context.getDeathCounter());
        assertEquals(Arrays.stream(EXPECTED_ROLES.get(configuration)).sum(), status.getPlayerSum());
        assertArrayEquals(EXPECTED_ROLES.get(configuration), countRoles(status));
        assertEquals(GameResult.NONE, context.getEndResult());
        assertTrue(context.getGameDay() >= 2, "初始化应完成首夜并进入首个白天");

        Set<Integer> characterNumbers = new HashSet<>();
        for (int player = 1; player <= status.getPlayerSum(); player++) {
            GameCharacter character = status.gc[player];
            assertNotNull(character, "玩家对象不得为空: " + player);
            assertTrue(character.actualRole >= 1 && character.actualRole <= 11,
                    "玩家职业必须处于合法范围: " + player);
            assertTrue(character.number >= 1 && character.number <= GameConstants.CHARACTER_SUM,
                    "角色编号必须处于合法范围: " + player);
            assertTrue(characterNumbers.add(character.number), "同局角色编号不得重复: " + character.number);
            assertEquals(character.isAlive(), context.isAlive(player));
        }

        String actualDigest = sha256(snapshot(result));
        System.out.printf("BASELINE %s seed=%d digest=%s random=%s%n",
                configuration, seed, actualDigest, result.randomChecksum);
        assertEquals(EXPECTED_SNAPSHOTS.get(configuration), actualDigest,
                "初始化行为基线发生变化；请先审查状态差异，再决定是否更新黄金摘要");
    }

    @Test
    void sameSeed_shouldReproduceCompleteInitializationState() {
        InitializationResult first = initialize(peiyi.daxing, 20260904L, "repeat-a");
        String firstSnapshot = snapshot(first);

        InitializationResult second = initialize(peiyi.daxing, 20260904L, "repeat-b");
        String secondSnapshot = snapshot(second);

        assertEquals(firstSnapshot, secondSnapshot);
        assertEquals(first.randomChecksum, second.randomChecksum);
    }

    @Test
    void curseKillInitialization_shouldRemainCompatible() {
        InitializationResult result = initialize(peiyi.yaoohu, 17L, "curse-golden");
        GameContext context = (GameContext) result.logic.getGameContext();

        assertEquals(List.of(10, 4), context.getDiebody());
        assertEquals("c789a5bb88211b91ae9c528497b6d6140be0a45cf801ec05484fdb18b94aee91",
                sha256(snapshot(result)));
        assertEquals("size=406 sha256=8ff208e4b9fcf87240b468fc34e79c80f1ec4ee85094a95eb1a8873faf7fb1e5",
                result.randomChecksum);
    }

    private InitializationResult initialize(peiyi configuration, long seed, String label) {
        ConstNum.setRandomSeed(seed);
        ConstNum.startRandomRecording();
        List<Event> events = new ArrayList<>();
        MainLogic logic = new MainLogic(events::add);
        GameRecordManager records = new GameRecordManager(
                tempDir.resolve(configuration + "-" + label + "-record.json").toString());
        GameStatus status = logic.start(configuration, records);
        ConstNum.stopRandomRecording();
        return new InitializationResult(logic, status, List.copyOf(events), ConstNum.getRandomChecksum());
    }

    private static int[] roles(int... counts) {
        int[] result = new int[12];
        System.arraycopy(counts, 0, result, 1, counts.length);
        return result;
    }

    private static int[] countRoles(GameStatus status) {
        int[] counts = new int[12];
        for (int player = 1; player <= status.getPlayerSum(); player++) {
            counts[status.gc[player].actualRole]++;
        }
        return counts;
    }

    private static String snapshot(InitializationResult result) {
        GameStatus status = result.status;
        GameContext context = (GameContext) result.logic.getGameContext();
        StringBuilder value = new StringBuilder(32_768);
        value.append("game=").append(status.p).append('|').append(status.gameDay).append('|')
                .append(status.getAliveCounter()).append('|').append(status.getDeathCounter()).append('|')
                .append(status.end).append('\n');
        value.append("ctx=").append(context.getInitialWolfCount()).append('|')
                .append(context.getInitialNonHumanCount()).append('|')
                .append(context.usesFanatic() ? 0 : 1).append('|')
                .append(context.getNonHumanAggression()).append('|')
                .append(context.getZhans()).append('|').append(context.getLings())
                .append('|').append(context.getLies()).append('|').append(context.getMaos()).append('|')
                .append(context.getDiebody()).append('\n');
        appendArray(value, "actualRoleindex", queriedInts(Role.values().length + 1,
                Role.values().length - 1, context::getActualRoleIndex));
        appendArray(value, "gyindex", queriedInts(3, 2, context::getGyIndex));
        appendArray(value, "rlindex", queriedInts(status.getPlayerSum() + 1,
                status.getPlayerSum(), context::getRlIndex));
        appendArray(value, "claimedRoleaskday", queriedInts(12, 11, context::getClaimedRoleAskDay));
        appendArray(value, "claimedRoleorder", queriedInts(12, 11, context::getClaimedRoleOrderCount));
        appendArray(value, "nonHumanPlan", queriedInts(status.getPlayerSum() + 1,
                status.getPlayerSum(), player -> context.getNonHumanPlan(player).legacyCode()));
        appendArray(value, "ybzw", queriedInts(status.getPlayerSum() + 1, status.getPlayerSum(),
                context::getPreliminaryDivinationTarget));
        appendArray(value, "zw", queriedInts(status.getPlayerSum() * 2 + 1, status.getPlayerSum(),
                context::getPlannedDivinationResult));
        appendArray(value, "nightDeathCount", queriedInts(GameConstants.MAX_GAME_DAYS + 1,
                GameConstants.MAX_GAME_DAYS, context::getNightDeathCount));
        value.append("doubleDeath=").append(Arrays.toString(queriedBooleans(
                GameConstants.MAX_GAME_DAYS + 1, GameConstants.MAX_GAME_DAYS,
                context::isDoubleDeathOccurred))).append('\n');

        for (int player = 1; player <= status.getPlayerSum(); player++) {
            int currentPlayer = player;
            GameCharacter character = status.gc[player];
            value.append("player=").append(player).append('|').append(character.number).append('|')
                    .append(character.actualRole).append('|').append(character.getClaimedRole()).append('|')
                    .append(character.getClaimedRoleOrder()).append('|').append(character.getComingOutDay()).append('|')
                    .append(character.getDeathReason()).append('|').append(character.getDeathDay()).append('|')
                    .append(character.nonHumanMarker).append('|')
                    .append(Arrays.toString(queriedInts(GameConstants.MAX_GAME_DAYS + 1,
                            GameConstants.MAX_GAME_DAYS,
                            day -> context.getSkillTarget(currentPlayer, day)))).append('|')
                    .append(Arrays.toString(queriedInts(GameConstants.CHARACTER_SUM + 1,
                            status.getPlayerSum(),
                            target -> context.getSuspicionValue(currentPlayer, target)))).append('|')
                    .append(Arrays.deepToString(queriedIntMatrix(GameConstants.TOP3_ARRAY_SIZE,
                            GameConstants.MAX_GAME_DAYS + 1, GameConstants.TOP3_ARRAY_SIZE - 1,
                            GameConstants.MAX_GAME_DAYS,
                            (rank, day) -> context.getTop3SuspectedPlayer(currentPlayer, rank, day))))
                    .append('|')
                    .append(Arrays.deepToString(queriedMatrix(GameConstants.CHARACTER_SUM + 1,
                            GameConstants.MAX_GAME_DAYS + 1, status.getPlayerSum(),
                            GameConstants.MAX_GAME_DAYS,
                            (target, day) -> context.isClaimedRoleScheduled(currentPlayer, target, day))))
                    .append('\n');
        }
        value.append("lined=").append(Arrays.deepToString(queriedIntMatrix(
                status.getPlayerSum() + 1, status.getPlayerSum() + 1,
                status.getPlayerSum(), status.getPlayerSum(),
                (seer, medium) -> context.isSeerMediumLinked(seer, medium) ? 1 : 0))).append('\n');
        value.append("hiddenSeer=").append(Arrays.deepToString(queriedMatrix(
                status.getPlayerSum() + 1, GameConstants.MAX_GAME_DAYS + 1,
                status.getPlayerSum(), GameConstants.MAX_GAME_DAYS,
                context::isHiddenSeerScheduled))).append('\n');
        value.append("hiddenHunter=").append(Arrays.deepToString(queriedMatrix(
                status.getPlayerSum() + 1, GameConstants.MAX_GAME_DAYS + 1,
                status.getPlayerSum(), GameConstants.MAX_GAME_DAYS,
                context::isHiddenHunterScheduled))).append('\n');
        for (Event event : result.events) {
            value.append("event=").append(event.eventname).append('|').append(event.ch1).append('|')
                    .append(event.ch2).append('|').append(event.ch3).append('\n');
        }
        value.append("random=").append(result.randomChecksum).append('\n');
        return value.toString();
    }

    private static void appendArray(StringBuilder output, String name, int[] values) {
        output.append(name).append('=').append(Arrays.toString(values)).append('\n');
    }

    private static int[] queriedInts(int length, int maxIndex, IntUnaryOperator query) {
        int[] values = new int[length];
        for (int index = 1; index <= maxIndex; index++) values[index] = query.applyAsInt(index);
        return values;
    }

    private static boolean[] queriedBooleans(int length, int maxIndex, IntPredicate query) {
        boolean[] values = new boolean[length];
        for (int index = 1; index <= maxIndex; index++) values[index] = query.test(index);
        return values;
    }

    private static boolean[][] queriedMatrix(int rows, int columns, int maxRow, int maxColumn,
                                               BiPredicate<Integer, Integer> query) {
        boolean[][] values = new boolean[rows][columns];
        for (int row = 1; row <= maxRow; row++)
            for (int column = 1; column <= maxColumn; column++)
                values[row][column] = query.test(row, column);
        return values;
    }

    private static int[][] queriedIntMatrix(int rows, int columns, int maxRow, int maxColumn,
                                             IntBinaryOperator query) {
        int[][] values = new int[rows][columns];
        for (int row = 1; row <= maxRow; row++)
            for (int column = 1; column <= maxColumn; column++)
                values[row][column] = query.applyAsInt(row, column);
        return values;
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte item : hash) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法计算初始化状态摘要", e);
        }
    }

    private record InitializationResult(
            MainLogic logic,
            GameStatus status,
            List<Event> events,
            String randomChecksum) {
    }
}
