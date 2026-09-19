package com.vocr.application;

import com.vocr.domain.Event;

import java.util.List;
import java.util.Objects;

/**
 * 当前应用更新的不可变快照。
 *
 * 现阶段包含白名单玩家视图、事件输出及其续读游标；合法动作和完整状态 revision 会在
 * 相应写操作全部迁入应用层后加入，避免提前给出不完整的一致性保证。
 */
public record GameUpdate(
        PlayerViewSnapshot view,
        long nextEventCursor,
        List<Event> events)
{
    public GameUpdate
    {
        Objects.requireNonNull(view, "玩家视图不能为空");
        if (nextEventCursor < 0)
            throw new IllegalArgumentException("下一事件游标不能为负数");
        events = List.copyOf(events);
    }
}
