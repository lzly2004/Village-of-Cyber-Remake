package com.vocr.application;

/** 玩家发起白天投票时选择的候补范围。 */
public enum VotingMethod
{
    FREE(0),
    GREY(1),
    DESIGNATED(2);

    private final int legacyCode;

    VotingMethod(int legacyCode)
    {
        this.legacyCode = legacyCode;
    }

    int legacyCode()
    {
        return legacyCode;
    }

    static VotingMethod fromLegacyCode(int legacyCode)
    {
        for (VotingMethod method : values())
            if (method.legacyCode == legacyCode)
                return method;
        throw new IllegalArgumentException("未知投票方式编号: " + legacyCode);
    }
}
