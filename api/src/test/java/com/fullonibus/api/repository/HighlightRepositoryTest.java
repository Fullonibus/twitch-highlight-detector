package com.fullonibus.api.repository;

import com.fullonibus.api.entity.HighlightEntity;
import com.fullonibus.api.entity.JsonListConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class HighlightRepositoryTest {

    @Autowired
    private HighlightRepository repository;

    private HighlightEntity createEntity(String channel, double score, Instant createdAt) {
        return HighlightEntity.builder()
                .channel(channel)
                .startTimestamp(createdAt)
                .endTimestamp(createdAt.plusSeconds(30))
                .score(score)
                .messageCount(100)
                .emoteCount(50)
                .messageRate(3.33)
                .topEmotes(JsonListConverter.toJson(List.of("Kappa", "PogChamp")))
                .topMessages(JsonListConverter.toJson(List.of("LETS GO")))
                .build();
    }

    @Test
    void findByChannelOrderByScoreDesc_returnsSortedResults() {
        Instant now = Instant.now();
        repository.save(createEntity("#xqc", 15.0, now));
        repository.save(createEntity("#xqc", 25.0, now));
        repository.save(createEntity("#forsen", 10.0, now));

        Pageable pageable = PageRequest.of(0, 10);
        Page<HighlightEntity> result = repository.findByChannelOrderByScoreDesc("#xqc", pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getScore()).isEqualTo(25.0);
        assertThat(result.getContent().get(1).getScore()).isEqualTo(15.0);
    }

    @Test
    void findByChannelAndCreatedAtAfter_filtersByDate() throws InterruptedException {
        HighlightEntity entity1 = createEntity("#xqc", 10.0, Instant.now());
        repository.save(entity1);
        Instant saved1CreatedAt = entity1.getCreatedAt();

        Thread.sleep(50);

        HighlightEntity entity2 = createEntity("#xqc", 20.0, Instant.now());
        repository.save(entity2);
        Instant saved2CreatedAt = entity2.getCreatedAt();

        Pageable pageable = PageRequest.of(0, 10);
        Page<HighlightEntity> result = repository.findByChannelAndCreatedAtAfter("#xqc", saved1CreatedAt, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getScore()).isEqualTo(20.0);
    }

    @Test
    void findByScoreGreaterThan_filtersByMinScore() {
        Instant now = Instant.now();
        repository.save(createEntity("#xqc", 5.0, now));
        repository.save(createEntity("#xqc", 15.0, now));
        repository.save(createEntity("#xqc", 25.0, now));

        Pageable pageable = PageRequest.of(0, 10);
        Page<HighlightEntity> result = repository.findByScoreGreaterThan(10.0, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findAll_withPageable_returnsPagedResults() {
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            repository.save(createEntity("#xqc", (double) i, now));
        }

        Pageable pageable = PageRequest.of(0, 2, Sort.by("score").descending());
        Page<HighlightEntity> result = repository.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }
}
