package com.fullonibus.emote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EmoteDictionary {

    private static final String GLOBAL_EMOTES_URL = "https://api.twitchemotes.com/api/v4/channels/0";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Set<String> knownEmoteIds = new HashSet<>();

    public EmoteDictionary() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void loadGlobalEmotes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GLOBAL_EMOTES_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to load global emotes: HTTP {}", response.statusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("emotes")) {
                for (JsonNode emote : root.get("emotes")) {
                    if (emote.has("id")) {
                        knownEmoteIds.add(emote.get("id").asText());
                    }
                }
            }
            log.info("Loaded {} global emotes", knownEmoteIds.size());
        } catch (Exception e) {
            log.error("Failed to load global emotes", e);
        }
    }

    public boolean isKnownEmote(String emoteId) {
        return knownEmoteIds.contains(emoteId);
    }

    public Set<String> getKnownEmoteIds() {
        return Collections.unmodifiableSet(knownEmoteIds);
    }
}
