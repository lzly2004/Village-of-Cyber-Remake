package com.vocr.application;

import com.vocr.domain.Event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** 单局领域事件缓冲区；核心写入，应用层标注展示阶段并发布不可变快照。 */
public final class GameEventBuffer
{
    private final Deque<PresentedEvent> pendingEvents = new ArrayDeque<>();
    private final List<PresentedEvent> publishedEvents = new ArrayList<>();

    public synchronized void publish(Event event)
    {
        Event publishedEvent = Objects.requireNonNull(event, "领域事件不能为空");
        PresentedEvent presentation = EventPresentationProjector.project(publishedEvent);
        pendingEvents.addLast(presentation);
        publishedEvents.add(presentation);
    }

    synchronized EventBatch pendingSnapshot()
    {
        long nextCursor = publishedEvents.size();
        long cursor = nextCursor - pendingEvents.size();
        return new EventBatch(cursor, nextCursor, new ArrayList<>(pendingEvents));
    }

    synchronized EventBatch snapshotAfter(long cursor)
    {
        if (cursor < 0 || cursor > publishedEvents.size())
            throw new IllegalArgumentException("事件游标超出当前对局范围: " + cursor);
        int start = Math.toIntExact(cursor);
        return new EventBatch(
                cursor,
                publishedEvents.size(),
                publishedEvents.subList(start, publishedEvents.size()));
    }

    synchronized long currentCursor()
    {
        return publishedEvents.size();
    }

    synchronized PresentedEvent poll()
    {
        return pendingEvents.pollFirst();
    }

    synchronized PresentedEvent peek()
    {
        return pendingEvents.peekFirst();
    }

    synchronized void markDialogueAfter(long cursor, EventPresentationStage stage)
    {
        if (cursor < 0 || cursor > publishedEvents.size())
            throw new IllegalArgumentException("事件游标超出当前对局范围: " + cursor);
        if (stage != EventPresentationStage.DAY_DIALOGUE
                && stage != EventPresentationStage.AFTERNOON_DIALOGUE)
            throw new IllegalArgumentException("只能重标记对话展示阶段: " + stage);

        int pendingStart = publishedEvents.size() - pendingEvents.size();
        for (int index = Math.toIntExact(cursor); index < publishedEvents.size(); index++)
        {
            PresentedEvent current = publishedEvents.get(index);
            if (current.stage() == EventPresentationStage.DAY_DIALOGUE)
                publishedEvents.set(index, new PresentedEvent(current.event(), stage));
        }
        pendingEvents.clear();
        pendingEvents.addAll(publishedEvents.subList(pendingStart, publishedEvents.size()));
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
