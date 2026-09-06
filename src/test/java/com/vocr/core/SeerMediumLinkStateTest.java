package com.vocr.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeerMediumLinkStateTest
{
    @Test
    void links_shouldStartEmptyAndSupportEstablishingAndClearing()
    {
        SeerMediumLinkState links = new SeerMediumLinkState(4);

        assertFalse(links.isLinked(1, 2));
        links.establish(1, 2);
        assertTrue(links.isLinked(1, 2));
        links.clear(1, 2);
        assertFalse(links.isLinked(1, 2));
    }

    @Test
    void links_shouldRemainDirected()
    {
        SeerMediumLinkState links = new SeerMediumLinkState(3);

        links.establish(1, 2);

        assertTrue(links.isLinked(1, 2));
        assertFalse(links.isLinked(2, 1));
    }

    @Test
    void links_shouldRejectInvalidWritesBeforeMutation()
    {
        assertThrows(IllegalArgumentException.class, () -> new SeerMediumLinkState(0));

        SeerMediumLinkState links = new SeerMediumLinkState(3);
        links.establish(1, 2);

        assertThrows(IllegalArgumentException.class, () -> links.isLinked(0, 2));
        assertThrows(IllegalArgumentException.class, () -> links.isLinked(1, 4));
        assertThrows(IllegalArgumentException.class, () -> links.establish(1, 1));
        assertThrows(IllegalArgumentException.class, () -> links.clear(0, 2));
        assertThrows(IllegalArgumentException.class, () -> links.clear(1, 4));

        assertTrue(links.isLinked(1, 2));
    }
}
