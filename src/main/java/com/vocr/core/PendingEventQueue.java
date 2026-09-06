package com.vocr.core;

import com.vocr.domain.Event;

import java.util.ArrayList;

/**
 * 待交付事件的唯一存储，保持事件的追加顺序并负责夜间事件重排。
 */
final class PendingEventQueue
{
    private final ArrayList<Event> events = new ArrayList<>();

    void add(Event event)
    {
        if (event == null)
            throw new IllegalArgumentException("待交付事件不能为空");
        events.add(event);
    }

    Event get(int index)
    {
        return events.get(index);
    }

    int size()
    {
        return events.size();
    }

    boolean isEmpty()
    {
        return events.isEmpty();
    }

    void clear()
    {
        events.clear();
    }

    void shuffle()
    {
        ArrayList<Event> shuffled = GameLogicUtils.shuffleList(events);
        events.clear();
        events.addAll(shuffled);
    }
}
