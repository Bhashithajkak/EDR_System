package com.example.edrsystem.static_analysis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiringCache<K, V> {
    private final long expirationMillis;
    private final Map<K, CacheEntry<V>> map;

    public ExpiringCache(long expirationMillis) {
        this.expirationMillis = expirationMillis;
        this.map = new ConcurrentHashMap<>();
    }

    public void put(K key, V value) {
        map.put(key, new CacheEntry<>(value, System.currentTimeMillis() + expirationMillis));
    }

    public V get(K key) {
        CacheEntry<V> entry = map.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            map.remove(key);
            return null;
        }
        return entry.value;
    }

    private static class CacheEntry<V> {
        final V value;
        final long expirationTime;

        CacheEntry(V value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}