package com.fullonibus.emote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EmoteDictionary {

    private static final String TWITCH_EMOTES_URL = "https://api.twitchemotes.com/api/v4/channels/0";
    private static final String HELIX_GLOBAL_EMOTES_URL = "https://api.twitch.tv/helix/chat/emotes/global";
    private static final String SEVENTV_GLOBAL_URL = "https://7tv.io/v3/emote-sets/global";
    private static final String FFZ_GLOBAL_URL = "https://api.frankerfacez.com/v1/set/global";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @Value("${twitch.client-id:}")
    private String twitchClientId;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> emoteIdToName = new ConcurrentHashMap<>();

    public EmoteDictionary() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void loadGlobalEmotes() {
        int before = emoteIdToName.size();
        loadTwitchemotes();
        loadHelixGlobalEmotes();
        load7TvEmotes();
        loadFfzEmotes();
        log.info("Loaded {} global emotes (was {} before)", emoteIdToName.size(), before);
    }

    private void loadTwitchemotes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TWITCH_EMOTES_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to load twitchemotes.com global emotes: HTTP {}", response.statusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("emotes")) {
                for (JsonNode emote : root.get("emotes")) {
                    String id = emote.has("id") ? emote.get("id").asText() : null;
                    String code = emote.has("code") ? emote.get("code").asText() : null;
                    if (id != null && code != null) {
                        emoteIdToName.put(id, code);
                    }
                }
            }
            log.info("Loaded {} emotes from twitchemotes.com", emoteIdToName.size());
        } catch (Exception e) {
            log.error("Failed to load twitchemotes.com global emotes", e);
        }
    }

    private void loadHelixGlobalEmotes() {
        if (twitchClientId == null || twitchClientId.isBlank()) {
            log.info("No twitch.client-id configured, skipping Helix global emotes");
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HELIX_GLOBAL_EMOTES_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Client-Id", twitchClientId)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to load Helix global emotes: HTTP {}", response.statusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("data")) {
                for (JsonNode emote : root.get("data")) {
                    String id = emote.has("id") ? emote.get("id").asText() : null;
                    String name = emote.has("name") ? emote.get("name").asText() : null;
                    if (id != null && name != null) {
                        emoteIdToName.put(id, name);
                    }
                }
            }
            log.info("Loaded emotes from Helix API, total now {}", emoteIdToName.size());
        } catch (Exception e) {
            log.error("Failed to load Helix global emotes", e);
        }
    }

    private void load7TvEmotes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEVENTV_GLOBAL_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to load 7TV global emotes: HTTP {}", response.statusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode emotes = root.path("emotes");
            for (JsonNode emote : emotes) {
                String id = emote.has("id") ? emote.get("id").asText() : null;
                String name = emote.has("name") ? emote.get("name").asText() : null;
                if (id != null && name != null) {
                    emoteIdToName.put(id, name);
                }
            }
            log.info("Loaded emotes from 7TV, total now {}", emoteIdToName.size());
        } catch (Exception e) {
            log.error("Failed to load 7TV global emotes", e);
        }
    }

    private void loadFfzEmotes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FFZ_GLOBAL_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to load FFZ global emotes: HTTP {}", response.statusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode sets = root.path("sets");
            for (JsonNode set : sets) {
                JsonNode emoticons = set.path("emoticons");
                for (JsonNode emote : emoticons) {
                    String id = emote.has("id") ? emote.get("id").asText() : null;
                    String name = emote.has("name") ? emote.get("name").asText() : null;
                    if (id != null && name != null) {
                        emoteIdToName.put(id, name);
                    }
                }
            }
            log.info("Loaded emotes from FFZ, total now {}", emoteIdToName.size());
        } catch (Exception e) {
            log.error("Failed to load FFZ global emotes", e);
        }
    }

    public boolean isKnownEmote(String emoteId) {
        return emoteIdToName.containsKey(emoteId);
    }

    public String resolveEmoteName(String emoteId) {
        return emoteIdToName.getOrDefault(emoteId, emoteId);
    }

    public Map<String, String> getEmoteMap() {
        return Collections.unmodifiableMap(emoteIdToName);
    }

    public int size() {
        return emoteIdToName.size();
    }
}
