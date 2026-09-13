package com.vocr.application;

import com.vocr.domain.peiyi;

import java.util.Objects;

/** 开始新对局所需的最小平台无关命令。 */
public record StartGameCommand(peiyi configuration)
{
    public StartGameCommand
    {
        Objects.requireNonNull(configuration, "配役不能为空");
        if (configuration == peiyi.NONE)
            throw new IllegalArgumentException("必须选择有效配役");
    }
}
