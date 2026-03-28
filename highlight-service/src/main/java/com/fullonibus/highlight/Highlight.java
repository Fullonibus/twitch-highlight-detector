package com.fullonibus.highlight;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class Highlight {

    String id;
    String channel;
    Instant startTimestamp;
    Instant endTimestamp;
    double score;
    int messageCount;
    int emoteCount;
    double messageRate;
    List<String> topEmotes;
    List<String> topMessages;
}
