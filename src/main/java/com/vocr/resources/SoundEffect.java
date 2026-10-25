package com.vocr.resources;

/**
 * 固定用途音效的稳定语义标识。
 *
 * <p>界面只选择音效用途，不再依赖实际文件名；桌面资源适配器负责把用途解析为
 * classpath 中的 WAV 文件。</p>
 */
public enum SoundEffect {
    CLICK("click.wav"),
    DAY_EXECUTION("白天处刑音效.wav"),
    PEACEFUL_NIGHT("平和音效.wav"),
    NIGHT_DEATH("夜间死亡音效.wav"),
    VILLAGE_VICTORY("村人胜利音效.wav"),
    WOLF_VICTORY("人狼胜利音效.wav"),
    FOX_VICTORY("妖狐胜利音效.wav"),
    NIGHTFALL("入夜音效.wav"),
    WOLF_HOWL("狼嚎音效.wav");

    private static final String RESOURCE_DIRECTORY = "/sounds/";

    private final String fileName;

    SoundEffect(String fileName) {
        this.fileName = fileName;
    }

    public String resourcePath() {
        return RESOURCE_DIRECTORY + fileName;
    }
}
