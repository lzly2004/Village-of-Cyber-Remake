package com.vocr.core;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;
import com.vocr.domain.peiyi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContextEventQueueTest
{
    @AfterEach
    void resetRandomSeed()
    {
        ConstNum.stopRandomRecording();
        ConstNum.resetRandomSeed();
    }

    @Test
    void queue_shouldPreserveAppendOrderAndClearAsOneBuffer()
    {
        GameContext context = createContext();
        Event first = event(EventName.zco7, CharacterEnglishName.Abel);
        Event second = event(EventName.lnco18, CharacterEnglishName.Erich);

        context.queueEvent(first);
        context.queueEvent(second);

        assertTrue(context.hasPendingEvents());
        assertEquals(2, context.getPendingEventCount());
        assertSame(first, context.getPendingEvent(0));
        assertSame(second, context.getPendingEvent(1));

        context.clearPendingEvents();
        assertFalse(context.hasPendingEvents());
        assertEquals(0, context.getPendingEventCount());
        assertThrows(IndexOutOfBoundsException.class, () -> context.getPendingEvent(0));
    }

    @Test
    void queue_shouldRejectNullWithoutChangingExistingEvents()
    {
        GameContext context = createContext();
        Event existing = event(EventName.wsw, null);
        context.queueEvent(existing);

        assertThrows(IllegalArgumentException.class, () -> context.queueEvent(null));
        assertEquals(1, context.getPendingEventCount());
        assertSame(existing, context.getPendingEvent(0));
    }

    @Test
    void shuffle_shouldMatchTheExistingProjectRandomization()
    {
        GameContext context = createContext();
        List<Event> original = List.of(
                event(EventName.yjsw, CharacterEnglishName.Abel),
                event(EventName.yjsw, CharacterEnglishName.Erich),
                event(EventName.yjsw, CharacterEnglishName.Matthäus),
                event(EventName.yjsw, CharacterEnglishName.Otfried));

        ConstNum.setRandomSeed(20260906L);
        ConstNum.startRandomRecording();
        ArrayList<Event> expected = GameLogicUtils.shuffleList(original);
        ArrayList<Integer> expectedRandomSequence = ConstNum.stopRandomRecording();

        for (Event event : original)
            context.queueEvent(event);
        ConstNum.setRandomSeed(20260906L);
        ConstNum.startRandomRecording();
        context.shufflePendingEvents();
        ArrayList<Integer> actualRandomSequence = ConstNum.stopRandomRecording();

        assertEquals(expectedRandomSequence, actualRandomSequence);
        assertEquals(expected.size(), context.getPendingEventCount());
        for (int index = 0; index < expected.size(); index++)
            assertSame(expected.get(index), context.getPendingEvent(index));
    }

    private static Event event(EventName name, CharacterEnglishName character)
    {
        return new Event(name, character);
    }

    private static GameContext createContext()
    {
        GameStatus status = new GameStatus();
        status.startGame(peiyi.jianyi);
        return new GameContext(status);
    }
}
