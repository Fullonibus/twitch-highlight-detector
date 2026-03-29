package com.fullonibus.analyzer.detector;

import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.model.EmoteOccurrence;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class SpikeDetectorTest {

    private static final Duration WINDOW = Duration.ofSeconds(30);
    private static final double THRESHOLD = 1.5;
    private static final Duration COOLDOWN = Duration.ofSeconds(30);
    private static final int MIN_MSG_COUNT = 5;
    private static final double MIN_EMOTE_DENSITY = 0.1;

    private ChatMessage createMessage(Instant ts, int emoteCount, String channel) {
        String username = "user" + System.nanoTime();
        StringBuilder text = new StringBuilder("msg");
        var emotes = new java.util.ArrayList<EmoteOccurrence>();
        int idx = 0;
        for (int i = 0; i < emoteCount; i++) {
            emotes.add(new EmoteOccurrence("emote" + i, idx, idx + 4));
            idx += 5;
            text.append(" Kappa");
        }
        return ChatMessage.builder()
                .username(username)
                .channel(channel != null ? channel : "#test")
                .text(text.toString())
                .timestamp(ts)
                .emotes(emotes)
                .isSubscriber(false)
                .build();
    }

    @Test
    void steadyChat_noSpike() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Steady 1 msg/sec over 10 seconds
        for (int i = 0; i < 10; i++) {
            detector.ingest(createMessage(now.plusSeconds(i), 0, "#test"));
        }

        assertTrue(spikes.isEmpty());
    }

    @Test
    void gradualIncrease_noSpike() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Gradual increase: 1, 2, 3, 4, 5 msgs per burst over 5 seconds
        int msgNum = 0;
        for (int burst = 1; burst <= 5; burst++) {
            Instant burstTime = now.plusSeconds(burst);
            for (int j = 0; j < burst; j++) {
                detector.ingest(createMessage(burstTime.plusMillis(j * 50), 1, "#test"));
                msgNum++;
            }
        }

        assertTrue(spikes.isEmpty(), "Should not spike on gradual increase");
    }

    @Test
    void suddenSpikeWithEmotes_spikeDetected() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Feed many low-rate messages to drive baseline down
        // Space them far apart so they don't accumulate in the window
        for (int i = 0; i < 50; i++) {
            detector.ingest(createMessage(now.minusSeconds(60 + i * 35), 0, "#test"));
        }

        // Sudden burst: 20 messages with emotes in 1 second
        for (int i = 0; i < 20; i++) {
            detector.ingest(createMessage(now.plusMillis(i * 50), 3, "#test"));
        }

        assertFalse(spikes.isEmpty(), "Should detect spike");
        assertTrue(spikes.get(0).size() >= MIN_MSG_COUNT);
    }

    @Test
    void spikeDuringCooldown_noSecondSpike() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Baseline - spaced far apart
        for (int i = 0; i < 50; i++) {
            detector.ingest(createMessage(now.minusSeconds(60 + i * 35), 0, "#test"));
        }

        // First spike
        for (int i = 0; i < 20; i++) {
            detector.ingest(createMessage(now.plusMillis(i * 50), 3, "#test"));
        }

        int afterFirstSpike = spikes.size();
        assertTrue(afterFirstSpike > 0, "First spike should have been detected");

        // Second spike within cooldown
        for (int i = 0; i < 20; i++) {
            detector.ingest(createMessage(now.plusSeconds(5).plusMillis(i * 50), 3, "#test"));
        }

        assertEquals(afterFirstSpike, spikes.size(), "Second spike during cooldown should be ignored");
    }

    @Test
    void multipleRapidSpikes_onlyFirstTriggers() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Baseline - spaced far apart
        for (int i = 0; i < 50; i++) {
            detector.ingest(createMessage(now.minusSeconds(60 + i * 35), 0, "#test"));
        }

        // Three rapid spikes
        for (int s = 0; s < 3; s++) {
            Instant spikeTime = now.plusSeconds(s * 2);
            for (int i = 0; i < 20; i++) {
                detector.ingest(createMessage(spikeTime.plusMillis(i * 50), 3, "#test"));
            }
        }

        assertEquals(1, spikes.size(), "Only first spike should trigger");
    }

    @Test
    void spikeBelowMinMessageCount_noSpike() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Only 3 messages with high rate but below min count
        for (int i = 0; i < 3; i++) {
            detector.ingest(createMessage(now.plusMillis(i * 10), 3, "#test"));
        }

        assertTrue(spikes.isEmpty());
    }

    @Test
    void spikeBelowMinEmoteDensity_noSpike() {
        SpikeDetector detector = new SpikeDetector(WINDOW, THRESHOLD, COOLDOWN, MIN_MSG_COUNT, MIN_EMOTE_DENSITY);
        List<List<ChatMessage>> spikes = new CopyOnWriteArrayList<>();
        detector.onSpike(spikes::add);

        Instant now = Instant.now();
        // Baseline - spaced far apart
        for (int i = 0; i < 50; i++) {
            detector.ingest(createMessage(now.minusSeconds(60 + i * 35), 0, "#test"));
        }

        // 20 messages with 0 emotes (density = 0, below 0.1 threshold)
        for (int i = 0; i < 20; i++) {
            detector.ingest(createMessage(now.plusMillis(i * 50), 0, "#test"));
        }

        assertTrue(spikes.isEmpty(), "Should not spike without minimum emote density");
    }
}
