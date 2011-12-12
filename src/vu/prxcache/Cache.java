package vu.prxcache;

/**
 * @author v.uspenskiy
 * @since 06.12.11 7:58
 */
public interface Cache {

    Object get(Object key);

    void set(Object key, Object value);
}
