package com.fullonibus.highlight;

import com.fullonibus.emote.EmoteDictionary;
import com.fullonibus.twitchirc.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Slf4j
public class HighlightScorer {

    private final double emoteWeight;
    private final double rateWeight;
    private final double subscriberWeight;
    private final int maxTopEmotes;
    private final EmoteDictionary emoteDictionary;

    private static final int DEFAULT_MAX_TOP_EMOTES = 5;

    public HighlightScorer(double emoteWeight, double rateWeight, double subscriberWeight, EmoteDictionary emoteDictionary) {
        this.emoteWeight = emoteWeight;
        this.rateWeight = rateWeight;
        this.subscriberWeight = subscriberWeight;
        this.maxTopEmotes = DEFAULT_MAX_TOP_EMOTES;
        this.emoteDictionary = emoteDictionary;
    }

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

        Map<String, String> emoteMap = emoteDictionary != null ? emoteDictionary.getEmoteMap() : Map.of();

        for (ChatMessage msg : messages) {
            emoteCount += msg.getEmotes().size();

            // Also detect 7TV/FFZ emotes from text
            int textEmotes = countTextEmotes(msg.getText(), emoteMap);
            if (textEmotes > 0) {
                emoteCount += textEmotes;
                for (String name : emoteMap.values()) {
                    if (msg.getText().toLowerCase().contains(name.toLowerCase())) {
                        emoteFrequency.merge(name, 1, Integer::sum);
                    }
                }
            }

            if (msg.isSubscriber()) subCount++;

            for (var emote : msg.getEmotes()) {
                emoteFrequency.merge(emote.getEmoteId(), 1, Integer::sum);
            }
        }

        double emoteDensity = messageCount > 0 ? (double) emoteCount / messageCount : 0;
        double subRatio = messageCount > 0 ? (double) subCount / messageCount : 0;

        double score = (messageRate * rateWeight)
                + (emoteDensity * emoteWeight * 10)
                + (subRatio * subscriberWeight * 5);

        List<String> topEmotes = emoteFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxTopEmotes)
                .map(e -> resolveEmoteName(e.getKey()) + "(" + e.getValue() + ")")
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

    private String resolveEmoteName(String emoteId) {
        if (emoteDictionary != null) {
            return emoteDictionary.resolveEmoteName(emoteId);
        }
        return emoteId;
    }

    private int countTextEmotes(String text, Map<String, String> emoteMap) {
        if (text == null || text.isEmpty() || emoteMap.isEmpty()) return 0;
        int count = 0;
        for (String name : emoteMap.values()) {
            // Match whole word emotes (case-insensitive)
            String pattern = "(?<!\\w)" + Pattern.quote(name) + "(?!\\w)";
            if (text.toLowerCase().contains(name.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
}