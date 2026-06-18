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
    private volatile double baselineRate = 0;

    // Warm-up state
    private static final int WARMUP_MESSAGE_THRESHOLD = 20;
    private static final Duration WARMUP_TIME_THRESHOLD = Duration.ofSeconds(30);
    private static final double WARMUP_WEIGHT = 0.3;
    private static final double NORMAL_WEIGHT = 0.1;

    private final Instant startTime;
    private int totalMessageCount = 0;
    private boolean warmedUp = false;

    // Debug logging state
    private Instant lastDebugLog = Instant.now();
    private int lastDebugMessageCount = 0;

    // Optional viewer count tracker
    private ViewerCountTracker viewerCountTracker;

    private Consumer<List<ChatMessage>> spikeHandler;

    public SpikeDetector(Duration windowSize, double thresholdMultiplier, Duration cooldown,
                         int minMessageCount, double minEmoteDensity) {
        this.buffer = new MessageBuffer(windowSize);
        this.thresholdMultiplier = thresholdMultiplier;
        this.measurementWindow = windowSize;
        this.cooldown = cooldown;
        this.minMessageCount = minMessageCount;
        this.minEmoteDensity = minEmoteDensity;
        this.startTime = Instant.now();
    }

    public void setViewerCountTracker(ViewerCountTracker tracker) {
        this.viewerCountTracker = tracker;
    }

    public void onSpike(Consumer<List<ChatMessage>> handler) {
        this.spikeHandler = handler;
    }

    public void ingest(ChatMessage message) {
        buffer.add(message);
        totalMessageCount++;

        // Check warm-up condition
        if (!warmedUp) {
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (totalMessageCount >= WARMUP_MESSAGE_THRESHOLD || elapsed.compareTo(WARMUP_TIME_THRESHOLD) >= 0) {
                warmedUp = true;
                log.info("SpikeDetector warm-up complete after {} messages, {}s. baseline={}",
                        totalMessageCount, elapsed.getSeconds(), String.format("%.2f", baselineRate));
            }
        }

        log.debug("Ingested message from {} in {}, buffer size: {}", message.getUsername(), message.getChannel(), buffer.size());
        checkForSpike();

        // Periodic debug logging
        maybeDebugLog();
    }

    private void maybeDebugLog() {
        Instant now = Instant.now();
        boolean timeTrigger = now.isAfter(lastDebugLog.plusSeconds(30));
        boolean countTrigger = (totalMessageCount - lastDebugMessageCount) >= 50;
        if (timeTrigger || countTrigger) {
            double currentRate = buffer.messageRatePerSecond();
            String channel = buffer.snapshot().isEmpty() ? "?" : buffer.snapshot().get(0).getChannel();
            int viewers = getViewerCount(channel);
            log.debug("SpikeDetector stats: channel={}, messages={}, rate={}/s, baseline={}, warmedUp={}, viewers={}",
                    channel, totalMessageCount, String.format("%.2f", currentRate),
                    String.format("%.2f", baselineRate), warmedUp, viewers);
            lastDebugLog = now;
            lastDebugMessageCount = totalMessageCount;
        }
    }

    private int getViewerCount(String channel) {
        if (viewerCountTracker == null) return 0;
        String clean = channel.startsWith("#") ? channel.substring(1) : channel;
        return viewerCountTracker.getViewerCount(clean);
    }

    private void checkForSpike() {
        Instant now = Instant.now();
        if (now.isBefore(lastSpikeTime.plus(cooldown))) {
            return;
        }

        double currentRate = buffer.messageRatePerSecond();
        if (currentRate == 0) return;

        // Adapt baseline with appropriate weight
        double weight = warmedUp ? NORMAL_WEIGHT : WARMUP_WEIGHT;
        baselineRate = baselineRate * (1 - weight) + currentRate * weight;

        // Skip spike detection during warm-up
        if (!warmedUp) return;

        List<ChatMessage> snapshot = buffer.snapshot();
        if (snapshot.isEmpty()) return;
        String channel = snapshot.get(0).getChannel();

        // Compute effective rates (optionally normalized by viewer count)
        double effectiveCurrentRate = currentRate;
        double effectiveBaseline = baselineRate;

        int viewers = getViewerCount(channel);
        if (viewers >= 1) {
            double viewerFactor = viewers / 1000.0;
            effectiveCurrentRate = currentRate / viewerFactor;
            effectiveBaseline = baselineRate / viewerFactor;
        }

        if (effectiveCurrentRate <= effectiveBaseline * thresholdMultiplier) {
            return;
        }

        if (snapshot.size() < minMessageCount) {
            return;
        }

        double emoteDensity = computeEmoteDensity(snapshot);
        if (emoteDensity < minEmoteDensity) {
            log.debug("Spike rate threshold met in {} but emote density too low: {} < {}", channel,
                    String.format("%.2f", emoteDensity), minEmoteDensity);
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

        log.info("Spike detected in {}! rate={} msg/s, baseline={}, normalized_rate={}, normalized_baseline={}, messages={}, emoteDensity={}, viewers={}, topEmotes={}",
                channel, String.format("%.1f", currentRate), String.format("%.1f", baselineRate),
                String.format("%.1f", effectiveCurrentRate), String.format("%.1f", effectiveBaseline),
                snapshot.size(), String.format("%.2f", emoteDensity), viewers, topEmotes);

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
