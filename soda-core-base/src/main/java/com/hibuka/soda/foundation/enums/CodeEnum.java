package com.hibuka.soda.foundation.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Code-based enumeration interface definition.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public interface CodeEnum<T> {
    
    /**
     * Gets the code value of the enumeration.
     *
     * @return the code value
     */
    T getCode();
    
    /**
     * Gets the description of the enumeration.
     *
     * @return the description
     */
    String getDesc();
    
    /**
     * Gets the list of all enumeration values.
     *
     * @param <T> the code type
     * @param <E> the enumeration type
     * @param enumClass the enumeration class
     * @return the list of all enumeration values
     */
    @SuppressWarnings("unchecked")
    static <T, E extends Enum<?> & CodeEnum<T>> List<E> getAllEnumValues(Class<E> enumClass) {
        List<E> result = new ArrayList<>();
        Enum<?>[] enums = enumClass.getEnumConstants();
        for (Enum<?> e : enums) {
            result.add((E) e);
        }
        return result;
    }
}