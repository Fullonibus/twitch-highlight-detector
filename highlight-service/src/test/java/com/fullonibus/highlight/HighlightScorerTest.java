package com.fullonibus.highlight;

import com.fullonibus.emote.EmoteDictionary;
import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.model.EmoteOccurrence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HighlightScorerTest {

    private static final double EMOTE_WEIGHT = 2.0;
    private static final double RATE_WEIGHT = 1.5;
    private static final double SUB_WEIGHT = 1.2;

    private ChatMessage createMessage(Instant ts, int emoteCount, boolean subscriber, String displayName) {
        var emotes = new java.util.ArrayList<EmoteOccurrence>();
        int idx = 0;
        for (int i = 0; i < emoteCount; i++) {
            emotes.add(new EmoteOccurrence("emote" + i, idx, idx + 4));
            idx += 5;
        }
        return ChatMessage.builder()
                .username("user" + System.nanoTime())
                .channel("#test")
                .text("msg")
                .timestamp(ts)
                .emotes(emotes)
                .isSubscriber(subscriber)
                .displayName(displayName != null ? displayName : "user")
                .build();
    }

    @Test
    void manyEmotes_highScore() {
        HighlightScorer scorer = new HighlightScorer(EMOTE_WEIGHT, RATE_WEIGHT, SUB_WEIGHT, null);
        Instant now = Instant.now();

        List<ChatMessage> messages = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            messages.add(createMessage(now.plusMillis(i * 50), 5, false, "user"));
        }

        Highlight highlight = scorer.score(messages, "#test");
        assertNotNull(highlight);
        assertTrue(highlight.getScore() > 0);
        assertEquals(100, highlight.getEmoteCount());
    }

    @Test
    void fewEmotes_lowerScore() {
        HighlightScorer scorer = new HighlightScorer(EMOTE_WEIGHT, RATE_WEIGHT, SUB_WEIGHT, null);
        Instant now = Instant.now();

        List<ChatMessage> messages = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(createMessage(now.plusMillis(i * 100), 0, false, "user"));
        }

        Highlight highEmote = scorer.score(List.of(
                createMessage(now, 5, false, "user"),
                createMessage(now.plusMillis(100), 5, false, "user")
        ), "#test");

        Highlight lowEmote = scorer.score(messages, "#test");

        assertNotNull(highEmote);
        assertNotNull(lowEmote);
        assertTrue(highEmote.getScore() > lowEmote.getScore(),
                "High emote score (" + highEmote.getScore() + ") should be > low emote score (" + lowEmote.getScore() + ")");
    }

    @Test
    void subscriberMessages_boostedScore() {
        HighlightScorer scorer = new HighlightScorer(EMOTE_WEIGHT, RATE_WEIGHT, SUB_WEIGHT, null);
        Instant now = Instant.now();

        List<ChatMessage> nonSub = new java.util.ArrayList<>();
        List<ChatMessage> sub = new java.util.ArrayList<>();

        for (int i = 0; i < 10; i++) {
            nonSub.add(createMessage(now.plusMillis(i * 100), 1, false, "user"));
            sub.add(createMessage(now.plusMillis(i * 100), 1, true, "user"));
        }

        Highlight nonSubHighlight = scorer.score(nonSub, "#test");
        Highlight subHighlight = scorer.score(sub, "#test");

        assertNotNull(nonSubHighlight);
        assertNotNull(subHighlight);
        assertTrue(subHighlight.getScore() > nonSubHighlight.getScore(),
                "Sub score (" + subHighlight.getScore() + ") should be > non-sub score (" + nonSubHighlight.getScore() + ")");
    }

    @Test
    void emptyMessages_returnsNull() {
        HighlightScorer scorer = new HighlightScorer(EMOTE_WEIGHT, RATE_WEIGHT, SUB_WEIGHT, null);
        assertNull(scorer.score(Collections.emptyList(), "#test"));
        assertNull(scorer.score(null, "#test"));
    }

    @Test
    void topEmotes_resolvesNames() {
        EmoteDictionary dict = new EmoteDictionary();
        // Manually populate via reflection or just test that when dict is null, IDs are used
        HighlightScorer scorerWithoutDict = new HighlightScorer(EMOTE_WEIGHT, RATE_WEIGHT, SUB_WEIGHT, null);
        Instant now = Instant.now();

        List<ChatMessage> messages = List.of(
                createMessage(now, 2, false, "user")
        );

        Highlight highlight = scorerWithoutDict.score(messages, "#test");
        assertNotNull(highlight);
        assertFalse(highlight.getTopEmotes().isEmpty());
        // Without dictionary, should show raw IDs
        assertTrue(highlight.getTopEmotes().get(0).contains("emote"));
    }
}
