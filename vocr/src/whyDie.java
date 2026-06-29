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
}