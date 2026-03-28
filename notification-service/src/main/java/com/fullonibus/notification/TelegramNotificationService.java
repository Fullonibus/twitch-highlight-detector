package com.fullonibus.notification;

import com.fullonibus.highlight.Highlight;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TelegramNotificationService {

    private String botToken;
    private String chatId;

    public TelegramNotificationService() {
    }

    public TelegramNotificationService(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public void configure(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public void sendHighlight(Highlight highlight) {
        if (botToken == null || chatId == null) {
            log.info("Telegram not configured, skipping notification for highlight: {}", highlight.getId());
            return;
        }
        // Stub: actual implementation will send via Telegram Bot API
        log.info("Would send Telegram notification for highlight {} on channel {} (score={})",
                highlight.getId(), highlight.getChannel(), highlight.getScore());
    }
}
