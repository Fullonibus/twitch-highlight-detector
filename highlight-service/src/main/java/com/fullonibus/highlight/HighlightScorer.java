package com.fullonibus.highlight;

import com.fullonibus.twitchirc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class HighlightScorer {

    private static final double EMOTE_WEIGHT = 2.0;
    private static final double RATE_WEIGHT = 1.5;
    private static final double SUBSCRIBER_WEIGHT = 1.2;
    private static final int MAX_TOP_EMOTES = 5;

    public Highlight score(List<ChatMessage> messages, String channel) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        int messageCount = messages.size();
        int emoteCount = 0;
        int subCount = 0;
        Map<String, Integer> emoteFrequency = new HashMap<>();

        Instant start = messages.get(0).getTimestamp();
        Instant end = messages.get(messages.size() - 1).getTimestamp();
        double durationSec = Math.max(Duration.between(start, end).toMillis() / 1000.0, 0.1);
        double messageRate = messageCount / durationSec;

        for (ChatMessage msg : messages) {
            emoteCount += msg.getEmotes().size();
            if (msg.isSubscriber()) subCount++;

            for (var emote : msg.getEmotes()) {
                emoteFrequency.merge(emote.getEmoteId(), 1, Integer::sum);
            }
        }

        double emoteDensity = messageCount > 0 ? (double) emoteCount / messageCount : 0;
        double subRatio = messageCount > 0 ? (double) subCount / messageCount : 0;

        double score = (messageRate * RATE_WEIGHT)
                + (emoteDensity * EMOTE_WEIGHT * 10)
                + (subRatio * SUBSCRIBER_WEIGHT * 5);

        List<String> topEmotes = emoteFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_TOP_EMOTES)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> topMessages = messages.stream()
                .sorted(Comparator.comparingInt(m -> -m.getEmotes().size()))
                .limit(3)
                .map(m -> m.getDisplayName() + ": " + m.getText())
                .collect(Collectors.toList());

        return Highlight.builder()
                .id(UUID.randomUUID().toString())
                .channel(channel)
                .startTimestamp(start)
                .endTimestamp(end)
                .score(Math.round(score * 100.0) / 100.0)
                .messageCount(messageCount)
                .emoteCount(emoteCount)
                .messageRate(Math.round(messageRate * 100.0) / 100.0)
                .topEmotes(topEmotes)
                .topMessages(topMessages)
                .build();
    }
}
