package com.vocr.core;

/** 非人阵营当前的公开身份计划；STAY_HIDDEN 不是共有者职业。 */
enum NonHumanPlan
{
    NONE(0, 0),
    CLAIM_SEER(1, 1),
    CLAIM_MEDIUM(2, 2),
    CLAIM_HUNTER(3, 3),
    STAY_HIDDEN(4, 0),
    CLAIM_CAT(5, 5);

    private final int legacyCode;
    private final int claimedRole;

    NonHumanPlan(int legacyCode, int claimedRole)
    {
        this.legacyCode = legacyCode;
        this.claimedRole = claimedRole;
    }

    int legacyCode()
    {
        return legacyCode;
    }

    boolean isClaimPlan()
    {
        return claimedRole != 0;
    }

    boolean claimsRole(int role)
    {
        return claimedRole == role && isClaimPlan();
    }

    boolean isSeerOrMediumClaim()
    {
        return this == CLAIM_SEER || this == CLAIM_MEDIUM;
    }

    int claimedRole()
    {
        if (!isClaimPlan())
            throw new IllegalStateException("当前非人计划不包含可公开职业: " + this);
        return claimedRole;
    }

    static NonHumanPlan claimRole(int role)
    {
        return switch (role)
        {
            case 1 -> CLAIM_SEER;
            case 2 -> CLAIM_MEDIUM;
            case 3 -> CLAIM_HUNTER;
            case 5 -> CLAIM_CAT;
            default -> throw new IllegalArgumentException("非人不能计划公开该职业: " + role);
        };
    }
}
