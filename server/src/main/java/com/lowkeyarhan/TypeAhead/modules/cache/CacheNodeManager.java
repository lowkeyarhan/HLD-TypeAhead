package com.lowkeyarhan.TypeAhead.modules.cache;

import com.lowkeyarhan.TypeAhead.common.config.CacheProperties;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Facade service orchestrating consistent hashing ring routing and local cache operations.
@Service
public class CacheNodeManager {

    private final ConsistentHashRing ring;
    private final List<CacheNode> nodes;
    private final Clock clock;
    private final CacheProperties cacheProperties;

    public CacheNodeManager(CacheProperties cacheProperties, HashFunction hashFunction, Clock clock) {
        this.clock = clock;
        this.cacheProperties = cacheProperties;
        this.ring = new ConsistentHashRing(hashFunction, cacheProperties.getVirtualNodes());
        this.nodes = new ArrayList<>();

        // Initialize consistent hashing ring with configured number of physical nodes
        for (int i = 0; i < cacheProperties.getNodeCount(); i++) {
            CacheNode node = new CacheNode("node-" + i);
            nodes.add(node);
            ring.addNode(node);
        }
    }

    // Retrieves value from appropriate routed node, or empty if miss/expired.
    public Optional<Object> get(String key) {
        CacheNode node = ring.getNode(key);
        if (node == null) {
            return Optional.empty();
        }
        return node.get(key, clock);
    }

    // Puts a value into the routed cache node using default TTL.
    public void put(String key, Object value) {
        put(key, value, cacheProperties.getTtlSeconds());
    }

    // Puts a value into the routed cache node with custom TTL.
    public void put(String key, Object value, long ttlSeconds) {
        CacheNode node = ring.getNode(key);
        if (node != null) {
            node.put(key, value, Instant.now(clock).plusSeconds(ttlSeconds));
        }
    }

    // Invalidates a key on the appropriate routed node.
    public void invalidate(String key) {
        CacheNode node = ring.getNode(key);
        if (node != null) {
            node.invalidate(key);
        }
    }

    // Retrieves diagnostics information about key allocation on the ring.
    public CacheDebugInfo getDebugInfo(String key) {
        CacheNode node = ring.getNode(key);
        if (node == null) {
            return new CacheDebugInfo("none", false);
        }
        boolean hit = node.hasValidEntry(key, clock);
        return new CacheDebugInfo(node.getId(), hit);
    }

    // Exposes the list of physical cache nodes for telemetry.
    public List<CacheNode> getNodes() {
        return nodes;
    }
}
