package com.fullonibus.api.service;

import com.fullonibus.analyzer.detector.SpikeDetector;
import com.fullonibus.emote.EmoteDictionary;
import com.fullonibus.highlight.Highlight;
import com.fullonibus.highlight.HighlightScorer;
import com.fullonibus.notification.TelegramNotificationService;
import com.fullonibus.twitchirc.client.TwitchIrcClient;
import com.fullonibus.twitchirc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
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

    @Value("${telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${telegram.chat-id:}")
    private String telegramChatId;

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

    private final HighlightService highlightService;
    private final TelegramNotificationService notificationService;
    private final EmoteDictionary emoteDictionary;
    private final Map<String, TwitchIrcClient> activeClients = new ConcurrentHashMap<>();

    public IrcManager(HighlightService highlightService, TelegramNotificationService notificationService,
                      EmoteDictionary emoteDictionary) {
        this.highlightService = highlightService;
        this.notificationService = notificationService;
        this.emoteDictionary = emoteDictionary;
    }

    @PostConstruct
    public void init() {
        notificationService.configure(telegramBotToken, telegramChatId);
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
        HighlightScorer scorer = new HighlightScorer(
                scoringEmoteWeight, scoringRateWeight, scoringSubscriberWeight, emoteDictionary);

        detector.onSpike(messages -> {
            Highlight highlight = scorer.score(messages, channel);
            if (highlight != null) {
                highlightService.addHighlight(highlight);
                notificationService.sendHighlight(highlight);
            }
        });

        client.onMessage(detector::ingest);
        client.connect(channel);
        activeClients.put(channel, client);
        log.info("Connecting to channel: {}", channel);
    }

    public Set<String> getConnectedChannels() {
        return activeClients.keySet();
    }

    public void disconnect(String channel) {
        String channelName = channel.startsWith("#") ? channel : "#" + channel;
        TwitchIrcClient client = activeClients.remove(channelName);
        if (client != null) {
            client.disconnect();
            log.info("Disconnected from channel: {}", channelName);
        }
    }
}
