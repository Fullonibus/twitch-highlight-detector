package com.fullonibus.twitchirc.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ChatMessage {

    String username;
    String channel;
    String text;
    Instant timestamp;
    List<EmoteOccurrence> emotes;
    boolean isSubscriber;
    String userId;
    String displayName;
}
