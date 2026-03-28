package com.fullonibus.twitchirc.client;

import com.fullonibus.twitchirc.model.ChatMessage;
import com.fullonibus.twitchirc.parser.IrcMessageParser;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshakes.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class TwitchIrcClient {

    private static final String TWITCH_IRC_URL = "wss://irc-ws.chat.twitch.tv:443";

    private final String token;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "twitch-irc");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocketClient wsClient;
    private volatile boolean running;
    private Consumer<ChatMessage> messageHandler;

    public TwitchIrcClient(String token) {
        this.token = token;
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
        String channelName = channel.startsWith("#") ? channel : "#" + channel;

        executor.submit(() -> {
            try {
                wsClient = new WebSocketClient(URI.create(TWITCH_IRC_URL)) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        log.info("Connected to Twitch IRC");
                        send("PASS oauth:" + token);
                        send("NICK justinfan" + System.currentTimeMillis() % 100000);
                        send("CAP REQ :twitch.tv/tags twitch.tv/commands");
                        send("JOIN " + channelName);
                    }

                    @Override
                    public void onMessage(String message) {
                        if (IrcMessageParser.isPing(message)) {
                            send(IrcMessageParser.pongResponse());
                            return;
                        }
                        ChatMessage chatMessage = IrcMessageParser.parse(message);
                        if (chatMessage != null && messageHandler != null) {
                            messageHandler.accept(chatMessage);
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        log.info("Disconnected from Twitch IRC: code={}, reason={}, remote={}", code, reason, remote);
                        running = false;
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
            }
        });
    }

    public void disconnect() {
        running = false;
        if (wsClient != null) {
            wsClient.close();
        }
        executor.shutdownNow();
    }

    public boolean isConnected() {
        return running && wsClient != null && wsClient.isOpen();
    }
}
