package com.lowkeyarhan.TypeAhead.modules.cache;

// Interface defining the strategy for hashing keys to a 64-bit long integer.
public interface HashFunction {
    long hash(String key);
}
