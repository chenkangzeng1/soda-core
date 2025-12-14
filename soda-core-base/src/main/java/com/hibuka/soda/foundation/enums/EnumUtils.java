package com.hibuka.soda.foundation.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration utility class for common enumeration operations.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public class EnumUtils {

    /**
     * Gets an enumeration by its code value.
     *
     * @param <T> the code type
     * @param <E> the enumeration type
     * @param enumClass the enumeration class
     * @param code the code value
     * @return the enumeration, or null if not found
     */
    public static <T, E extends Enum<?> & CodeEnum<T>> E getByCode(Class<E> enumClass, T code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> code.equals(e.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets an optional enumeration by its code value.
     *
     * @param <T> the code type
     * @param <E> the enumeration type
     * @param enumClass the enumeration class
     * @param code the code value
     * @return the optional enumeration
     */
    public static <T, E extends Enum<?> & CodeEnum<T>> Optional<E> getOptionalByCode(Class<E> enumClass, T code) {
        return Optional.ofNullable(getByCode(enumClass, code));
    }
}