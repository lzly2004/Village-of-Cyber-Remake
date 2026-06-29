enum whyDie
{
    NONE,
    chuxing,
    daymaozhou,
    dayhouzhui,
    beiyao,
    nightmaozhou,
    nighthouzhui,
    zhousha;

    public static final whyDie EXECUTION = chuxing;
    public static final whyDie DAY_CAT_CURSE = daymaozhou;
    public static final whyDie DAY_PURSUED = dayhouzhui;
    public static final whyDie NIGHT_BITTEN = beiyao;
    public static final whyDie NIGHT_CAT_CURSE = nightmaozhou;
    public static final whyDie NIGHT_PURSUED = nighthouzhui;
    public static final whyDie CURSE_KILL = zhousha;

    public boolean isDayDeath()
    {
        return this == chuxing || this == dayhouzhui || this == daymaozhou;
    }

    public boolean isNightDeath()
    {
        return !isDayDeath() && this != NONE;
    }

    public String getDeathIconName() {
        return switch (this) {
            case chuxing -> "turi.png";
            case daymaozhou -> "noroi.png";
            case dayhouzhui -> "atooi.png";
            case beiyao, nightmaozhou, nighthouzhui, zhousha -> "kami.png";
            case NONE -> "";
        };
    }

    public static String getDeathIconName(int deathReason) {
        if (deathReason < 0 || deathReason >= values().length) return "";
        return values()[deathReason].getDeathIconName();
    }
}