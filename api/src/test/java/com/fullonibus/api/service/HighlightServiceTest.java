package com.fullonibus.api.service;

import com.fullonibus.highlight.Highlight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HighlightServiceTest {

    @Autowired
    private HighlightService highlightService;

    @Test
    void addAndRetrieveHighlights() {
        Highlight highlight = Highlight.builder()
                .channel("#xqc")
                .startTimestamp(Instant.now())
                .endTimestamp(Instant.now().plusSeconds(30))
                .score(20.0)
                .messageCount(100)
                .emoteCount(50)
                .messageRate(3.33)
                .topEmotes(List.of("Kappa"))
                .topMessages(List.of("LETS GO"))
                .build();

        highlightService.addHighlight(highlight);

        Page<Highlight> results = highlightService.getHighlights(PageRequest.of(0, 10));
        assertThat(results.getTotalElements()).isGreaterThanOrEqualTo(1);

        Highlight found = results.getContent().stream()
                .filter(h -> h.getChannel().equals("#xqc") && h.getScore() == 20.0)
                .findFirst()
                .orElseThrow();
        assertThat(found.getChannel()).isEqualTo("#xqc");
        assertThat(found.getScore()).isEqualTo(20.0);
        assertThat(found.getTopEmotes()).containsExactly("Kappa");
        assertThat(found.getTopMessages()).containsExactly("LETS GO");
    }

    @Test
    void getHighlightsByChannel_filtersCorrectly() {
        highlightService.addHighlight(Highlight.builder()
                .channel("#forsen")
                .startTimestamp(Instant.now())
                .endTimestamp(Instant.now().plusSeconds(30))
                .score(15.0)
                .messageCount(50)
                .emoteCount(20)
                .messageRate(1.67)
                .topEmotes(List.of("EZ"))
                .topMessages(List.of("OMEGALUL"))
                .build());

        Page<Highlight> results = highlightService.getHighlightsByChannel("#forsen", PageRequest.of(0, 10));
        assertThat(results).isNotEmpty();
        assertThat(results.getContent().get(0).getChannel()).isEqualTo("#forsen");
    }
}
