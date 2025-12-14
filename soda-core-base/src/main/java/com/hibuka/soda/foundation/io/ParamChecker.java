package com.hibuka.soda.foundation.io;

import com.hibuka.soda.foundation.error.BaseException;
import com.hibuka.soda.foundation.error.BaseErrorCode;

import java.util.*;
import java.util.function.Supplier;

/**
 * Parameter checker utility class for validating input parameters.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public class ParamChecker {

    /**
     * Private constructor to prevent instantiation.
     */
    private ParamChecker() {
    }

    /**
     * Checks if the given object is null, throws BaseException with BAD_REQUEST if it is.
     *
     * @param obj object to check
     * @param message error message
     * @param <T> object type
     * @return the object if not null
     */
    public static <T> T notNull(T obj, String message) {
        if (obj == null) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return obj;
    }

    /**
     * Checks if the given object is null, throws BaseException with specified error code if it is.
     *
     * @param obj object to check
     * @param errorCode error code
     * @param message error message
     * @param <T> object type
     * @return the object if not null
     */
    public static <T> T notNull(T obj, int errorCode, String message) {
        if (obj == null) {
            throw new BaseException(errorCode, message);
        }
        return obj;
    }

    /**
     * Checks if the given object is null, throws exception supplied by exceptionSupplier if it is.
     *
     * @param obj object to check
     * @param exceptionSupplier exception supplier
     * @param <T> object type
     * @param <E> exception type
     * @return the object if not null
     * @throws E if the object is null
     */
    public static <T, E extends RuntimeException> T notNull(T obj, Supplier<E> exceptionSupplier) throws E {
        if (obj == null) {
            throw exceptionSupplier.get();
        }
        return obj;
    }

    /**
     * Checks if the given string is null or empty, throws BaseException with BAD_REQUEST if it is.
     *
     * @param str string to check
     * @param message error message
     * @return the string if not null or empty
     */
    public static String notEmpty(String str, String message) {
        if (str == null || str.isEmpty()) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return str;
    }

    /**
     * Checks if the given string is null or blank, throws BaseException with BAD_REQUEST if it is.
     *
     * @param str string to check
     * @param message error message
     * @return the string if not null or blank
     */
    public static String notBlank(String str, String message) {
        if (str == null || str.isBlank()) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return str;
    }

    /**
     * Checks if the given collection is null or empty, throws BaseException with BAD_REQUEST if it is.
     *
     * @param collection collection to check
     * @param message error message
     * @param <T> collection type
     * @return the collection if not null or empty
     */
    public static <T extends Collection<?>> T notEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return collection;
    }

    /**
     * Checks if the given map is null or empty, throws BaseException with BAD_REQUEST if it is.
     *
     * @param map map to check
     * @param message error message
     * @param <K> key type
     * @param <V> value type
     * @return the map if not null or empty
     */
    public static <K, V> Map<K, V> notEmpty(Map<K, V> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return map;
    }

    /**
     * Checks if the given array is null or empty, throws BaseException with BAD_REQUEST if it is.
     *
     * @param array array to check
     * @param message error message
     * @param <T> array type
     * @return the array if not null or empty
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return array;
    }

    /**
     * Checks if the given boolean condition is true, throws BaseException with BAD_REQUEST if it is not.
     *
     * @param condition condition to check
     * @param message error message
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
    }

    /**
     * Checks if the given boolean condition is true, throws BaseException with specified error code if it is not.
     *
     * @param condition condition to check
     * @param errorCode error code
     * @param message error message
     */
    public static void isTrue(boolean condition, int errorCode, String message) {
        if (!condition) {
            throw new BaseException(errorCode, message);
        }
    }

    /**
     * Checks if the given boolean condition is true, throws exception supplied by exceptionSupplier if it is not.
     *
     * @param condition condition to check
     * @param exceptionSupplier exception supplier
     * @param <E> exception type
     * @throws E if the condition is false
     */
    public static <E extends RuntimeException> void isTrue(boolean condition, Supplier<E> exceptionSupplier) throws E {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Checks if the given boolean condition is false, throws BaseException with BAD_REQUEST if it is true.
     *
     * @param condition condition to check
     * @param message error message
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
    }

    /**
     * Checks if the given value is in the given range, throws BaseException with BAD_REQUEST if it is not.
     *
     * @param value value to check
     * @param min minimum value
     * @param max maximum value
     * @param message error message
     * @param <T> value type
     * @return the value if in range
     */
    public static <T extends Comparable<T>> T inRange(T value, T min, T max, String message) {
        notNull(value, message);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return value;
    }

    /**
     * Checks if the given string length is in the given range, throws BaseException with BAD_REQUEST if it is not.
     *
     * @param str string to check
     * @param min minimum length
     * @param max maximum length
     * @param message error message
     * @return the string if length in range
     */
    public static String lengthInRange(String str, int min, int max, String message) {
        notNull(str, message);
        if (str.length() < min || str.length() > max) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return str;
    }

    /**
     * Checks if the given string matches the given regular expression, throws BaseException with BAD_REQUEST if it does not.
     *
     * @param str string to check
     * @param regex regular expression
     * @param message error message
     * @return the string if matches
     */
    public static String matches(String str, String regex, String message) {
        notNull(str, message);
        if (!str.matches(regex)) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return str;
    }

    /**
     * Checks if the given value is in the given collection, throws BaseException with BAD_REQUEST if it is not.
     *
     * @param value value to check
     * @param collection collection to check in
     * @param message error message
     * @param <T> value type
     * @return the value if in collection
     */
    public static <T> T inCollection(T value, Collection<T> collection, String message) {
        notNull(value, message);
        notEmpty(collection, message);
        if (!collection.contains(value)) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return value;
    }

    /**
     * Checks if the given value is in the given array, throws BaseException with BAD_REQUEST if it is not.
     *
     * @param value value to check
     * @param array array to check in
     * @param message error message
     * @param <T> value type
     * @return the value if in array
     */
    public static <T> T inArray(T value, T[] array, String message) {
        notNull(value, message);
        notEmpty(array, message);
        if (!Arrays.asList(array).contains(value)) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return value;
    }

    /**
     * Checks if the given string is null or empty, returns the default value if it is.
     *
     * @param str string to check
     * @param defaultValue default value
     * @return the string if not null or empty, default value otherwise
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return str == null || str.isEmpty() ? defaultValue : str;
    }

    /**
     * Checks if the given string is null or blank, returns the default value if it is.
     *
     * @param str string to check
     * @param defaultValue default value
     * @return the string if not null or blank, default value otherwise
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return str == null || str.isBlank() ? defaultValue : str;
    }

    /**
     * Checks if the given object is null, returns the default value if it is.
     *
     * @param obj object to check
     * @param defaultValue default value
     * @param <T> object type
     * @return the object if not null, default value otherwise
     */
    public static <T> T defaultIfNull(T obj, T defaultValue) {
        return obj == null ? defaultValue : obj;
    }

    /**
     * Checks if the given collection is null, returns an empty collection if it is.
     *
     * @param collection collection to check
     * @param <T> collection type
     * @return the collection if not null, empty collection otherwise
     */
    public static <T extends Collection<?>> T defaultIfNull(T collection) {
        if (collection == null) {
            if (collection instanceof List) {
                return (T) Collections.emptyList();
            } else if (collection instanceof Set) {
                return (T) Collections.emptySet();
            } else if (collection instanceof Map) {
                return (T) Collections.emptyMap();
            } else {
                throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), "Unsupported collection type");
            }
        }
        return collection;
    }

    /**
     * Checks if the given map is null, returns an empty map if it is.
     *
     * @param map map to check
     * @param <K> key type
     * @param <V> value type
     * @return the map if not null, empty map otherwise
     */
    public static <K, V> Map<K, V> defaultIfNull(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Checks if the given array is null, returns an empty array if it is.
     *
     * @param array array to check
     * @param <T> array type
     * @return the array if not null, empty array otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] defaultIfNull(T[] array) {
        return array == null ? (T[]) new Object[0] : array;
    }
}