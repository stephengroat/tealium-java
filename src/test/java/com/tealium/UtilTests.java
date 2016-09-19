package com.tealium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class UtilTests {

    @Test
    public void testIsEmpty() throws Exception {
        Assert.assertFalse(Util.isEmpty("foo"));
        Assert.assertTrue(Util.isEmpty(""));
        Assert.assertTrue(Util.isEmpty(null));
    }

    @SuppressWarnings("unchecked") // Eclipse is being stupid
    @Test
    public void testCoerceIntoStringArray() throws Exception {

        Assert.assertNull(Util.coerceIntoStringArray(null));
        Assert.assertNull(Util.coerceIntoStringArray("foo"));
        Assert.assertNull(Util.coerceIntoStringArray(new Object()));
        Assert.assertNull(Util.coerceIntoStringArray(new HashMap<String, Object>()));

        final String[] expected = { "alpha", "beta" };

        // ArrayList
        Assert.assertArrayEquals(expected,
                Util.coerceIntoStringArray(createCollection(ArrayList.class, "alpha", "beta")));

        Assert.assertArrayEquals(expected,
                Util.coerceIntoStringArray(createCollection(ArrayList.class, "alpha", null, "beta")));

        // HashSet
        Assert.assertArrayEquals(expected,
                Util.coerceIntoStringArray(createCollection(HashSet.class, "alpha", "beta")));

        Assert.assertArrayEquals(expected,
                Util.coerceIntoStringArray(createCollection(HashSet.class, "alpha", null, "beta")));

        // String[]
        Assert.assertArrayEquals(expected,
                Util.coerceIntoStringArray(new String[] { "alpha", "beta" }));

        Assert.assertArrayEquals(expected,
                Util.coerceIntoStringArray(new String[] { "alpha", null, "beta" }));

    }

    private static <T extends Collection<Object>> Collection<Object> createCollection(Class<T> collectionClass, Object... items) throws Exception {
        T collection = collectionClass.newInstance();
        for (Object item : items) {
            collection.add(item);
        }
        return collection;
    }
}
