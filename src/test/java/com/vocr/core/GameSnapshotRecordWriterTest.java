package com.vocr.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.config.ProbabilityCalculator;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplaySave;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSnapshotRecordWriterTest
{
    @TempDir
    Path tempDir;

    @Test
    void writer_shouldPreserveDailyStateFormat()
    {
        GameContext context = createContext(createStatus());
        int seer = context.getActualRoleIndex(Role.zhan.ordinal());
        context.registerClaim(seer, Role.zhan.ordinal());
        context.markNonHuman(seer);
        context.recordSkillTarget(seer, 1,
                context.getPlayerSum() + context.getActualRoleIndex(Role.ling.ordinal()));
        List<String> records = new ArrayList<>();
        GameSnapshotRecordWriter writer = new GameSnapshotRecordWriter(records);

        writer.writeDailySnapshot(2, context);

        assertEquals(List.of(
                "DAILY_SNAPSHOT|day=2|alive=" + context.getAliveCounter(),
                expectedDailyState(2, context)), records);
    }

    @Test
    void recorder_shouldKeepDailyAndVoteSnapshotsInactiveBeforeStart()
    {
        GameContext context = createContext(createStatus());
        GameRecorder recorder = new GameRecorder(
                "inactive-snapshot", new GameRecordManager(tempDir.resolve("record.json").toString()));

        recorder.recordDailySnapshot(2, context);
        recorder.recordVoteData(2, context);

        assertTrue(recorder.getRecords().isEmpty());
    }

    @Test
    void nightlyFakeHunterDiary_shouldRemainChronologicalThroughLateCoAndReplay() throws Exception
    {
        GameContext context = createContext(createStatus());
        int fakeHunter = context.getActualRoleIndex(Role.renlang.ordinal());
        SuspicionSystem suspicion = context.getSuspicion();
        HunterGuarder hunterGuarder = new HunterGuarder(context, suspicion);
        Fabricator fabricator = new Fabricator(
                context, suspicion, new SeerDiviner(context, suspicion), hunterGuarder);
        List<String> records = new ArrayList<>();
        GameSnapshotRecordWriter writer = new GameSnapshotRecordWriter(records);

        context.getRandom().reseedForTest(20260912L);
        context.getRandom().startRecording();
        int firstTarget;
        try
        {
            while (context.getGameDay() < 2) context.incrementGameDay();
            fabricator.prepareFakeHunterDiaries();
            firstTarget = context.getSkillTarget(fakeHunter, 2);
            assertTrue(firstTarget > 0);

            // 目标是在护卫记录确定后才于当夜死亡；历史必须保留这个当时合法的选择。
            context.recordDeath(firstTarget, whyDie.beiyao);
            for (int skillDay = 3; skillDay <= 8; skillDay++)
            {
                context.incrementGameDay();
                fabricator.prepareFakeHunterDiaries();
                assertTrue(context.getSkillTarget(fakeHunter, skillDay) > 0);
            }
            assertTrue(context.getRandom().stopRecording().isEmpty(), "假猎人日记不得消耗主游戏随机序列");
        }
        finally
        {
            context.getRandom().stopRecording();
        }

        context.incrementGameDay();
        writer.writeDailySnapshot(9, context);
        int[] diaryBeforeCo = new int[9];
        for (int skillDay = 2; skillDay <= 8; skillDay++)
            diaryBeforeCo[skillDay] = context.getSkillTarget(fakeHunter, skillDay);

        COManager coManager = new COManager(context, suspicion,
                new ResultPresenter(context, suspicion), new ProbabilityCalculator());
        coManager.processActualCo(fakeHunter, Role.lie.ordinal(), List.of());
        for (int skillDay = 2; skillDay <= 8; skillDay++)
            assertEquals(diaryBeforeCo[skillDay], context.getSkillTarget(fakeHunter, skillDay),
                    "猎人 CO 不得重新生成过去的日记");
        writer.writeDailySnapshot(10, context);

        ReplaySave replay = ReplaySave.fromRecords(0, records);
        assertEquals(0, replay.daySnapshots.get(0).players[fakeHunter - 1].claimedRole);
        assertEquals(Role.lie.ordinal(), replay.daySnapshots.get(1).players[fakeHunter - 1].claimedRole);
        assertEquals(firstTarget, replay.getVisibleSkillTarget(fakeHunter, 2, 9));
        assertTrue(context.isDead(firstTarget), "日记应允许保留后来死亡的历史护卫对象");
        for (int skillDay = 3; skillDay <= 8; skillDay++)
        {
            assertEquals(context.getSkillTarget(fakeHunter, skillDay),
                    replay.getVisibleSkillTarget(fakeHunter, skillDay, 9));
        }

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ReplaySave roundTripped = mapper.readValue(mapper.writeValueAsBytes(replay), ReplaySave.class);
        for (int skillDay = 2; skillDay <= 8; skillDay++)
            assertEquals(context.getSkillTarget(fakeHunter, skillDay),
                    roundTripped.getVisibleSkillTarget(fakeHunter, skillDay, 9));
    }

    private static String expectedDailyState(int day, GameContext context)
    {
        StringBuilder state = new StringBuilder("STATE|day=" + day + "|");
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            state.append("{").append(String.format(
                    "%d:n=%d,r=%d,cr=%d,cro=%d,dd=%d,wd=%d,nm=%d,cod=%d,st=%d,sh=%s",
                    player, context.getCharacterNumber(player), context.getActualRole(player),
                    context.getClaimedRole(player), context.getClaimedRoleOrder(player),
                    context.getDeathDay(player), context.getDeathReason(player).ordinal(),
                    context.isNonHumanMarked(player) ? 1 : 0, context.getComingOutDay(player),
                    context.getSkillTarget(player, day - 1), expectedSkillHistory(context, player, day)))
                    .append("}");
        }
        return state.toString();
    }

    private static String expectedSkillHistory(GameContext context, int player, int snapshotDay)
    {
        StringBuilder history = new StringBuilder("0");
        for (int skillDay = 1; skillDay < snapshotDay; skillDay++)
            history.append('/').append(context.getSkillTarget(player, skillDay));
        return history.toString();
    }

    private static GameStatus createStatus()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return status;
    }

    private static GameContext createContext(GameStatus status)
    {
        return new GameContext(status, NonHumanConfiguration.from(peiyi.jianyi, 0));
    }
}
