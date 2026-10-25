package com.vocr.core;

/** 占卜师与灵媒师因一致黑结果建立的有向互证关系。 */
final class SeerMediumLinkState
{
    private final int playerSum;
    private final boolean[][] links;

    SeerMediumLinkState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);
        this.playerSum = playerSum;
        this.links = new boolean[playerSum + 1][playerSum + 1];
    }

    boolean isLinked(int seer, int medium)
    {
        validatePlayer(seer, "占卜师");
        validatePlayer(medium, "灵媒师");
        return links[seer][medium];
    }

    void establish(int seer, int medium)
    {
        validateDistinctPlayers(seer, medium);
        links[seer][medium] = true;
    }

    void clear(int seer, int medium)
    {
        validateDistinctPlayers(seer, medium);
        links[seer][medium] = false;
    }

    private void validateDistinctPlayers(int seer, int medium)
    {
        validatePlayer(seer, "占卜师");
        validatePlayer(medium, "灵媒师");
        if (seer == medium)
            throw new IllegalArgumentException("占卜师与灵媒师不能是同一玩家: " + seer);
    }

    private void validatePlayer(int player, String label)
    {
        if (player < 1 || player > playerSum)
            throw new IllegalArgumentException(label + "玩家索引越界: " + player);
    }
}
