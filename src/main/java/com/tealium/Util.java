package com.tealium;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class Util {
    private Util() {
    }

    /**
     * @param s
     *            The String to test.
     * @return True if s is null or has length 0.
     */
    static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    static Map<String, Object> copySanitized(Map<String, ?> original) {

        final Map<String, Object> copy = new HashMap<>(original.size());

        String[] arrayValue;
        for (Map.Entry<String, ?> entry : original.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            if (isEmpty(key) || value == null) {
                continue;
            }

            if ((arrayValue = coerceIntoStringArray(value)) == null) {
                copy.put(entry.getKey(), value.toString());
            } else {
                copy.put(entry.getKey(), arrayValue);
            }
        }

        return copy;
    }

    /**
     * @param source
     *            The object to be copied into a String array of non-null
     *            values.
     * @return An array copy or null if the source is null, or not coerceable
     *         into an array.
     */
    static String[] coerceIntoStringArray(Object source) {

        if (source instanceof Collection) {
            return coerceCollection((Collection<?>) source);
        }

        return coerceArray(source);
    }

    /**
     * Converts a {@link Collection} into a String array with non-null elements.
     * 
     * @param collection
     * @return String array of non-null values, otherwise null if collection was
     *         null.
     */
    private static String[] coerceCollection(Collection<?> collection) {

        if (collection == null) {
            return null;
        }

        final String[] array = new String[collection.size()];
        int count = 0;

        Object value;
        for (Iterator<?> i = collection.iterator(); i.hasNext();) {
            if ((value = i.next()) != null) {
                array[count++] = value.toString();
            }
        }

        // Ensure entire array is non-null
        return array.length == count ? array : Arrays.copyOf(array, count);
    }

    /**
     * Converts a native array into a String array with non-null elements.
     * 
     * @param array
     * @return String array of non-null values, otherwise null if array was null
     *         or not an array.
     */
    private static String[] coerceArray(Object array) {

        if (array == null || !array.getClass().isArray()) {
            return null;
        }

        final String[] copy = new String[Array.getLength(array)];
        int count = 0;

        Object value;
        for (int i = 0; i < copy.length; i++) {
            if ((value = Array.get(array, i)) != null) {
                copy[count++] = value.toString();
            }
        }

        // Ensure entire array is non-null
        return copy.length == count ? copy : Arrays.copyOf(copy, count);
    }

}
