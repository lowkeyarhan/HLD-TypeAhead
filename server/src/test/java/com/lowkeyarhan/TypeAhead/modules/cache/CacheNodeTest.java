package com.lowkeyarhan.TypeAhead.modules.cache;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

// Unit tests for CacheNode entry insertion, retrieval, and TTL expiry.
class CacheNodeTest {

    @Test
    void testCacheNodeTtlExpiry() {
        CacheNode node = new CacheNode("node-0");
        Instant baseTime = Instant.parse("2026-06-21T12:00:00Z");
        Clock clock = Clock.fixed(baseTime, ZoneId.of("UTC"));

        // Put an entry that expires in 10 seconds
        node.put("key1", "value1", baseTime.plusSeconds(10));

        // Get before expiration
        Optional<Object> value = node.get("key1", clock);
        assertThat(value).isPresent().contains("value1");
        assertThat(node.getHitCount()).isEqualTo(1L);
        assertThat(node.getMissCount()).isEqualTo(0L);

        // Move clock past the expiration time
        Clock expiredClock = Clock.fixed(baseTime.plusSeconds(11), ZoneId.of("UTC"));

        // Get after expiration (should be evicted/miss)
        Optional<Object> expiredValue = node.get("key1", expiredClock);
        assertThat(expiredValue).isEmpty();
        assertThat(node.getHitCount()).isEqualTo(1L);
        // Expiry should count as a miss
        assertThat(node.getMissCount()).isEqualTo(1L);
    }
}
