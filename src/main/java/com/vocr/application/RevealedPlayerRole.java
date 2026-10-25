package com.vocr.application;

import com.vocr.domain.Role;

import java.util.Objects;

/** 对局结束后才可公开的玩家真实职业。 */
public record RevealedPlayerRole(int playerNumber, Role actualRole)
{
    public RevealedPlayerRole
    {
        if (playerNumber <= 0)
            throw new IllegalArgumentException("玩家编号必须大于零");
        Objects.requireNonNull(actualRole, "真实职业不能为空");
        if (actualRole == Role.NONE)
            throw new IllegalArgumentException("结算揭晓不能包含空职业");
    }
}
