CREATE TABLE query_count (
    id BIGSERIAL PRIMARY KEY,
    query_text VARCHAR(255) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    recent_count BIGINT NOT NULL DEFAULT 0,
    last_searched_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_query_text UNIQUE (query_text)
);

CREATE INDEX idx_query_text_prefix ON query_count (query_text);
