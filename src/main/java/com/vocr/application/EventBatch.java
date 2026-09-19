package com.vocr.application;

import com.vocr.domain.Event;

import java.util.List;

/** 从某个事件游标开始读取的不可变事件批次。 */
record EventBatch(long nextCursor, List<Event> events)
{
    EventBatch
    {
        events = List.copyOf(events);
    }
}
