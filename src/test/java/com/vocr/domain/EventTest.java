package com.vocr.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventTest
{
    @Test
    void shortConstructor_shouldPreserveComponentsAndDefaultMissingTargets()
    {
        Event event = new Event(EventName.zco7, CharacterEnglishName.Abel);

        assertEquals(EventName.zco7, event.eventname());
        assertEquals(CharacterEnglishName.Abel, event.ch1());
        assertNull(event.ch2());
        assertNull(event.ch3());
        assertEquals(EventName.zco7 + " "
                        + CharacterKanjiName.values()[CharacterEnglishName.Abel.ordinal()] + " null",
                event.toString());
    }

    @Test
    void toString_shouldKeepLegacyNullEventNameBehavior()
    {
        assertNull(new Event(null, null).toString());
    }
}
