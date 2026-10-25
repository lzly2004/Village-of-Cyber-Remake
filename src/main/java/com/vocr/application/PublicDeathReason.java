package com.vocr.application;

import com.vocr.domain.whyDie;

/** v1公开分类；小写核心名称仅允许出现在FINISHED视图。核心枚举不变。 */
public enum PublicDeathReason
{
    NONE, EXECUTION, DAY_CAT_CURSE, DAY_PURSUED, NIGHT_DEATH,
    chuxing, daymaozhou, dayhouzhui, beiyao, nightmaozhou, nighthouzhui, zhousha;

    static PublicDeathReason project(whyDie reason, BusinessPhase phase)
    {
        if (phase == BusinessPhase.FINISHED) return valueOf(reason.name());
        return switch (reason)
        {
            case NONE -> NONE;
            case chuxing -> EXECUTION;
            case daymaozhou -> DAY_CAT_CURSE;
            case dayhouzhui -> DAY_PURSUED;
            case beiyao, nightmaozhou, nighthouzhui, zhousha -> NIGHT_DEATH;
        };
    }

    boolean allowedIn(BusinessPhase phase)
    {
        return phase == BusinessPhase.FINISHED ? switch (this) {
            case NONE, chuxing, daymaozhou, dayhouzhui, beiyao, nightmaozhou, nighthouzhui, zhousha -> true;
            default -> false;
        } : switch (this) {
            case NONE, EXECUTION, DAY_CAT_CURSE, DAY_PURSUED, NIGHT_DEATH -> true;
            default -> false;
        };
    }
}
