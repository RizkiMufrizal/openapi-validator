package com.axway.library.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;

public class CacheExposurePath2SpecifiedPathMap {
    private static volatile CacheExposurePath2SpecifiedPathMap _instance;

    @Getter
    private final Cache<String, Object> cache;

    public CacheExposurePath2SpecifiedPathMap() {
        cache = Caffeine.newBuilder().maximumSize(1000).build();
    }

    public static CacheExposurePath2SpecifiedPathMap getInstance() {
        if (_instance == null) {
            synchronized (CacheExposurePath2SpecifiedPathMap.class) {
                if (_instance == null) {
                    _instance = new CacheExposurePath2SpecifiedPathMap();
                }
            }
        }
        return _instance;
    }
}
