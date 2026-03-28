package com.fullonibus.api.service;

import com.fullonibus.analyzer.detector.SpikeDetector;
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

    private final HighlightService highlightService;
    private final TelegramNotificationService notificationService;
    private final Map<String, TwitchIrcClient> activeClients = new ConcurrentHashMap<>();

    public IrcManager(HighlightService highlightService, TelegramNotificationService notificationService) {
        this.highlightService = highlightService;
        this.notificationService = notificationService;
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

        SpikeDetector detector = new SpikeDetector(Duration.ofSeconds(30), 3.0, Duration.ofSeconds(30));
        HighlightScorer scorer = new HighlightScorer();

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
}
