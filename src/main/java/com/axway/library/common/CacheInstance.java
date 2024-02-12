package com.axway.library.common;

import com.vordel.circuit.cache.CacheContainer;
import net.sf.ehcache.Element;

public class CacheInstance {

    public <T> void addCache(String name, T key, Object value) {
        var cache = CacheContainer.getInstance().getCache(name);
        var element = cache.get(key);
        if (element != null) {
            cache.removeElement(element);
        }
        element = new Element(key, value);
        cache.put(element);
    }

    public <T, K> T getCache(String name, K key, Class<T> clazz) {
        var cache = CacheContainer.getInstance().getCache(name);
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        return clazz.cast(element.getObjectValue());
    }
}
