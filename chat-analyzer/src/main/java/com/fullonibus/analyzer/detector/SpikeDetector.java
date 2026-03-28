package com.fullonibus.analyzer.detector;

import com.fullonibus.analyzer.buffer.MessageBuffer;
import com.fullonibus.twitchirc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SpikeDetector {

    private final MessageBuffer buffer;
    private final double thresholdMultiplier;
    private final Duration measurementWindow;
    private final Duration cooldown;
    private Instant lastSpikeTime = Instant.MIN;
    private double baselineRate = 5.0; // messages/sec, adaptive

    private Consumer<List<ChatMessage>> spikeHandler;

    public SpikeDetector(Duration windowSize, double thresholdMultiplier, Duration cooldown) {
        this.buffer = new MessageBuffer(windowSize);
        this.thresholdMultiplier = thresholdMultiplier;
        this.measurementWindow = windowSize;
        this.cooldown = cooldown;
    }

    public void onSpike(Consumer<List<ChatMessage>> handler) {
        this.spikeHandler = handler;
    }

    public void ingest(ChatMessage message) {
        buffer.add(message);
        checkForSpike();
    }

    private void checkForSpike() {
        Instant now = Instant.now();
        if (now.isBefore(lastSpikeTime.plus(cooldown))) {
            return;
        }

        double currentRate = buffer.messageRatePerSecond();
        if (currentRate == 0) return;

        // Exponential moving average for baseline
        baselineRate = baselineRate * 0.95 + currentRate * 0.05;

        if (currentRate > baselineRate * thresholdMultiplier) {
            List<ChatMessage> snapshot = buffer.snapshot();
            log.info("Spike detected! rate={:.1f} msg/s, baseline={:.1f}, messages in window={}",
                    currentRate, baselineRate, snapshot.size());

            lastSpikeTime = now;
            if (spikeHandler != null) {
                spikeHandler.accept(snapshot);
            }
        }
    }
}
