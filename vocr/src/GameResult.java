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

    public static GameResult fromValue(int value) {
        for (GameResult result : values()) {
            if (result.value == value) {
                return result;
            }
        }
        return NONE;
    }
}