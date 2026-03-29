CREATE TABLE highlights (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    channel VARCHAR(255) NOT NULL,
    start_timestamp TIMESTAMP NOT NULL,
    end_timestamp TIMESTAMP NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    message_count INT NOT NULL,
    emote_count INT NOT NULL,
    message_rate DOUBLE PRECISION NOT NULL,
    top_emotes TEXT,
    top_messages TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_highlights_channel ON highlights(channel);
CREATE INDEX idx_highlights_score ON highlights(score);
CREATE INDEX idx_highlights_created_at ON highlights(created_at);
CREATE INDEX idx_highlights_channel_score ON highlights(channel, score DESC);
