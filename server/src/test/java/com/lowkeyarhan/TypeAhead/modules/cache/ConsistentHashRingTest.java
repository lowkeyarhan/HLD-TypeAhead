package com.lowkeyarhan.TypeAhead.modules.cache;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

// Unit tests for ConsistentHashRing routing and distribution characteristics.
class ConsistentHashRingTest {

    @Test
    void testDistributionAndRemappingOnNodeAddition() {
        HashFunction hashFunction = new Md5HashFunction();
        int virtualNodes = 150;
        ConsistentHashRing ring = new ConsistentHashRing(hashFunction, virtualNodes);

        List<CacheNode> initialNodes = List.of(
                new CacheNode("node-0"),
                new CacheNode("node-1"),
                new CacheNode("node-2"));

        for (CacheNode node : initialNodes) {
            ring.addNode(node);
        }

        // Test distribution with 10,000 random keys
        int numKeys = 10000;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < numKeys; i++) {
            keys.add(UUID.randomUUID().toString());
        }

        Map<String, String> originalAssignments = new HashMap<>();
        Map<String, Integer> nodeCounts = new HashMap<>();

        for (String key : keys) {
            CacheNode routed = ring.getNode(key);
            assertThat(routed).isNotNull();
            originalAssignments.put(key, routed.getId());
            nodeCounts.put(routed.getId(), nodeCounts.getOrDefault(routed.getId(), 0) + 1);
        }

        // Check that keys are distributed to all 3 nodes
        assertThat(nodeCounts.keySet()).hasSize(3);
        for (String node : nodeCounts.keySet()) {
            // Roughly even: each node should get at least 20% and at most 45% of keys
            // (ideal is 33.3%)
            double percentage = (double) nodeCounts.get(node) / numKeys;
            assertThat(percentage).isBetween(0.20, 0.45);
        }

        // Add a new node: node-3
        CacheNode newNode = new CacheNode("node-3");
        ring.addNode(newNode);

        int remappedKeys = 0;
        for (String key : keys) {
            CacheNode newlyRouted = ring.getNode(key);
            String oldNodeId = originalAssignments.get(key);
            if (!newlyRouted.getId().equals(oldNodeId)) {
                remappedKeys++;
                // Remapped keys MUST land on the new node "node-3" only (the core property of
                // consistent hashing)
                assertThat(newlyRouted.getId()).isEqualTo("node-3");
            }
        }

        // Expected remapped ratio should be roughly 1/4 (25%)
        double remappedPercentage = (double) remappedKeys / numKeys;
        assertThat(remappedPercentage).isBetween(0.15, 0.35);
    }
}
