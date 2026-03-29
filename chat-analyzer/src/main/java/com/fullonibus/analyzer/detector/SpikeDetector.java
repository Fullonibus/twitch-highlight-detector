package com.fullonibus.analyzer.detector;

import com.fullonibus.analyzer.buffer.MessageBuffer;
import com.fullonibus.twitchirc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class SpikeDetector {

    private final MessageBuffer buffer;
    private final double thresholdMultiplier;
    private final Duration measurementWindow;
    private final Duration cooldown;
    private final int minMessageCount;
    private final double minEmoteDensity;
    private Instant lastSpikeTime = Instant.MIN;
    private double baselineRate = 5.0;

    private Consumer<List<ChatMessage>> spikeHandler;

    public SpikeDetector(Duration windowSize, double thresholdMultiplier, Duration cooldown,
                         int minMessageCount, double minEmoteDensity) {
        this.buffer = new MessageBuffer(windowSize);
        this.thresholdMultiplier = thresholdMultiplier;
        this.measurementWindow = windowSize;
        this.cooldown = cooldown;
        this.minMessageCount = minMessageCount;
        this.minEmoteDensity = minEmoteDensity;
    }

    public void onSpike(Consumer<List<ChatMessage>> handler) {
        this.spikeHandler = handler;
    }

    public void ingest(ChatMessage message) {
        buffer.add(message);
        checkForSpike(message.getChannel());
    }

    private void checkForSpike(String channel) {
        Instant now = Instant.now();
        if (now.isBefore(lastSpikeTime.plus(cooldown))) {
            return;
        }

        double currentRate = buffer.messageRatePerSecond();
        if (currentRate == 0) return;

        baselineRate = baselineRate * 0.95 + currentRate * 0.05;

        if (currentRate <= baselineRate * thresholdMultiplier) {
            return;
        }

        List<ChatMessage> snapshot = buffer.snapshot();
        if (snapshot.size() < minMessageCount) {
            return;
        }

        double emoteDensity = computeEmoteDensity(snapshot);
        if (emoteDensity < minEmoteDensity) {
            return;
        }

        String topEmotes = snapshot.stream()
                .flatMap(m -> m.getEmotes().stream())
                .collect(Collectors.groupingBy(
                        e -> e.getEmoteId(),
                        Collectors.summingInt(e -> 1)))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

        log.info("Spike detected in {}! rate={} msg/s, baseline={}, messages={}, emoteDensity={}, topEmotes={}",
                channel, String.format("%.1f", currentRate), String.format("%.1f", baselineRate),
                snapshot.size(), String.format("%.2f", emoteDensity), topEmotes);

        lastSpikeTime = now;
        if (spikeHandler != null) {
            spikeHandler.accept(snapshot);
        }
    }

    private double computeEmoteDensity(List<ChatMessage> messages) {
        if (messages.isEmpty()) return 0;
        long totalEmotes = messages.stream().mapToLong(m -> m.getEmotes().size()).sum();
        return (double) totalEmotes / messages.size();
    }
}
