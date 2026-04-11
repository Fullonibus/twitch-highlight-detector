package com.fullonibus.api.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AppMetrics {

    private final Counter messagesIngested;
    private final Counter spikesDetected;
    private final Timer spikeDetectionTime;
    private final MeterRegistry registry;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.messagesIngested = Counter.builder("twitch.messages.ingested")
                .description("Total number of chat messages ingested")
                .register(registry);
        this.spikesDetected = Counter.builder("twitch.spikes.detected")
                .description("Total number of chat spikes detected")
                .register(registry);
        this.spikeDetectionTime = Timer.builder("twitch.spike.detection.duration")
                .description("Time spent on spike detection processing")
                .register(registry);
    }

    public void recordMessageIngested() {
        messagesIngested.increment();
    }

    public void recordSpikeDetected(String channel) {
        spikesDetected.increment();
        Counter.builder("twitch.spikes.detected.channel")
                .tag("channel", channel)
                .register(registry).increment();
    }

    public Timer.Sample startDetectionTimer() {
        return Timer.start(registry);
    }

    public void stopDetectionTimer(Timer.Sample sample) {
        sample.stop(spikeDetectionTime);
    }

    public void registerIrcConnectionsGauge(IrcConnectionProvider provider) {
        Gauge.builder("twitch.irc.active.connections", provider::getActiveConnectionCount)
                .description("Number of active IRC connections")
                .register(registry);
    }

    public void registerViewerCountGauge(ViewerCountProvider provider, String channel) {
        Gauge.builder("twitch.channel.viewers", () -> provider.getViewerCount(channel))
                .tag("channel", channel)
                .description("Viewer count per channel")
                .register(registry);
    }

    public void removeViewerCountGauge(String channel) {
        registry.find("twitch.channel.viewers").tag("channel", channel).meters()
                .forEach(registry::remove);
    }

    public interface IrcConnectionProvider {
        int getActiveConnectionCount();
    }

    public interface ViewerCountProvider {
        int getViewerCount(String channel);
    }
}
