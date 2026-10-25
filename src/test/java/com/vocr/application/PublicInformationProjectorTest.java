package com.vocr.application;

import com.vocr.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static com.vocr.application.PublicInformation.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicInformationProjectorTest
{
    @ParameterizedTest
    @EnumSource(whyDie.class)
    void deathClassification_shouldPreserveDayClassesAndRevealExactOnlyAtFinish(whyDie reason)
    {
        var fixture = new PublicInformationFixture();
        fixture.deaths[6] = reason; fixture.deathDays[6] = reason == whyDie.NONE ? 0 : 2;
        String expected = switch (reason) {
            case NONE -> "NONE";
            case chuxing -> "EXECUTION";
            case daymaozhou -> "DAY_CAT_CURSE";
            case dayhouzhui -> "DAY_PURSUED";
            default -> "NIGHT_DEATH";
        };
        var running = fixture.update();
        assertEquals(expected, running.view().players().get(5).deathReason().name());
        fixture.result = GameResult.VILLAGE_WIN;
        assertEquals(reason.name(), fixture.update().view().players().get(5).deathReason().name());
        assertEquals(expected, running.view().players().get(5).deathReason().name(), "old snapshots must remain unchanged");
    }

    @Test
    void reports_shouldMatchAuditDatesColorsAndDeadPublicActorsWithoutPrivateHolders()
    {
        var fixture = new PublicInformationFixture();
        var reports = fixture.update().publicInformation().skillReports();
        assertEquals(List.of(1, 2, 3, 5), reports.stream().map(SkillReport::playerNumber).toList());
        assertEquals(List.of(new SkillResult(1, 2, ReportColor.WHITE), new SkillResult(2, 3, ReportColor.BLACK),
                new SkillResult(3, 4, ReportColor.WHITE)), reports.get(0).results());
        assertEquals(List.of(2, 3), reports.get(1).results().stream().map(SkillResult::skillDay).toList());
        assertEquals(List.of(ReportColor.NOT_APPLICABLE, ReportColor.NOT_APPLICABLE),
                reports.get(2).results().stream().map(SkillResult::color).toList());
        assertEquals(List.of(1, 2), reports.get(3).results().stream().map(SkillResult::skillDay).toList());
        assertTrue(reports.get(0).exposed()); assertTrue(reports.get(3).exposed());
        assertEquals(3, fixture.context().getComingOutDay(1), "CO day must not erase already visible old reports");
        assertFalse(reports.stream().anyMatch(value -> value.playerNumber() == 4), "unclaimed actual seer and mark remain private");
        assertThrows(UnsupportedOperationException.class, () -> reports.clear());
        assertThrows(UnsupportedOperationException.class, () -> reports.getFirst().results().clear());
        fixture.roles[1] = 0;
        assertFalse(fixture.update().publicInformation().skillReports().stream().anyMatch(value -> value.playerNumber() == 1));
        assertEquals(3, reports.getFirst().results().size());
    }

    @ParameterizedTest
    @CsvSource({"-1,CLOSE", "2,CLOSE", "3,SLIGHTLY_GREATER", "5,SLIGHTLY_GREATER", "6,GREATER", "10,GREATER", "11,MUCH_GREATER"})
    void suspicion_shouldExposeOnlyOrderedTopTargetsAndBoundaryComparison(int gap, Comparison expected)
    {
        var fixture = new PublicInformationFixture();
        fixture.scores[4][2] = 100 - gap;
        var suspicion = fixture.update().publicInformation().suspicion();
        var row = suspicion.rows().stream().filter(value -> value.playerNumber() == 4).findFirst().orElseThrow();
        assertTrue(suspicion.available());
        assertEquals(List.of(1, 2), row.targets(), "zero slots removed without re-ranking");
        assertEquals(List.of(expected), row.comparisons());
        assertFalse(suspicion.rows().stream().anyMatch(value -> value.playerNumber() == 5 || value.playerNumber() == 6));
        fixture.day = 2;
        assertEquals(new SuspicionSummary(false, List.of()), fixture.update().publicInformation().suspicion());
        fixture.day = 4; fixture.result = GameResult.WOLF_WIN;
        assertEquals(new SuspicionSummary(false, List.of()), fixture.update().publicInformation().suspicion());
    }

    @Test
    void deathHistory_shouldMatchPublicFoxCatPrefixesAndHideNightSubtypes()
    {
        var fixture = new PublicInformationFixture();
        fixture.cat = 7; fixture.fox = 8; fixture.deviant = 9;
        fixture.actual[7] = 5; fixture.actual[8] = 10;
        fixture.deaths[7] = whyDie.chuxing; fixture.deathDays[7] = 3;
        fixture.deaths[8] = whyDie.chuxing; fixture.deathDays[8] = 3;
        fixture.deaths[9] = whyDie.daymaozhou; fixture.deathDays[9] = 3;
        fixture.deaths[10] = whyDie.dayhouzhui; fixture.deathDays[10] = 3;
        var day = fixture.update().publicInformation().deathHistory().get(2);
        assertFalse(day.executions().stream().anyMatch(value -> value.playerNumber() == 7 || value.playerNumber() == 8));
        assertTrue(day.executions().contains(new Execution(9, PublicDeathReason.DAY_CAT_CURSE, List.of(7))));
        assertTrue(day.executions().contains(new Execution(10, PublicDeathReason.DAY_PURSUED, List.of(8))));
        assertEquals(List.of(6), fixture.update().publicInformation().deathHistory().get(1).nightDeaths());
        fixture.deathDays[9] = 2;
        assertTrue(fixture.update().publicInformation().deathHistory().get(2).executions()
                .contains(new Execution(8, PublicDeathReason.EXECUTION, List.of())), "fox execution visible after earlier deviant death");
    }

    @Test
    void gameUpdate_shouldRejectPrematurePreciseDeathAndUnavailableSuspicion()
    {
        var fixture = new PublicInformationFixture();
        var running = fixture.update();
        var players = new java.util.ArrayList<>(running.view().players());
        var dead = players.get(5);
        players.set(5, new VisiblePlayer(dead.playerNumber(), dead.character(), false, PublicDeathReason.zhousha,
                dead.deathDay(), dead.claimedRole(), dead.claimedRoleOrder(), dead.comingOutDay()));
        var view = running.view();
        var unsafeView = new PlayerViewSnapshot(view.configuration(), view.gameDay(), view.aliveCount(),
                view.deathCount(), view.result(), players);
        assertThrows(IllegalArgumentException.class, () -> new GameUpdate(running.revision(), running.phase(),
                unsafeView, running.selections(), running.legalVoteActions(), running.claimInquiry(), running.legalTargetSelections(),
                running.voteHistory(), running.revealedRoles(), running.publicInformation(), 0, 0, List.of(), List.of()));
        fixture.day = 2;
        var early = fixture.update();
        var unsafeInformation = new PublicInformation(early.publicInformation().skillReports(),
                new SuspicionSummary(true, List.of()), early.publicInformation().deathHistory());
        assertThrows(IllegalArgumentException.class, () -> new GameUpdate(early.revision(), early.phase(),
                early.view(), early.selections(), early.legalVoteActions(), early.claimInquiry(), early.legalTargetSelections(),
                early.voteHistory(), early.revealedRoles(), unsafeInformation, 0, 0, List.of(), List.of()));
    }
}
