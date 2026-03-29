package com.fullonibus.analyzer.buffer;

import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.model.EmoteOccurrence;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MessageBufferTest {

    private ChatMessage createMessage(Instant timestamp) {
        return ChatMessage.builder()
                .username("user")
                .channel("#test")
                .text("hello")
                .timestamp(timestamp)
                .emotes(Collections.emptyList())
                .isSubscriber(false)
                .build();
    }

    @Test
    void addMessages_checkSize() {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(60));
        Instant now = Instant.now();

        for (int i = 0; i < 10; i++) {
            buffer.add(createMessage(now.plusSeconds(i)));
        }

        assertEquals(10, buffer.size());
    }

    @Test
    void expiredMessagesAreEvicted() {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(10));
        Instant now = Instant.now();

        // Add messages outside the window
        buffer.add(createMessage(now.minusSeconds(20)));
        buffer.add(createMessage(now.minusSeconds(15)));

        assertEquals(0, buffer.size());
    }

    @Test
    void partialEviction_keepsRecent() {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(10));
        Instant now = Instant.now();

        buffer.add(createMessage(now.minusSeconds(20))); // expired
        buffer.add(createMessage(now.minusSeconds(5)));  // within window
        buffer.add(createMessage(now));                    // within window

        assertEquals(2, buffer.size());
    }

    @Test
    void messageRatePerSecond_calculation() {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(60));
        Instant now = Instant.now();

        for (int i = 0; i < 10; i++) {
            buffer.add(createMessage(now.plusMillis(i * 100)));
        }

        double rate = buffer.messageRatePerSecond();
        assertTrue(rate > 8.0, "Expected rate > 8, got " + rate);
        assertTrue(rate < 12.0, "Expected rate < 12, got " + rate);
    }

    @Test
    void messageRatePerSecond_emptyBuffer() {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(60));
        assertEquals(0.0, buffer.messageRatePerSecond());
    }

    @Test
    void snapshot_returnsCopy() {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(60));
        Instant now = Instant.now();

        buffer.add(createMessage(now));
        List<ChatMessage> snapshot = buffer.snapshot();

        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(createMessage(now)));
    }

    @Test
    void threadSafety_concurrentAdds() throws InterruptedException {
        MessageBuffer buffer = new MessageBuffer(Duration.ofSeconds(60));
        Instant now = Instant.now();
        int threadCount = 10;
        int messagesPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        buffer.add(createMessage(now.plusNanos(i)));
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(), "Errors during concurrent adds: " + errors);
        assertEquals(threadCount * messagesPerThread, buffer.size());
    }
}
