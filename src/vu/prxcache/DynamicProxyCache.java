package vu.prxcache;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.Set;


/**
 * Dynamic Proxy Cache
 *
 * Uses dynamic proxies to give caching capability to every single object
 * that needs it regardless of its methods signatures.
 *
 * @see <a href="http://docs.oracle.com/javase/1.3/docs/guide/reflection/proxy.html">Dynamic Proxy Classes</a>
 *
 * @author v.uspenskiy
 * @since 06.12.11 7:49
 */
public class DynamicProxyCache implements InvocationHandler {

    private final Cache cache;
    private final Object dataProvidingObject;
    private final Set<String> cachedMethods;

    public DynamicProxyCache(Cache cache,
                             Object dataProvidingObject) {

        this.cache = cache;
        this.dataProvidingObject = dataProvidingObject;
        this.cachedMethods = Collections.emptySet();
    }

    public DynamicProxyCache(Cache cache,
                             Object dataProvidingObject,
                             Set<String> cachedMethods) {

        this.cache = cache;
        this.dataProvidingObject = dataProvidingObject;
        this.cachedMethods = cachedMethods;
    }

    /**
     * Fabric method for Dynamic Proxy Caches
     *
     * @param dataProvider - an object which provides data if there is nothing found in cache,
     *                       needs to have interface on the top of the class to allow proxy cast to it.
     * @param cache - cache implementation to be used (implement with Memcached, Cassandra, etc.)
     * @param cachedMethods - specifies the methods to be cached on dataProvider object
     * @param <T> - type of the dataProvider
     * @return An instance of the object which caches methods calls
     */
    public static <T> T createFor(T dataProvider, Cache cache, Set<String> cachedMethods) {
        return (T) Proxy.newProxyInstance(
            dataProvider.getClass().getClassLoader(),
            dataProvider.getClass().getInterfaces(),
            new DynamicProxyCache(cache, dataProvider, cachedMethods));
    }

    /**
     * Fabric method for Dynamic Proxy Caches.
     * Creates cache for all dataProvider methods.
     *
     * @param dataProvider - an object which provides data if there is nothing found in cache,
     *                       needs to have interface on the top of the class to allow proxy cast to it.
     * @param cache - cache implementation to be used (implement with Memcached, Cassandra, etc.)
     * @param <T> - type of the dataProvider
     * @return An instance of the object which caches methods calls
     */
    public static <T> T createFor(T dataProvider, Cache cache) {
        return (T) Proxy.newProxyInstance(
            dataProvider.getClass().getClassLoader(),
            dataProvider.getClass().getInterfaces(),
            new DynamicProxyCache(cache, dataProvider));
    }

    /**
     * Proxy method, handling all invocations on dataProvidingObject.
     * Using isCachingEnabledForObjectMethod it determines whether dataProvidingObject method
     * needs caching and uses Cache implementation to cache method call with arguments.
     *
     * @param object - Object, the method is called on (does not matter as we are acting on dataProvidingObject)
     * @param method - Method that was called and need to be cached and proxied to dataProvidingObject
     * @param arguments - Method arguments to be used in cache key and dataProvidingObject method invocation
     * @return The value returned with dataProvidingObject method, which is supposed to be cached
     * @throws Throwable - exception thrown by dataProvidingObject method or during caching
     */
    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {

        boolean cachingEnabledForObjectMethod = isCachingEnabledForObjectMethod(dataProvidingObject, method);
        Object key = null;

        if(cachingEnabledForObjectMethod) {
            key = composeKey(dataProvidingObject, method, arguments);
            Object cachedValue = cache.get(key);
            if(cachedValue != null) {
                return cachedValue;
            }
        }

        Object value;
        try {
            value = method.invoke(dataProvidingObject, arguments);
        } catch(InvocationTargetException e) {
            throw e.getTargetException();
        }

        if(cachingEnabledForObjectMethod) {
            try {
                cache.set(key, value);
            } catch(Exception e) {
                handleException(e);
            }
        }

        return value;
    }

    /**
     * Method defines whether dataProvidingObject method calls should be cached.
     * By default relies on cachedMethods set, containing all the cached methods names.
     * If neither method is specified at cachedMethods, all dataProvidingObject methods are cached.
     * To disable caching just stop using this cache class.
     *
     * Method could be overridden to establish your own caching strategy.
     *
     * @param object - data providing object is passed to this method to be probably used in inheritors,
     *                 overriding default caching detection behaviour.
     * @param method - method is the method that is intended to be called if caching is enabled for it
     * @return truth whether method call should be cached
     */
    @SuppressWarnings("UnusedParameters")
    protected boolean isCachingEnabledForObjectMethod(Object object, Method method) {
        return cachedMethods.isEmpty() || cachedMethods.contains(method.getName());
    }

    /**
     * Composes the key for the cache. Could be overriden by inheritors
     *
     * @param object - data provider object used for data retrieval when no records found in cache
     * @param method - method used on o to retrieve data
     * @param args - method arguments used to retrieve data
     * @return key-object used to query the cache on values (better if it depends on all of the arguments)
     * @throws Exception when key could not be generated on parameters
     */
    protected Object composeKey(Object object, Method method, Object[] args) throws Exception {

        StringBuilder keyBuilder = new StringBuilder(128);

        keyBuilder.append(object.getClass().getCanonicalName())
                  .append('.')
                  .append(method.getName());

        if(args != null) {
            keyBuilder.append(":").append(new String(serializeArguments(args)));
        }

        return keyBuilder.toString();
    }

    /**
     * Serializes arguments of the called cached method.
     * As arguments could be hard to serialize this method is declared
     * protected to facilitate arguments serialisation strategy overriding
     *
     * @param arguments – method arguments to serialize
     * @return array of bytes containing serialized arguments
     * @throws Exception when error during serialisation occurs
     */
    protected byte[] serializeArguments(Object[] arguments) throws Exception {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytes);

        for(Object argument : arguments) {
            oos.writeObject(argument);
        }

        return bytes.toByteArray();
    }

    /**
     * Method handles exceptions when value cannot be set to the cache.
     * Override to handle such exceptions in your own specific way.
     *
     * @param e - exception to handle
     */
    @SuppressWarnings("UnusedParameters")
    protected void handleException(Exception e) {

    }
}
