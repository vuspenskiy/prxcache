package vu.prxcache;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;


/**
 * @author v.uspenskiy
 * @since 06.12.11 8:02
 */
public class DynamicProxyCacheTest {

    @Test
    public void testSelectiveCaching() {

        Cache cache = new HashMapCache();
        TestDataProvider dataProvider = new TestDataProvider();
        Set<String> cachedMethods = new HashSet<String>(Arrays.asList("provideCachedData"));
        DataProvider cachedDataProvider = DynamicProxyCache.createFor(dataProvider, cache, cachedMethods);

        Assert.assertEquals("testDatatestData",
                            cachedDataProvider.provideCachedData(BigDecimal.ZERO));

        Assert.assertEquals("testDatatestData",
                            cachedDataProvider.provideCachedData(BigDecimal.ZERO));

        Assert.assertEquals("testDatatestDatatestDatatestDatatestDatatestData",
                            cachedDataProvider.provideNotCachedData(BigDecimal.ZERO));

        Assert.assertEquals("testDatatestDatatestDatatestDatatestDatatestData" +
                            "testDatatestDatatestDatatestDatatestDatatestData" +
                            "testDatatestDatatestDatatestDatatestDatatestData",
                            cachedDataProvider.provideNotCachedData(BigDecimal.ZERO));
    }

    @Test
    public void testCacheEverything() {

        Cache cache = new HashMapCache();
        TestDataProvider dataProvider = new TestDataProvider();
        DataProvider cachedDataProvider = DynamicProxyCache.createFor(dataProvider, cache);

        Assert.assertEquals("testDatatestData",
                            cachedDataProvider.provideCachedData(BigDecimal.ZERO));

        Assert.assertEquals("testDatatestData",
                            cachedDataProvider.provideCachedData(BigDecimal.ZERO));

        Assert.assertEquals("testDatatestDatatestDatatestDatatestDatatestData",
                            cachedDataProvider.provideNotCachedData(BigDecimal.ZERO));

        Assert.assertEquals("testDatatestDatatestDatatestDatatestDatatestData",
                            cachedDataProvider.provideNotCachedData(BigDecimal.ZERO));
    }

    private interface DataProvider {
        public String provideCachedData(Object param);
        public String provideNotCachedData(Object param);
    }

    private static class TestDataProvider implements DataProvider {

        private String testData = "testData";

        public String provideCachedData(Object param) {
            return testData = testData + testData;
        }

        public String provideNotCachedData(Object param) {
            return testData = testData + testData + testData;
        }
    }
}
