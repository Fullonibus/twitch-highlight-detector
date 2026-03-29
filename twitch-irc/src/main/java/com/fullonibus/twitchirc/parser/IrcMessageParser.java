package com.fullonibus.twitchirc.parser;

import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.model.EmoteOccurrence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IrcMessageParser {

    private static final String PRIVMSG = "PRIVMSG";
    private static final String TAGS_PREFIX = "@";

    private IrcMessageParser() {
    }

    public static ChatMessage parse(String rawMessage) {
        String tagsPart = "";
        String rest = rawMessage;

        if (rawMessage.startsWith(TAGS_PREFIX)) {
            int spaceIdx = rawMessage.indexOf(' ');
            tagsPart = rawMessage.substring(1, spaceIdx);
            rest = rawMessage.substring(spaceIdx + 1);
        }

        // Skip prefix (server/nick)
        if (rest.startsWith(":")) {
            int spaceIdx = rest.indexOf(' ');
            if (spaceIdx > 0) {
                rest = rest.substring(spaceIdx + 1);
            }
        }

        if (!rest.startsWith(PRIVMSG)) {
            return null;
        }

        rest = rest.substring(PRIVMSG.length()).trim();

        int spaceIdx = rest.indexOf(' ');
        if (spaceIdx < 0) {
            return null;
        }

        String channel = rest.substring(0, spaceIdx).startsWith("#")
                ? rest.substring(0, spaceIdx)
                : "#" + rest.substring(0, spaceIdx);

        String text = rest.substring(spaceIdx + 1);
        if (text.startsWith(":")) {
            text = text.substring(1);
        }

        Map<String, String> tags = parseTags(tagsPart);
        List<EmoteOccurrence> emotes = parseEmotes(tags.get("emotes"));

        return ChatMessage.builder()
                .username(tags.getOrDefault("login", extractNick(rawMessage)))
                .channel(channel)
                .text(text)
                .timestamp(parseTimestamp(tags.get("tmi-sent-ts")))
                .emotes(emotes)
                .isSubscriber("1".equals(tags.get("subscriber")))
                .userId(tags.get("user-id"))
                .displayName(tags.get("display-name"))
                .build();
    }

    private static Map<String, String> parseTags(String tagsPart) {
        Map<String, String> tags = new java.util.HashMap<>();
        if (tagsPart == null || tagsPart.isEmpty()) {
            return tags;
        }
        for (String tag : tagsPart.split(";")) {
            int eq = tag.indexOf('=');
            if (eq > 0) {
                String key = tag.substring(0, eq);
                String value = tag.substring(eq + 1).replace("\\s", " ").replace("\\:", ";");
                tags.put(key, value);
            }
        }
        return tags;
    }

    private static List<EmoteOccurrence> parseEmotes(String emotesTag) {
        List<EmoteOccurrence> result = new ArrayList<>();
        if (emotesTag == null || emotesTag.isEmpty()) {
            return result;
        }
        for (String entry : emotesTag.split("/")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;
            String emoteId = parts[0];
            for (String range : parts[1].split(",")) {
                String[] indices = range.split("-");
                if (indices.length == 2) {
                    try {
                        result.add(new EmoteOccurrence(
                                emoteId,
                                Integer.parseInt(indices[0]),
                                Integer.parseInt(indices[1])
                        ));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return result;
    }

    private static Instant parseTimestamp(String ts) {
        if (ts != null && !ts.isEmpty()) {
            try {
                return Instant.ofEpochMilli(Long.parseLong(ts));
            } catch (NumberFormatException ignored) {
            }
        }
        return Instant.now();
    }

    private static String extractNick(String rawMessage) {
        // :nick!user@host
        int excl = rawMessage.indexOf('!');
        int colon = rawMessage.indexOf(':');
        if (colon >= 0 && excl > colon) {
            return rawMessage.substring(colon + 1, excl);
        }
        return "unknown";
    }

    public static boolean isPing(String rawMessage) {
        String trimmed = rawMessage.trim();
        return trimmed.equals("PING") || trimmed.startsWith("PING ");
    }

    public static String pongResponse() {
        return "PONG :tmi.twitch.tv";
    }
}
