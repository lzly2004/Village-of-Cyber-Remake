package com.vocr.application;

import com.vocr.core.MainLogicInterface;
import com.vocr.domain.GameContextView;

import java.util.Objects;

/**
 * 平台无关的对局应用入口。
 *
 * 当前先迁移开局用例；后续业务操作按垂直切片逐步加入，Swing 与 Web 均不得绕过这里
 * 复制一份规则流程。
 */
public final class GameApplication
{
    private final MainLogicInterface gameLogic;

    public GameApplication(MainLogicInterface gameLogic)
    {
        this.gameLogic = Objects.requireNonNull(gameLogic, "主逻辑不能为空");
    }

    public StartedGame startGame(StartGameCommand command)
    {
        Objects.requireNonNull(command, "开局命令不能为空");
        gameLogic.start(command.configuration());
        GameContextView context = Objects.requireNonNull(
                gameLogic.getGameContext(), "主逻辑开局后未提供游戏状态");
        return new StartedGame(context.getPeiyi(), context.getPlayerSum(), context.getGameDay());
    }
}
