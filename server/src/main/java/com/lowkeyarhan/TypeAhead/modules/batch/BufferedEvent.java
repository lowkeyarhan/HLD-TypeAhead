package com.lowkeyarhan.TypeAhead.modules.batch;

import java.time.Instant;

// Record representing an aggregated search event waiting in the batch buffer.
public record BufferedEvent(long count, Instant lastSeenAt) {}
