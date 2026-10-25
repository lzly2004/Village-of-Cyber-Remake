package com.vocr.ui;

import com.vocr.replay.DaySnapshot;
import com.vocr.replay.ReplaySave;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayTextFormatterTest
{
    @Test
    void normalInfo_shouldPreserveLegacySkillAndDeathText() throws Exception
    {
        ReplaySave replay = legacyReplay();

        assertEquals("""
                [占い師]
                宝石 : 少年○
                [霊能者]
                [処刑]

                [死体]
                平和
                [護衛先]
                """, ReplayTextFormatter.buildNormalInfo(replay, 2, 3));
    }

    @Test
    void normalInfo_shouldShowEveryNightOfLateHunterDiary()
    {
        ReplaySave replay = lateHunterReplay();

        String result = ReplayTextFormatter.buildNormalInfo(replay, 9, 2);

        assertTrue(result.contains("[護衛先]\n青年 : 研究→研究→研究→研究→研究→研究→研究\n"));
    }

    @Test
    void roleInfo_shouldPreserveGroupingAndDisplayOrder() throws Exception
    {
        ReplaySave replay = legacyReplay();
        DaySnapshot finalSnapshot = replay.daySnapshots.get(1);

        assertEquals("""
                一役職一
                [占卜师]
                宝石
                [人狼]
                歌姫
                [妖狐]
                少年
                """, ReplayTextFormatter.buildRoleInfo(finalSnapshot, 3));
    }

    @Test
    void voteInfo_shouldPreserveColumnsAndClampStaleRoundIndex() throws Exception
    {
        ReplaySave replay = legacyReplay();
        DaySnapshot dayTwo = replay.daySnapshots.get(0);

        ReplayTextFormatter.VoteText result = ReplayTextFormatter.buildVoteInfo(dayTwo, 2, 99, 3);

        assertEquals(0, result.roundIndex());
        assertEquals("""
                -投票結果/2日目-第1轮指定投票：
                歌姫
                宝石：0票  投票先→歌姫
                歌姫：2票  投票先→棄権
                少年：0票  投票先→歌姫
                """, result.leftColumn());
        assertEquals("\n\n", result.rightColumn());
    }

    private ReplaySave legacyReplay() throws Exception
    {
        try (InputStream fixture = getClass().getResourceAsStream("/replay/legacy-record-v0.txt"))
        {
            assertNotNull(fixture);
            List<String> records = new String(fixture.readAllBytes(), StandardCharsets.UTF_8)
                    .lines().toList();
            return ReplaySave.fromRecords(0, records);
        }
    }

    private ReplaySave lateHunterReplay()
    {
        ReplaySave replay = new ReplaySave();
        replay.totalDays = 10;
        int[] diary = {0, 0, 2, 2, 2, 2, 2, 2, 2};
        for (int day = 2; day <= 10; day++)
        {
            DaySnapshot snapshot = new DaySnapshot();
            snapshot.dayNumber = day;
            snapshot.aliveCount = day < 5 ? 2 : 1;
            int targetDeathDay = day < 5 ? 0 : 4;
            int targetDeathReason = day < 5 ? 0 : 4;
            snapshot.players = new DaySnapshot.PlayerStatus[] {
                    player(1, 1, 7, day == 10 ? 3 : 0, 0, 0, diary),
                    player(2, 2, 6, 0, targetDeathDay, targetDeathReason, new int[0])
            };
            replay.daySnapshots.add(snapshot);
        }
        return replay;
    }

    private DaySnapshot.PlayerStatus player(int playerIndex, int characterNumber, int actualRole,
                                            int claimedRole, int deathDay, int deathReason,
                                            int[] skillTargets)
    {
        DaySnapshot.PlayerStatus player = new DaySnapshot.PlayerStatus();
        player.playerIndex = playerIndex;
        player.characterNumber = characterNumber;
        player.actualRole = actualRole;
        player.claimedRole = claimedRole;
        player.deathDay = deathDay;
        player.deathReason = deathReason;
        player.skillTargets = skillTargets.clone();
        return player;
    }
}
