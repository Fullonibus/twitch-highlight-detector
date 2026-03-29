package com.fullonibus.emote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmoteDictionaryTest {

    @Test
    void resolveEmoteName_unknownId_returnsId() {
        EmoteDictionary dict = new EmoteDictionary();
        assertEquals("unknownEmote", dict.resolveEmoteName("unknownEmote"));
    }

    @Test
    void size_startsAtZero() {
        EmoteDictionary dict = new EmoteDictionary();
        assertEquals(0, dict.size());
    }

    @Test
    void isKnownEmote_unknown_returnsFalse() {
        EmoteDictionary dict = new EmoteDictionary();
        assertFalse(dict.isKnownEmote("nonexistent"));
    }

    @Test
    void getEmoteMap_isEmptyInitially() {
        EmoteDictionary dict = new EmoteDictionary();
        assertTrue(dict.getEmoteMap().isEmpty());
    }
}
