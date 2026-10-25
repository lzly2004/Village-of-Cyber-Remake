package com.vocr.domain;

public enum GameResult {
    NONE(0),
    VILLAGE_WIN(1),
    WOLF_WIN(2),
    FOX_WIN(3);

    private final int value;

    GameResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
