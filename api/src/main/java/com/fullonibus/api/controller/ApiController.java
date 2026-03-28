package com.fullonibus.api.controller;

import com.fullonibus.api.service.HighlightService;
import com.fullonibus.api.service.IrcManager;
import com.fullonibus.highlight.Highlight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<Highlight>> getHighlights(@RequestParam(required = false) String channel) {
        List<Highlight> all = highlightService.getHighlights();
        if (channel != null && !channel.isBlank()) {
            String ch = channel.startsWith("#") ? channel : "#" + channel;
            all = all.stream().filter(h -> ch.equals(h.getChannel())).collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(all);
    }
}
