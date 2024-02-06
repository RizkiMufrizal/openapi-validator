package com.axway.library.common;

import com.vordel.circuit.cache.CacheContainer;
import lombok.Getter;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class CacheInstance {

    @Getter
    private final Cache cache;

    public CacheInstance(String name) {
        cache = CacheContainer.getInstance().getCache(name);
    }

    public void addCache(String key, Object value) {
        var element = cache.get(key);
        if (element != null) {
            cache.removeElement(element);
        }
        element = new Element(key, value);
        cache.put(element);
    }

    public void addCache(Integer key, Object value) {
        var element = cache.get(key);
        if (element != null) {
            cache.removeElement(element);
        }
        element = new Element(key, value);
        cache.put(element);
    }

    public <T> T getCache(String key, Class<T> clazz) {
        Element element = cache.get(key);
        return clazz.cast(element.getObjectValue());
    }

    public <T> T getCache(Integer key, Class<T> clazz) {
        Element element = cache.get(key);
        return clazz.cast(element.getObjectValue());
    }
}
