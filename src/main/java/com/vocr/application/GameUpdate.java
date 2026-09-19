package com.vocr.application;

import com.vocr.domain.Event;

import java.util.List;

/**
 * 当前应用更新的不可变快照。
 *
 * 现阶段收口事件输出及其续读游标；玩家安全视图、合法动作和完整状态 revision 会在相应写操作
 * 全部迁入应用层后加入，避免提前给出不完整的一致性保证。
 */
public record GameUpdate(long nextEventCursor, List<Event> events)
{
    public GameUpdate
    {
        if (nextEventCursor < 0)
            throw new IllegalArgumentException("下一事件游标不能为负数");
        events = List.copyOf(events);
    }
}
