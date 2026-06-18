package com.fullonibus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullonibus.api.entity.HighlightEntity;
import com.fullonibus.api.entity.JsonListConverter;
import com.fullonibus.api.repository.HighlightRepository;
import com.fullonibus.notification.TelegramNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.fullonibus.api.service.HighlightService highlightService;

    @MockitoBean
    private com.fullonibus.api.service.IrcManager ircManager;

    @MockitoBean
    private TelegramNotificationService notificationService;

    @Test
    void health_returnsUp() throws Exception {
        when(ircManager.getConnectedChannels()).thenReturn(java.util.Set.of("#xqc"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.connectedChannels[0]").value("#xqc"));
    }

    @Test
    void connectChannel_returnsOk() throws Exception {
        mockMvc.perform(post("/api/channels/xqc/connect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("xqc"))
                .andExpect(jsonPath("$.status").value("connecting"));
    }

    @Test
    void disconnectChannel_returnsOk() throws Exception {
        mockMvc.perform(post("/api/channels/xqc/disconnect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("xqc"))
                .andExpect(jsonPath("$.status").value("disconnected"));
    }

    @Test
    void getHighlights_returnsPagedResults() throws Exception {
        com.fullonibus.highlight.Highlight h = com.fullonibus.highlight.Highlight.builder()
                .id("test-id")
                .channel("#xqc")
                .startTimestamp(Instant.now())
                .endTimestamp(Instant.now().plusSeconds(30))
                .score(20.0)
                .messageCount(100)
                .emoteCount(50)
                .messageRate(3.33)
                .topEmotes(List.of("Kappa"))
                .topMessages(List.of("GG"))
                .build();

        Page<com.fullonibus.highlight.Highlight> page = new PageImpl<>(List.of(h));
        when(highlightService.getHighlights(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/highlights?page=0&size=20&sort=score,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].channel").value("#xqc"))
                .andExpect(jsonPath("$.content[0].score").value(20.0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getHighlights_withChannelFilter() throws Exception {
        Page<com.fullonibus.highlight.Highlight> page = new PageImpl<>(List.of());
        when(highlightService.getHighlightsByChannel(eq("#xqc"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/highlights?channel=xqc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getHighlights_withMinScoreFilter() throws Exception {
        Page<com.fullonibus.highlight.Highlight> page = new PageImpl<>(List.of());
        when(highlightService.getHighlightsByMinScore(eq(10.0), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/highlights?minScore=10.0"))
                .andExpect(status().isOk());
    }

    @Test
    void getHighlights_withDateFilter() throws Exception {
        Page<com.fullonibus.highlight.Highlight> page = new PageImpl<>(List.of());
        when(highlightService.getHighlightsByFrom(any(Instant.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/highlights?from=2026-03-28T00:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test
    void getHighlights_withAllFilters() throws Exception {
        Page<com.fullonibus.highlight.Highlight> page = new PageImpl<>(List.of());
        when(highlightService.getHighlightsByChannelAndMinScoreAndFrom(eq("#xqc"), eq(10.0), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/highlights?channel=xqc&minScore=10.0&from=2026-03-28T00:00:00Z"))
                .andExpect(status().isOk());
    }
}
