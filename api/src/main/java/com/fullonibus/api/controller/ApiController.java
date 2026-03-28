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

    @GetMapping("/highlights")
    public ResponseEntity<List<Highlight>> getHighlights() {
        return ResponseEntity.ok(highlightService.getHighlights());
    }
}
