package com.fullonibus.api.service;

import com.fullonibus.highlight.Highlight;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class HighlightService {

    private final List<Highlight> highlights = new CopyOnWriteArrayList<>();

    public void addHighlight(Highlight highlight) {
        highlights.add(highlight);
    }

    public List<Highlight> getHighlights() {
        return Collections.unmodifiableList(new ArrayList<>(highlights));
    }
}
