package com.axway.library.common;

import com.axway.library.OpenAPIValidator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;

public class CacheInstanceAPIIDs {

    private static volatile CacheInstanceAPIIDs _instance;

    @Getter
    private final Cache<String, OpenAPIValidator> cache;

    public CacheInstanceAPIIDs() {
        cache = Caffeine.newBuilder().maximumSize(1000).build();
    }

    public static CacheInstanceAPIIDs getInstance() {
        if (_instance == null) {
            synchronized (CacheInstanceAPIIDs.class) {
                if (_instance == null) {
                    _instance = new CacheInstanceAPIIDs();
                }
            }
        }
        return _instance;
    }
}
