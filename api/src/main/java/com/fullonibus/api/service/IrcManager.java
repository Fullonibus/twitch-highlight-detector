package com.fullonibus.api.service;

import com.fullonibus.analyzer.detector.SpikeDetector;
import com.fullonibus.analyzer.detector.ViewerCountTracker;
import com.fullonibus.emote.EmoteDictionary;
import com.fullonibus.highlight.Highlight;
import com.fullonibus.highlight.HighlightScorer;
import com.fullonibus.notification.HighlightData;
import com.fullonibus.notification.TelegramNotificationService;
import com.fullonibus.twitchirc.client.TwitchIrcClient;
import com.fullonibus.twitchirc.model.ChatMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class IrcManager {

    @Value("${twitch.irc.token:}")
    private String twitchToken;

    @Value("${detector.window-seconds:30}")
    private int detectorWindowSeconds;

    @Value("${detector.threshold-multiplier:3.0}")
    private double detectorThresholdMultiplier;

    @Value("${detector.cooldown-seconds:30}")
    private int detectorCooldownSeconds;

    @Value("${detector.min-message-count:5}")
    private int detectorMinMessageCount;

    @Value("${detector.min-emote-density:0.1}")
    private double detectorMinEmoteDensity;

    @Value("${scoring.emote-weight:2.0}")
    private double scoringEmoteWeight;

    @Value("${scoring.rate-weight:1.5}")
    private double scoringRateWeight;

    @Value("${scoring.subscriber-weight:1.2}")
    private double scoringSubscriberWeight;

    @Value("${telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${telegram.chat-id:}")
    private String telegramChatId;

    @Value("${notification.enabled:true}")
    private boolean notificationEnabled;

    @Value("${highlight.viewers.enabled:true}")
    private boolean viewerTrackingEnabled;

    @Value("${highlight.viewers.poll-interval-seconds:60}")
    private long viewerPollIntervalSeconds;

    @Value("${twitch.api.client-id:}")
    private String twitchApiClientId;

    @Value("${twitch.api.access-token:}")
    private String twitchApiAccessToken;

    @Value("${twitch.api.refresh-token:}")
    private String twitchApiRefreshToken;

    private final HighlightService highlightService;
    private final EmoteDictionary emoteDictionary;
    private final TelegramNotificationService notificationService;
    private final Map<String, TwitchIrcClient> activeClients = new ConcurrentHashMap<>();
    private volatile ViewerCountTracker viewerCountTracker;

    public IrcManager(HighlightService highlightService, EmoteDictionary emoteDictionary,
                      TelegramNotificationService notificationService) {
        this.highlightService = highlightService;
        this.emoteDictionary = emoteDictionary;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void init() {
        notificationService.configure(telegramBotToken, telegramChatId);

        if (viewerTrackingEnabled && twitchApiClientId != null && !twitchApiClientId.isEmpty()) {
            viewerCountTracker = new ViewerCountTracker(
                    twitchApiClientId, twitchApiAccessToken, twitchApiRefreshToken, viewerPollIntervalSeconds);
            viewerCountTracker.start();
            log.info("Viewer count tracking enabled");
        } else {
            log.info("Viewer count tracking disabled");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (viewerCountTracker != null) {
            viewerCountTracker.stop();
        }
        for (TwitchIrcClient client : activeClients.values()) {
            client.disconnect();
        }
        activeClients.clear();
    }

    public void connect(String channel) {
        if (activeClients.containsKey(channel)) {
            log.info("Already connected to {}", channel);
            return;
        }

        TwitchIrcClient client = new TwitchIrcClient(twitchToken);

        SpikeDetector detector = new SpikeDetector(
                Duration.ofSeconds(detectorWindowSeconds),
                detectorThresholdMultiplier,
                Duration.ofSeconds(detectorCooldownSeconds),
                detectorMinMessageCount,
                detectorMinEmoteDensity
        );

        // Wire viewer count tracker if available
        if (viewerCountTracker != null) {
            detector.setViewerCountTracker(viewerCountTracker);
            String cleanChannel = channel.startsWith("#") ? channel.substring(1) : channel;
            viewerCountTracker.trackChannel(cleanChannel);
        }

        HighlightScorer scorer = new HighlightScorer(
                scoringEmoteWeight, scoringRateWeight, scoringSubscriberWeight, emoteDictionary);

        detector.onSpike(messages -> {
            Highlight highlight = scorer.score(messages, channel);
            if (highlight != null) {
                highlightService.addHighlight(highlight);
                if (notificationEnabled) {
                    try {
                        notificationService.sendHighlight(toHighlightData(highlight));
                    } catch (Exception e) {
                        log.error("Failed to send notification for highlight: {}", e.getMessage());
                    }
                }
            }
        });

        client.onMessage(detector::ingest);
        client.connect(channel);
        activeClients.put(channel, client);
        log.info("Connecting to channel: {}", channel);
    }

    private HighlightData toHighlightData(Highlight h) {
        return new HighlightData(
                h.getChannel(),
                h.getStartTimestamp(),
                h.getScore(),
                h.getMessageCount(),
                h.getEmoteCount(),
                h.getMessageRate(),
                h.getTopEmotes(),
                h.getTopMessages()
        );
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public Set<String> getConnectedChannels() {
        return activeClients.keySet();
    }

    public void disconnect(String channel) {
        String channelName = channel.startsWith("#") ? channel : "#" + channel;
        TwitchIrcClient client = activeClients.remove(channelName);
        if (client != null) {
            client.disconnect();
            if (viewerCountTracker != null) {
                String cleanChannel = channelName.substring(1);
                viewerCountTracker.untrackChannel(cleanChannel);
            }
            log.info("Disconnected from channel: {}", channelName);
        }
    }
}
