package com.fullonibus.api.controller;

import com.fullonibus.api.service.HighlightService;
import com.fullonibus.api.service.IrcManager;
import com.fullonibus.notification.TelegramNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IrcManager ircManager;

    @MockitoBean
    private HighlightService highlightService;

    @MockitoBean
    private TelegramNotificationService notificationService;

    @Test
    void testNotification_returnsSentWhenConfigured() throws Exception {
        when(notificationService.isConfigured()).thenReturn(true);
        mockMvc.perform(post("/api/notifications/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"));
        verify(notificationService).sendTestMessage();
    }

    @Test
    void testNotification_returnsNotConfigured() throws Exception {
        when(notificationService.isConfigured()).thenReturn(false);
        mockMvc.perform(post("/api/notifications/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_configured"));
    }

    @Test
    void notificationStatus_returnsConfigurationState() throws Exception {
        when(notificationService.isConfigured()).thenReturn(true);
        when(ircManager.isNotificationEnabled()).thenReturn(true);
        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
