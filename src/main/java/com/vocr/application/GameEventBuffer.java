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
    private final Deque<Event> events = new ArrayDeque<>();

    public synchronized void publish(Event event)
    {
        events.addLast(Objects.requireNonNull(event, "领域事件不能为空"));
    }

    synchronized List<Event> snapshot()
    {
        return List.copyOf(new ArrayList<>(events));
    }

    synchronized Event poll()
    {
        return events.pollFirst();
    }

    synchronized Event peek()
    {
        return events.peekFirst();
    }

    synchronized void clear()
    {
        events.clear();
    }
}
