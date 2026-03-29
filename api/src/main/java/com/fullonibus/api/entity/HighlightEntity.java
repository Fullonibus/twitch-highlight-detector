package com.fullonibus.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "highlights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HighlightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "start_timestamp", nullable = false)
    private Instant startTimestamp;

    @Column(name = "end_timestamp", nullable = false)
    private Instant endTimestamp;

    @Column(nullable = false)
    private double score;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "emote_count", nullable = false)
    private int emoteCount;

    @Column(name = "message_rate", nullable = false)
    private double messageRate;

    @Column(name = "top_emotes", columnDefinition = "TEXT")
    private String topEmotes;

    @Column(name = "top_messages", columnDefinition = "TEXT")
    private String topMessages;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
