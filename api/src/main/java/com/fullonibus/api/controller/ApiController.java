package com.fullonibus.api.controller;

import com.fullonibus.api.service.HighlightService;
import com.fullonibus.api.service.IrcManager;
import com.fullonibus.highlight.Highlight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final IrcManager ircManager;
    private final HighlightService highlightService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "connectedChannels", ircManager.getConnectedChannels()
        ));
    }

    @PostMapping("/channels/{channel}/connect")
    public ResponseEntity<Map<String, String>> connectChannel(@PathVariable String channel) {
        ircManager.connect(channel);
        return ResponseEntity.ok(Map.of("channel", channel, "status", "connecting"));
    }

    @PostMapping("/channels/{channel}/disconnect")
    public ResponseEntity<Map<String, String>> disconnectChannel(@PathVariable String channel) {
        ircManager.disconnect(channel);
        return ResponseEntity.ok(Map.of("channel", channel, "status", "disconnected"));
    }

    @GetMapping("/highlights")
    public ResponseEntity<Page<Highlight>> getHighlights(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Instant from,
            @PageableDefault(size = 20, sort = "score", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        String normalizedChannel = null;
        if (channel != null && !channel.isBlank()) {
            normalizedChannel = channel.startsWith("#") ? channel : "#" + channel;
        }

        Page<Highlight> result;
        if (normalizedChannel != null && minScore != null && from != null) {
            result = highlightService.getHighlightsByChannelAndMinScoreAndFrom(normalizedChannel, minScore, from, pageable);
        } else if (normalizedChannel != null && minScore != null) {
            result = highlightService.getHighlightsByChannelAndMinScore(normalizedChannel, minScore, pageable);
        } else if (normalizedChannel != null && from != null) {
            result = highlightService.getHighlightsByChannelAndFrom(normalizedChannel, from, pageable);
        } else if (normalizedChannel != null) {
            result = highlightService.getHighlightsByChannel(normalizedChannel, pageable);
        } else if (minScore != null && from != null) {
            result = highlightService.getHighlightsByMinScoreAndFrom(minScore, from, pageable);
        } else if (minScore != null) {
            result = highlightService.getHighlightsByMinScore(minScore, pageable);
        } else if (from != null) {
            result = highlightService.getHighlightsByFrom(from, pageable);
        } else {
            result = highlightService.getHighlights(pageable);
        }

        return ResponseEntity.ok(result);
    }
}
