package com.fullonibus.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TelegramNotificationServiceTest {

    private TelegramNotificationService service;

    @BeforeEach
    void setUp() {
        service = new TelegramNotificationService();
    }

    @Test
    void sendHighlight_doesNotThrowWithoutCredentials() {
        HighlightData data = new HighlightData(
                "testchannel", Instant.now(), 5.0, 10, 3, 1.5,
                List.of("Kappa", "PogChamp"), List.of("LETS GOOO", "OMG")
        );
        assertDoesNotThrow(() -> service.sendHighlight(data));
    }

    @Test
    void sendHighlight_doesNotThrowWithEmptyCredentials() {
        service.configure("", "");
        HighlightData data = new HighlightData(
                "testchannel", Instant.now(), 5.0, 10, 3, 1.5,
                List.of("Kappa"), List.of("msg")
        );
        assertDoesNotThrow(() -> service.sendHighlight(data));
    }

    @Test
    void isConfigured_returnsFalseWithoutConfiguration() {
        assertFalse(service.isConfigured());
    }

    @Test
    void isConfigured_returnsTrueWithValidConfiguration() {
        service.configure("123456:ABC-DEF", "-100123456789");
        assertTrue(service.isConfigured());
    }

    @Test
    void isConfigured_returnsFalseWithEmptyToken() {
        service.configure("", "-100123456789");
        assertFalse(service.isConfigured());
    }

    @Test
    void sendTestMessage_doesNotThrowWithoutCredentials() {
        assertDoesNotThrow(() -> service.sendTestMessage());
    }
}
