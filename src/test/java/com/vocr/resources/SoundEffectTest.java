package com.vocr.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SoundEffectTest {
    private static final String RESOURCE_DIRECTORY = "/sounds/";

    @Test
    void everySoundEffectShouldResolveToOneExistingResource() {
        Set<String> resourcePaths = Arrays.stream(SoundEffect.values())
                .map(SoundEffect::resourcePath)
                .collect(Collectors.toSet());

        assertEquals(SoundEffect.values().length, resourcePaths.size(),
                "每种音效都应映射到不同的资源路径");
        for (SoundEffect soundEffect : SoundEffect.values()) {
            assertNotNull(SoundEffect.class.getResource(soundEffect.resourcePath()),
                    () -> "音效资源不存在: " + soundEffect.resourcePath());
        }
    }

    @Test
    void catalogShouldCoverEveryPackagedSoundEffect() throws IOException {
        Set<String> catalogFileNames = Arrays.stream(SoundEffect.values())
                .map(SoundEffect::resourcePath)
                .map(path -> path.substring(RESOURCE_DIRECTORY.length()))
                .collect(Collectors.toSet());

        Path soundDirectory = Path.of("src/main/resources/sounds");
        Set<String> packagedFileNames;
        try (var files = Files.list(soundDirectory)) {
            packagedFileNames = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".wav"))
                    .collect(Collectors.toSet());
        }

        assertEquals(packagedFileNames, catalogFileNames,
                "sounds 目录中的固定音效与语义目录必须一一对应");
    }
}
