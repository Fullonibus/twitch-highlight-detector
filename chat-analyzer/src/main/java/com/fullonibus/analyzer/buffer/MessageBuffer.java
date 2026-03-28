package com.fullonibus.analyzer.buffer;

import com.fullonibus.twitchirc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class MessageBuffer {

    private final Duration windowSize;
    private final Deque<ChatMessage> buffer = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public MessageBuffer(Duration windowSize) {
        this.windowSize = windowSize;
    }

    public void add(ChatMessage message) {
        lock.lock();
        try {
            buffer.addLast(message);
            evictExpired(message.getTimestamp());
        } finally {
            lock.unlock();
        }
    }

    public List<ChatMessage> snapshot() {
        lock.lock();
        try {
            evictExpired(Instant.now());
            return List.copyOf(buffer);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            evictExpired(Instant.now());
            return buffer.size();
        } finally {
            lock.unlock();
        }
    }

    public double messageRatePerSecond() {
        lock.lock();
        try {
            evictExpired(Instant.now());
            if (buffer.isEmpty()) return 0.0;
            Duration span = Duration.between(buffer.peekFirst().getTimestamp(), buffer.peekLast().getTimestamp());
            double seconds = Math.max(span.toMillis() / 1000.0, 0.1);
            return buffer.size() / seconds;
        } finally {
            lock.unlock();
        }
    }

    private void evictExpired(Instant now) {
        Instant cutoff = now.minus(windowSize);
        while (!buffer.isEmpty() && buffer.peekFirst().getTimestamp().isBefore(cutoff)) {
            buffer.pollFirst();
        }
    }
}
