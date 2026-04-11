package com.fullonibus.twitchirc.client;

import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.parser.IrcMessageParser;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class TwitchIrcClient {

    private static final String TWITCH_IRC_URL = "wss://irc-ws.chat.twitch.tv:443";
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 60000;

    private final String token;
    private final String channel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "twitch-irc");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocketClient wsClient;
    private volatile boolean running;
    private volatile boolean intentionallyClosed;
    private Consumer<ChatMessage> messageHandler;
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "twitch-irc-reconnect");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    public TwitchIrcClient(String token) {
        this.token = token;
        this.channel = null;
    }

    public void onMessage(Consumer<ChatMessage> handler) {
        this.messageHandler = handler;
    }

    public void connect(String channel) {
        if (running) {
            log.warn("Already connected");
            return;
        }
        running = true;
        intentionallyClosed = false;
        String channelName = channel.startsWith("#") ? channel : "#" + channel;

        executor.submit(() -> doConnect(channelName));
    }

    private void doConnect(String channelName) {
        try {
            wsClient = new WebSocketClient(URI.create(TWITCH_IRC_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("Connected to Twitch IRC, joining {}", channelName);
                    send("PASS oauth:" + token);
                    send("NICK grombila");
                    send("CAP REQ :twitch.tv/tags twitch.tv/commands");
                    send("JOIN " + channelName);
                    reconnectAttempts.set(0);
                }

                @Override
                public void onMessage(String message) {
                    // Check for RECONNECT command
                    if (message.trim().equalsIgnoreCase("RECONNECT")) {
                        log.info("Received RECONNECT from Twitch IRC, scheduling reconnect...");
                        scheduleReconnect(channelName);
                        return;
                    }

                    if (IrcMessageParser.isPing(message)) {
                        send(IrcMessageParser.pongResponse());
                        return;
                    }
                    ChatMessage chatMessage = IrcMessageParser.parse(message);
                    log.debug("IRC raw ({}): {}", chatMessage != null ? chatMessage.getUsername() : "null", message.substring(0, Math.min(message.length(), 150)));
                    if (chatMessage != null && messageHandler != null) {
                        messageHandler.accept(chatMessage);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Disconnected from Twitch IRC: code={}, reason={}, remote={}", code, reason, remote);
                    running = false;
                    if (!intentionallyClosed) {
                        scheduleReconnect(channelName);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Twitch IRC error", ex);
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            log.error("Failed to connect to Twitch IRC", e);
            running = false;
            if (!intentionallyClosed) {
                scheduleReconnect(channelName);
            }
        }
    }

    private void scheduleReconnect(String channelName) {
        if (intentionallyClosed) return;
        int attempt = reconnectAttempts.incrementAndGet();
        long backoff = Math.min(INITIAL_BACKOFF_MS * (1L << Math.min(attempt - 1, 6)), MAX_BACKOFF_MS);
        log.info("Scheduling reconnect attempt #{} in {}ms", attempt, backoff);
        reconnectScheduler.schedule(() -> {
            if (intentionallyClosed) return;
            log.info("Attempting reconnect #{} to {}", attempt, channelName);
            running = true;
            doConnect(channelName);
        }, backoff, TimeUnit.MILLISECONDS);
    }

    public void disconnect() {
        intentionallyClosed = true;
        running = false;
        if (wsClient != null) {
            wsClient.close();
        }
        executor.shutdownNow();
        reconnectScheduler.shutdownNow();
    }

    public boolean isConnected() {
        return running && wsClient != null && wsClient.isOpen();
    }
}
