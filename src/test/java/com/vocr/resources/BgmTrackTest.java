package com.vocr.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BgmTrackTest {
    private static final String BGM_DIRECTORY = "/BGM/";

    @Test
    void everyBgmTrackAndDescriptionShouldResolveToExistingResources() {
        Set<String> resourcePaths = Arrays.stream(BgmTrack.values())
                .map(BgmTrack::resourcePath)
                .collect(Collectors.toSet());

        assertEquals(BgmTrack.values().length, resourcePaths.size(),
                "每首BGM都应映射到不同的资源路径");
        for (BgmTrack track : BgmTrack.values()) {
            assertNotNull(BgmTrack.class.getResource(track.resourcePath()),
                    () -> "BGM资源不存在: " + track.resourcePath());
            track.descriptionResourcePath().ifPresent(descriptionPath ->
                    assertNotNull(BgmTrack.class.getResource(descriptionPath),
                            () -> "BGM说明资源不存在: " + descriptionPath));
        }
    }

    @Test
    void catalogShouldCoverEveryPackagedBgm() throws IOException {
        Set<String> catalogFileNames = Arrays.stream(BgmTrack.values())
                .map(BgmTrack::resourcePath)
                .map(path -> path.substring(BGM_DIRECTORY.length()))
                .collect(Collectors.toSet());

        Set<String> packagedFileNames;
        try (var files = Files.list(Path.of("src/main/resources/BGM"))) {
            packagedFileNames = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".wav"))
                    .collect(Collectors.toSet());
        }

        assertEquals(packagedFileNames, catalogFileNames,
                "BGM目录中的音频与语义目录必须一一对应");
    }

    @Test
    void gamePlaylistsShouldKeepTheirExistingOrder() {
        assertEquals(List.of(
                BgmTrack.LAST_BOSS_02,
                BgmTrack.DARK_TOMORROW,
                BgmTrack.BLOOD,
                BgmTrack.LIGHTLESS_GAZE), BgmTrack.lateGamePlaylist());
        assertEquals(List.of(
                BgmTrack.DEATH_IMPACT,
                BgmTrack.PERMANENT_FIVE_COLORS,
                BgmTrack.EMOTIONALLY_UNSTABLE,
                BgmTrack.FELT_LOVE,
                BgmTrack.JUST_COMPLEX,
                BgmTrack.PEACE_OF_LOST_PUZZLE,
                BgmTrack.TEEK_TEEK_TEEK,
                BgmTrack.DUNGEON_09,
                BgmTrack.LIAR_BUSINESS,
                BgmTrack.SAMIDARE_VIVARIDE,
                BgmTrack.VILLAGE_08,
                BgmTrack.DUNGEON_14,
                BgmTrack.SAMIDARE_VIVARIDE_REMIX), BgmTrack.normalGamePlaylist());
    }
}
