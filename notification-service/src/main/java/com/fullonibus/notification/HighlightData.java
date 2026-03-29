package com.fullonibus.notification;

import java.time.Instant;
import java.util.List;

public record HighlightData(
        String channel,
        Instant startTimestamp,
        double score,
        int messageCount,
        int emoteCount,
        double messageRate,
        List<String> topEmotes,
        List<String> topMessages
) {}
