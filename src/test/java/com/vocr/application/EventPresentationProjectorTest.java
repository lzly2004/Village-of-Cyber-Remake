package com.vocr.application;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventPresentationProjectorTest
{
    @Test
    void shouldClassifyIntrinsicEventStagesAndDefaultDialogue()
    {
        assertStages(
                EventPresentationStage.NIGHT_RESULT,
                EventName.wsw, EventName.yjsw);
        assertStages(
                EventPresentationStage.EXECUTION,
                EventName.cxs, EventName.hzsw, EventName.mzsw);
        assertStages(
                EventPresentationStage.SETTLEMENT,
                EventName.crsl, EventName.rlsl, EventName.yhsl, EventName.krsl);

        EnumSet<EventName> dialogueNames = EnumSet.allOf(EventName.class);
        dialogueNames.removeAll(EnumSet.of(
                EventName.wsw, EventName.yjsw,
                EventName.cxs, EventName.hzsw, EventName.mzsw,
                EventName.crsl, EventName.rlsl, EventName.yhsl, EventName.krsl));
        for (EventName name : dialogueNames)
            assertEquals(
                    EventPresentationStage.DAY_DIALOGUE,
                    EventPresentationProjector.project(event(name)).stage(),
                    name.toString());
        assertEquals(
                EventPresentationStage.DAY_DIALOGUE,
                EventPresentationProjector.project(event(null)).stage());
    }

    @Test
    void shouldRetagOnlyDialogueEventsPublishedByInterruptedVote()
    {
        GameEventBuffer buffer = new GameEventBuffer();
        Event earlier = event(EventName.zco7);
        Event avoidance = event(EventName.lrco);
        Event intrinsic = event(EventName.yjsw);
        buffer.publish(earlier);
        long cursor = buffer.currentCursor();
        buffer.publish(avoidance);
        buffer.publish(intrinsic);

        buffer.markDialogueAfter(cursor, EventPresentationStage.AFTERNOON_DIALOGUE);

        assertEquals(List.of(
                EventPresentationStage.DAY_DIALOGUE,
                EventPresentationStage.AFTERNOON_DIALOGUE,
                EventPresentationStage.NIGHT_RESULT),
                buffer.snapshotAfter(0).presentedEvents().stream()
                        .map(PresentedEvent::stage).toList());
        assertEquals(
                EventPresentationStage.DAY_DIALOGUE,
                buffer.poll().stage(),
                "重标记不得改动游标之前的待展示事件");
        assertThrows(IllegalArgumentException.class, () ->
                buffer.markDialogueAfter(0, EventPresentationStage.EXECUTION));
    }

    private static void assertStages(EventPresentationStage stage, EventName... names)
    {
        for (EventName name : names)
            assertEquals(stage, EventPresentationProjector.project(event(name)).stage());
    }

    private static Event event(EventName name)
    {
        return new Event(name, CharacterEnglishName.Abel);
    }
}
