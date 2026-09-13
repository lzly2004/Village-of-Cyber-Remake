package com.vocr.resources;

import java.util.List;
import java.util.Optional;

/** 固定 BGM 的语义目录，并集中维护游戏内切歌顺序与可选说明文本。 */
public enum BgmTrack {
    START_MENU("start_menu.wav"),
    INFO("Info.wav"),
    DEFEAT_SCREEN("失败画面.wav"),
    VICTORY_SCREEN("胜利画面.wav"),
    REPLAY_PLAYER("replay_bgm.wav"),
    REPLAY_BROWSER("纯音乐-优美旋律.wav"),

    LAST_BOSS_02("真甜呢丨先来一斤吧 - ラストボス02.wav"),
    DARK_TOMORROW("西江紫堂 - 黒き明日、清き闇.wav"),
    BLOOD("西江紫堂 - BLOOD.wav"),
    LIGHTLESS_GAZE("西江紫堂 - 灯り無き眼光.wav"),

    DEATH_IMPACT("Death Impact.wav", "Death Impact.txt"),
    PERMANENT_FIVE_COLORS("永久色五色.wav", "永久色五色.txt"),
    EMOTIONALLY_UNSTABLE("Emotionally Unstable.wav", "Emotionally Unstable.txt"),
    FELT_LOVE("FELT LOVE.wav", "FELT LOVE.txt"),
    JUST_COMPLEX("Just Complex.wav", "Just Complex.txt"),
    PEACE_OF_LOST_PUZZLE("Peace of lost puzzle.wav", "Peace of lost puzzle.txt"),
    TEEK_TEEK_TEEK("TEEK TEEK TEEK.wav", "TEEK TEEK TEEK.txt"),
    DUNGEON_09("ダンジョン09.wav", "ダンジョン09.txt"),
    LIAR_BUSINESS("ライアービジネス.wav", "ライアービジネス.txt"),
    SAMIDARE_VIVARIDE("五月雨Vivaride.wav", "五月雨Vivaride.txt"),
    VILLAGE_08("村08.wav", "村08.txt"),
    DUNGEON_14("真甜呢丨先来一斤吧 - ダンジョン14.wav", "真甜呢丨先来一斤吧 - ダンジョン14.txt"),
    SAMIDARE_VIVARIDE_REMIX(
            "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.wav",
            "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.txt");

    private static final String BGM_DIRECTORY = "/BGM/";
    private static final String DESCRIPTION_DIRECTORY = "/help/";

    private static final List<BgmTrack> LATE_GAME_PLAYLIST = List.of(
            LAST_BOSS_02,
            DARK_TOMORROW,
            BLOOD,
            LIGHTLESS_GAZE);

    private static final List<BgmTrack> NORMAL_GAME_PLAYLIST = List.of(
            DEATH_IMPACT,
            PERMANENT_FIVE_COLORS,
            EMOTIONALLY_UNSTABLE,
            FELT_LOVE,
            JUST_COMPLEX,
            PEACE_OF_LOST_PUZZLE,
            TEEK_TEEK_TEEK,
            DUNGEON_09,
            LIAR_BUSINESS,
            SAMIDARE_VIVARIDE,
            VILLAGE_08,
            DUNGEON_14,
            SAMIDARE_VIVARIDE_REMIX);

    private final String fileName;
    private final String descriptionFileName;

    BgmTrack(String fileName) {
        this(fileName, null);
    }

    BgmTrack(String fileName, String descriptionFileName) {
        this.fileName = fileName;
        this.descriptionFileName = descriptionFileName;
    }

    public String resourcePath() {
        return BGM_DIRECTORY + fileName;
    }

    public Optional<String> descriptionResourcePath() {
        return Optional.ofNullable(descriptionFileName)
                .map(file -> DESCRIPTION_DIRECTORY + file);
    }

    public static List<BgmTrack> lateGamePlaylist() {
        return LATE_GAME_PLAYLIST;
    }

    public static List<BgmTrack> normalGamePlaylist() {
        return NORMAL_GAME_PLAYLIST;
    }
}
