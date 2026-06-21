package com.lowkeyarhan.TypeAhead.modules.batch;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

// Unit tests for SearchEventBuffer concurrent safety and draining behavior.
class SearchEventBufferTest {

    @Test
    void testConcurrentIncrementsAndDraining() throws InterruptedException {
        Clock clock = Clock.fixed(Instant.parse("2026-06-21T12:00:00Z"), ZoneId.of("UTC"));
        SearchEventBuffer buffer = new SearchEventBuffer(clock);

        int threadsCount = 10;
        int incrementsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        buffer.increment("iphone");
                        buffer.increment("ipad");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(buffer.size()).isEqualTo(2);
        assertThat(buffer.getTotalRequests()).isEqualTo(20000L);

        Map<String, BufferedEvent> drained = buffer.drainAndSwap();

        // Check buffer has been cleared/swapped
        assertThat(buffer.size()).isEqualTo(0);

        // Check drained content is accurate
        assertThat(drained).hasSize(2);
        assertThat(drained.get("iphone").count()).isEqualTo(10000L);
        assertThat(drained.get("iphone").lastSeenAt()).isEqualTo(clock.instant());
        assertThat(drained.get("ipad").count()).isEqualTo(10000L);
        assertThat(drained.get("ipad").lastSeenAt()).isEqualTo(clock.instant());
    }
}
