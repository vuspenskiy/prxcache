package vu.prxcache;

import java.util.HashMap;
import java.util.Map;


/**
 * Simple cache implementation based on the HashMap.
 *
 * @author v.uspenskiy
 * @since 06.12.11 8:45
 */
public class HashMapCache implements Cache {

    private final Map<Object, Object> hashMap;

    public HashMapCache() {
        this.hashMap = new HashMap<Object, Object>();
    }

    public Object get(Object key) {
        return hashMap.get(key);
    }

    public void set(Object key, Object value) {
        hashMap.put(key, value);
    }
}
