package com.lowkeyarhan.TypeAhead.modules.cache;

import java.util.Map;
import java.util.TreeMap;

// Thread-safe consistent hash ring structure using TreeMap.
public class ConsistentHashRing {

    private final HashFunction hashFunction;
    private final int virtualNodes;
    private final TreeMap<Long, CacheNode> ring;

    public ConsistentHashRing(HashFunction hashFunction, int virtualNodes) {
        this.hashFunction = hashFunction;
        this.virtualNodes = virtualNodes;
        this.ring = new TreeMap<>();
    }

    // Adds a physical cache node to the ring by creating virtual nodes.
    public synchronized void addNode(CacheNode node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hashFunction.hash(node.getId() + "-vnode-" + i);
            ring.put(hash, node);
        }
    }

    // Removes a physical cache node and all its virtual nodes from the ring.
    public synchronized void removeNode(String nodeId) {
        ring.entrySet().removeIf(entry -> entry.getValue().getId().equals(nodeId));
    }

    // Routes a query key to the nearest node on the ring.
    public synchronized CacheNode getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = hashFunction.hash(key);
        Map.Entry<Long, CacheNode> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            return ring.firstEntry().getValue();
        }
        return entry.getValue();
    }

    // Returns total virtual nodes currently mapped in the ring.
    public synchronized int size() {
        return ring.size();
    }
}
