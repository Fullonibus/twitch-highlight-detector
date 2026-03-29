package com.fullonibus.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramNotificationService {

    private static final String TELEGRAM_API = "https://api.telegram.org/bot%s/sendMessage";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private String botToken;
    private String chatId;
    private final HttpClient httpClient;

    public TelegramNotificationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void configure(String botToken, String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
        if (isConfigured()) {
            log.info("Telegram notifications configured for chat: {}", chatId);
        }
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
    }

    public void sendHighlight(HighlightData highlight) {
        if (!isConfigured()) {
            log.warn("Telegram not configured, skipping highlight notification");
            return;
        }

        String time = TIME_FORMATTER.format(highlight.startTimestamp());
        String topEmotes = String.join(" ", highlight.topEmotes());
        String topMessages = highlight.topMessages().stream()
                .map(m -> "• " + m)
                .collect(Collectors.joining("\n"));

        String text = String.format(
                "🔥 <b>Highlight Detected!</b>\n\n" +
                "📺 Channel: <code>%s</code>\n" +
                "⏰ Time: %s\n" +
                "📊 Score: %.1f\n" +
                "💬 Messages: %d (%.1f msg/s)\n" +
                "😀 Emotes: %d\n\n" +
                "Top Emotes: %s\n" +
                "💬 Top Messages:\n%s",
                escapeHtml(highlight.channel()),
                time,
                highlight.score(),
                highlight.messageCount(),
                highlight.messageRate(),
                highlight.emoteCount(),
                escapeHtml(topEmotes),
                escapeHtml(topMessages)
        );

        sendTelegramMessage(text);
    }

    public void sendTestMessage() {
        if (!isConfigured()) {
            log.warn("Telegram not configured, skipping test message");
            return;
        }
        sendTelegramMessage("✅ Twitch Highlight Detector is running!");
    }

    private void sendTelegramMessage(String text) {
        try {
            String url = TELEGRAM_API.formatted(botToken);

            String escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            String jsonBody = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + escapedText + "\",\"parse_mode\":\"HTML\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Telegram API returned status {}: {}", response.statusCode(), response.body());
            } else {
                log.debug("Telegram message sent successfully");
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram notification: {}", e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
