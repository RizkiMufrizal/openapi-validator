package com.axway.library.common;

import com.vordel.circuit.cache.CacheContainer;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class CacheInstance {

    private final Cache cache;

    public CacheInstance(String name) {
        cache = CacheContainer.getInstance().getCache(name);
    }

    public <T> void addCache(T key, Object value) {
        var element = cache.get(key);
        if (element != null) {
            cache.removeElement(element);
        }
        element = new Element(key, value);
        cache.put(element);
    }

    public <T, K> T getCache(K key, Class<T> clazz) {
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        return clazz.cast(element.getObjectValue());
    }
}
