package com.vocr.application;

import com.vocr.domain.Event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** 单局领域事件缓冲区；核心写入，应用层发布不可变快照。 */
public final class GameEventBuffer
{
    private final Deque<Event> pendingEvents = new ArrayDeque<>();
    private final List<Event> publishedEvents = new ArrayList<>();

    public synchronized void publish(Event event)
    {
        Event publishedEvent = Objects.requireNonNull(event, "领域事件不能为空");
        pendingEvents.addLast(publishedEvent);
        publishedEvents.add(publishedEvent);
    }

    synchronized EventBatch pendingSnapshot()
    {
        return new EventBatch(publishedEvents.size(), new ArrayList<>(pendingEvents));
    }

    synchronized EventBatch snapshotAfter(long cursor)
    {
        if (cursor < 0 || cursor > publishedEvents.size())
            throw new IllegalArgumentException("事件游标超出当前对局范围: " + cursor);
        int start = Math.toIntExact(cursor);
        return new EventBatch(
                publishedEvents.size(), publishedEvents.subList(start, publishedEvents.size()));
    }

    synchronized Event poll()
    {
        return pendingEvents.pollFirst();
    }

    synchronized Event peek()
    {
        return pendingEvents.peekFirst();
    }

    synchronized void clear()
    {
        pendingEvents.clear();
    }

    synchronized void reset()
    {
        pendingEvents.clear();
        publishedEvents.clear();
    }
}
