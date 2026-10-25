package com.vocr.application;

import com.vocr.domain.Role;

import java.util.HashSet;
import java.util.List;

/** 当前业务阶段允许询问 CO 的职业与玩家。 */
public record ClaimInquiryOptions(List<Role> roles, List<Integer> players)
{
    public ClaimInquiryOptions
    {
        roles = List.copyOf(roles);
        players = List.copyOf(players);
        if (new HashSet<>(roles).size() != roles.size())
            throw new IllegalArgumentException("可询问职业不能重复");
        if (players.stream().anyMatch(player -> player <= 0)
                || new HashSet<>(players).size() != players.size())
            throw new IllegalArgumentException("可询问玩家必须为不重复的正数: " + players);
    }

    public static ClaimInquiryOptions unavailable()
    {
        return new ClaimInquiryOptions(List.of(), List.of());
    }

    public boolean available()
    {
        return !roles.isEmpty() && !players.isEmpty();
    }
}
