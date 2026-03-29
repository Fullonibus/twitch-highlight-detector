package com.fullonibus.api.repository;

import com.fullonibus.api.entity.HighlightEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface HighlightRepository extends JpaRepository<HighlightEntity, UUID>, JpaSpecificationExecutor<HighlightEntity> {

    Page<HighlightEntity> findByChannelOrderByScoreDesc(String channel, Pageable pageable);

    Page<HighlightEntity> findByChannelAndCreatedAtAfter(String channel, Instant after, Pageable pageable);

    Page<HighlightEntity> findByScoreGreaterThan(double minScore, Pageable pageable);
}
