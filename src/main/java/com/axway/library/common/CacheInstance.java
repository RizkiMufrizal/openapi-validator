package com.axway.library.common;

import com.axway.library.OpenAPIValidator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;

public class CacheInstance {

    private static volatile CacheInstance _instance;

    @Getter
    private final Cache<Integer, OpenAPIValidator> cache;

    public CacheInstance() {
        cache = Caffeine.newBuilder().maximumSize(1000).build();
    }

    public static CacheInstance getInstance() {
        if (_instance == null) {
            synchronized (CacheInstance.class) {
                if (_instance == null) {
                    _instance = new CacheInstance();
                }
            }
        }
        return _instance;
    }
}
