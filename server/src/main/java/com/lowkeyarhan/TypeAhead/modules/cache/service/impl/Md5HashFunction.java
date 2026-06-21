package com.lowkeyarhan.TypeAhead.modules.cache.service.impl;

import com.lowkeyarhan.TypeAhead.modules.cache.service.HashFunction;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// MD5-based implementation of HashFunction.
@Component
public class Md5HashFunction implements HashFunction {

    @Override
    public long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            // Convert first 8 bytes of MD5 digest to a long value
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 MessageDigest not available", e);
        }
    }
}
