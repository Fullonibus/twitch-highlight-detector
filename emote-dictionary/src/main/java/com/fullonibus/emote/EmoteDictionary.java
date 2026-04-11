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

    private static final String SEVENTV_GLOBAL_URL = "https://7tv.io/v3/emote-sets/global";
    private static final String FFZ_GLOBAL_URL = "https://api.frankerfacez.com/v1/set/global";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @Value("${emote.7tv.enabled:true}")
    private boolean seventvEnabled;

    @Value("${emote.ffz.enabled:true}")
    private boolean ffzEnabled;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> emoteIdToName = new ConcurrentHashMap<>();

    // Common text emotes not in 7TV/FFZ global sets
    private static final Map<String, String> STATIC_EMOTES;
    static {
        Map<String, String> m = new HashMap<>();
        String[] emotes = {"xdd","xd",":3","kekw","lul","pog","omegalul","monkaS","pepega","sadge","kappa","pogchamp","ez",":)","<3",";)",":D",":P","rip","gg","ezclap","catjam","pepehands","cope"};
        for (String e : emotes) m.put(e, e);
        STATIC_EMOTES = Collections.unmodifiableMap(m);
    }


    public EmoteDictionary() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void loadGlobalEmotes() {
        int before = emoteIdToName.size();
        if (seventvEnabled) load7TvEmotes();
        if (ffzEnabled) loadFfzEmotes();
        // Add static text emotes
        for (var entry : STATIC_EMOTES.entrySet()) {
            emoteIdToName.putIfAbsent("static:" + entry.getKey(), entry.getValue());
        }
        log.info("Loaded {} global emotes total (incl. {} static)", emoteIdToName.size(), STATIC_EMOTES.size());
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
