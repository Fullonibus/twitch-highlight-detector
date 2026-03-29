package com.fullonibus.api.service;

import com.fullonibus.api.entity.HighlightEntity;
import com.fullonibus.api.entity.JsonListConverter;
import com.fullonibus.api.repository.HighlightRepository;
import com.fullonibus.highlight.Highlight;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HighlightService {

    private final HighlightRepository repository;

    @Transactional
    public void addHighlight(Highlight highlight) {
        HighlightEntity entity = toEntity(highlight);
        repository.save(entity);
    }

    public Page<Highlight> getHighlights(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByChannel(String channel, Pageable pageable) {
        return repository.findByChannelOrderByScoreDesc(channel, pageable).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByChannelAndFrom(String channel, Instant from, Pageable pageable) {
        return repository.findByChannelAndCreatedAtAfter(channel, from, pageable).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByMinScore(double minScore, Pageable pageable) {
        return repository.findByScoreGreaterThan(minScore, pageable).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByMinScoreAndFrom(double minScore, Instant from, Pageable pageable) {
        return repository.findAll(
                (root, query, cb) -> cb.and(
                        cb.greaterThan(root.get("score"), minScore),
                        cb.greaterThan(root.get("createdAt"), from)
                ),
                pageable
        ).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByChannelAndMinScore(String channel, double minScore, Pageable pageable) {
        return repository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("channel"), channel),
                        cb.greaterThan(root.get("score"), minScore)
                ),
                pageable
        ).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByChannelAndMinScoreAndFrom(String channel, double minScore, Instant from, Pageable pageable) {
        return repository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("channel"), channel),
                        cb.greaterThan(root.get("score"), minScore),
                        cb.greaterThan(root.get("createdAt"), from)
                ),
                pageable
        ).map(this::toDomain);
    }

    public Page<Highlight> getHighlightsByFrom(Instant from, Pageable pageable) {
        return repository.findAll(
                (root, query, cb) -> cb.greaterThan(root.get("createdAt"), from),
                pageable
        ).map(this::toDomain);
    }

    static HighlightEntity toEntity(Highlight highlight) {
        return HighlightEntity.builder()
                .id(highlight.getId() != null ? UUID.fromString(highlight.getId()) : null)
                .channel(highlight.getChannel())
                .startTimestamp(highlight.getStartTimestamp())
                .endTimestamp(highlight.getEndTimestamp())
                .score(highlight.getScore())
                .messageCount(highlight.getMessageCount())
                .emoteCount(highlight.getEmoteCount())
                .messageRate(highlight.getMessageRate())
                .topEmotes(JsonListConverter.toJson(highlight.getTopEmotes()))
                .topMessages(JsonListConverter.toJson(highlight.getTopMessages()))
                .build();
    }

    Highlight toDomain(HighlightEntity entity) {
        return Highlight.builder()
                .id(entity.getId().toString())
                .channel(entity.getChannel())
                .startTimestamp(entity.getStartTimestamp())
                .endTimestamp(entity.getEndTimestamp())
                .score(entity.getScore())
                .messageCount(entity.getMessageCount())
                .emoteCount(entity.getEmoteCount())
                .messageRate(entity.getMessageRate())
                .topEmotes(JsonListConverter.fromJson(entity.getTopEmotes()))
                .topMessages(JsonListConverter.fromJson(entity.getTopMessages()))
                .build();
    }
}
