package com.vocr.application;

import java.util.List;

/** 事件游标窗口 {@code [cursor, nextCursor)} 内的不可变事件批次。 */
record EventBatch(long cursor, long nextCursor, List<PresentedEvent> presentedEvents)
{
    EventBatch
    {
        if (cursor < 0 || nextCursor < cursor)
            throw new IllegalArgumentException("事件游标窗口无效");
        presentedEvents = List.copyOf(presentedEvents);
        if (nextCursor - cursor != presentedEvents.size())
            throw new IllegalArgumentException("事件数量与游标窗口不一致");
    }
}
