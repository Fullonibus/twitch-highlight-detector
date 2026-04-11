package com.fullonibus.analyzer.detector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class ViewerCountTracker {

    private static final String TWITCH_STREAMS_URL = "https://api.twitch.tv/helix/streams?user_login=";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String clientId;
    private String accessToken;
    private final String refreshToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Integer> viewerCounts = new ConcurrentHashMap<>();
    private final Set<String> trackedChannels = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService scheduler;
    private long pollIntervalSeconds;

    public ViewerCountTracker(String clientId, String accessToken, String refreshToken, long pollIntervalSeconds) {
        this.clientId = clientId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            log.warn("ViewerCountTracker already running");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "viewer-count-tracker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::pollAllChannels, pollIntervalSeconds, pollIntervalSeconds, TimeUnit.SECONDS);
        log.info("ViewerCountTracker started, polling every {}s", pollIntervalSeconds);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            log.info("ViewerCountTracker stopped");
        }
    }

    public void trackChannel(String channel) {
        trackedChannels.add(channel);
    }

    public void untrackChannel(String channel) {
        trackedChannels.remove(channel);
        viewerCounts.remove(channel);
    }

    public int getViewerCount(String channel) {
        return viewerCounts.getOrDefault(channel, 0);
    }

    private void pollAllChannels() {
        for (String channel : trackedChannels) {
            try {
                pollChannel(channel);
            } catch (Exception e) {
                log.debug("Failed to poll viewer count for {}: {}", channel, e.getMessage());
            }
        }
    }

    private void pollChannel(String channel) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TWITCH_STREAMS_URL + channel))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                log.warn("Twitch API returned 401 for viewer count, token may be expired");
                tryRefreshToken();
                return;
            }

            if (response.statusCode() != 200) {
                log.debug("Twitch API returned {} for channel {}", response.statusCode(), channel);
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            int oldCount = viewerCounts.getOrDefault(channel, 0);

            if (data.isArray() && !data.isEmpty()) {
                int viewerCount = data.get(0).path("viewer_count").asInt(0);
                viewerCounts.put(channel, viewerCount);
                if (Math.abs(viewerCount - oldCount) > oldCount * 0.2 + 10) {
                    log.info("Viewer count for {}: {} -> {}", channel, oldCount, viewerCount);
                }
            } else {
                // Stream is offline
                viewerCounts.remove(channel);
                if (oldCount > 0) {
                    log.info("Stream {} went offline (was {} viewers)", channel, oldCount);
                }
            }
        } catch (Exception e) {
            log.debug("Error polling viewer count for {}: {}", channel, e.getMessage());
        }
    }

    private void tryRefreshToken() {
        if (refreshToken == null || refreshToken.isEmpty()) return;
        try {
            String body = "grant_type=refresh_token&refresh_token=" + refreshToken +
                    "&client_id=" + clientId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://id.twitch.tv/oauth2/token"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                this.accessToken = root.path("access_token").asText();
                log.info("Successfully refreshed Twitch API token");
            } else {
                log.warn("Failed to refresh Twitch token: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Error refreshing Twitch token: {}", e.getMessage());
        }
    }
}
