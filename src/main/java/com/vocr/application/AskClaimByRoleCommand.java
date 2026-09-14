package com.vocr.application;

import com.vocr.domain.Role;

import java.util.Objects;

/** 按可公开声称的职业询问 CO。 */
public record AskClaimByRoleCommand(Role role)
{
    public AskClaimByRoleCommand
    {
        Objects.requireNonNull(role, "询问职业不能为空");
        switch (role)
        {
            case zhan, ling, lie, gong, mao -> { }
            default -> throw new IllegalArgumentException("不可询问该职业 CO: " + role);
        }
    }
}
