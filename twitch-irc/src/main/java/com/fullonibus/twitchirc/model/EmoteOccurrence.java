package com.fullonibus.twitchirc.model;

import lombok.Value;

@Value
public class EmoteOccurrence {

    String emoteId;
    int startIndex;
    int endIndex;
}
