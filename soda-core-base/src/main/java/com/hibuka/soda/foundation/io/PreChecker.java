package com.hibuka.soda.foundation.io;

import com.hibuka.soda.foundation.error.BaseException;
import com.hibuka.soda.foundation.error.BaseErrorCode;

/**
 * Pre-checker utility class for validating pre-conditions.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Deprecated
public class PreChecker {

    /**
     * Private constructor to prevent instantiation.
     */
    private PreChecker() {
    }

    /**
     * Checks if the given boolean condition is true, throws BaseException with specified error code and message if it is not.
     *
     * @param condition condition to check
     * @param errorCode error code
     * @param message error message
     */
    @Deprecated
    public static void checkArgument(boolean condition, int errorCode, String message) {
        if (!condition) {
            throw new BaseException(errorCode, message);
        }
    }

    /**
     * Checks if the given boolean condition is true, throws BaseException with BAD_REQUEST and message if it is not.
     *
     * @param condition condition to check
     * @param message error message
     */
    @Deprecated
    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
    }

    /**
     * Checks if the given object is null, throws BaseException with BAD_REQUEST and message if it is.
     *
     * @param obj object to check
     * @param message error message
     * @param <T> object type
     * @return the object if not null
     */
    @Deprecated
    public static <T> T notNull(T obj, String message) {
        if (obj == null) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return obj;
    }

    /**
     * Checks if the given string is null or empty, throws BaseException with BAD_REQUEST and message if it is.
     *
     * @param str string to check
     * @param message error message
     * @return the string if not null or empty
     */
    @Deprecated
    public static String notEmpty(String str, String message) {
        if (str == null || str.isEmpty()) {
            throw new BaseException(BaseErrorCode.BAD_REQUEST.getCode(), message);
        }
        return str;
    }
}
