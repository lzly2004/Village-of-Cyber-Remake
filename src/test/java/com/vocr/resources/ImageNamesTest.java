package com.vocr.resources;

import com.vocr.domain.SelectionType;
import com.vocr.domain.whyDie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageNamesTest {

    @Test
    void shouldBuildPageAndSetupImageNames() {
        assertEquals("game3.png", ImageNames.gameSetupButton(3));
        assertEquals("game3-1.png", ImageNames.gameSetupTitle(3));
        assertEquals("gameTitle0.png", ImageNames.recordTitle(0));
        assertEquals("9day.png", ImageNames.voteDay(9));
    }

    @Test
    void shouldBuildCharacterImageNames() {
        assertEquals("01s.png", ImageNames.characterStatusPortrait(1, true));
        assertEquals("01gs.png", ImageNames.characterStatusPortrait(1, false));
        assertEquals("31ws.png", ImageNames.characterRolePortrait(31, 7));
        assertEquals("31ws.png", ImageNames.characterResultPortrait(31, whyDie.NONE, 7));
        assertEquals("31gs.png", ImageNames.characterResultPortrait(31, whyDie.chuxing, 7));
        assertEquals("31job.png", ImageNames.characterText(31));
    }

    @Test
    void shouldBuildClaimAndSkillResultImageNames() {
        assertEquals("yaku1_2.png", ImageNames.claimedRoleIcon(1, 2));
        assertEquals("yaku5.png", ImageNames.claimedRoleIcon(5, 0));
        assertEquals("result1_2black.png", ImageNames.skillResultIcon(1, 2, true));
        assertEquals("result2_1white.png", ImageNames.skillResultIcon(2, 1, false));
        assertEquals("result3_2.png", ImageNames.skillResultIcon(3, 2, true));
        assertEquals("result1_2white.png",
                ImageNames.selectionResultIcon(SelectionType.DIVINATION, 2));
        assertEquals("result3_2.png", ImageNames.selectionResultIcon(SelectionType.GUARD, 2));
        assertEquals("uranai2.png",
                ImageNames.claimedRoleActionIcon(SelectionType.DIVINATION, 2));
        assertEquals("goei2.png", ImageNames.claimedRoleActionIcon(SelectionType.GUARD, 2));
        assertThrows(IllegalArgumentException.class,
                () -> ImageNames.selectionResultIcon(SelectionType.VOTE, 1));
    }

    @Test
    void shouldBuildDeathMarkerImageNames() {
        assertEquals("", ImageNames.deathMarker(whyDie.NONE));
        assertEquals("turi.png", ImageNames.deathMarker(whyDie.chuxing));
        assertEquals("noroi.png", ImageNames.deathMarker(whyDie.daymaozhou));
        assertEquals("atooi.png", ImageNames.deathMarker(whyDie.dayhouzhui));
        assertEquals("kami.png", ImageNames.deathMarker(whyDie.beiyao));
        assertEquals("", ImageNames.deathMarker(-1));
    }
}
