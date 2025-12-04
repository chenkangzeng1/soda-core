package com.hibuka.soda.base.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class EnumUtils {
    private static final Map<Class<?>, Map<Integer, ? extends CodeEnum>> CODE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, ? extends CodeEnum>> DESC_CACHE = new ConcurrentHashMap<>();

    private EnumUtils() {}

    public static <T extends Enum<T> & CodeEnum> Optional<T> fromCode(Class<T> enumClass, Integer code) {
        if (enumClass == null || code == null) return Optional.empty();
        for (T e : enumClass.getEnumConstants()) {
            if (Objects.equals(e.getCode(), code)) return Optional.of(e);
        }
        return Optional.empty();
    }

    public static <T extends Enum<T> & CodeEnum> Optional<T> fromDescription(Class<T> enumClass, String description) {
        if (enumClass == null || description == null) return Optional.empty();
        for (T e : enumClass.getEnumConstants()) {
            if (Objects.equals(e.getDescription(), description)) return Optional.of(e);
        }
        return Optional.empty();
    }

    public static <T extends Enum<T> & CodeEnum> T fromCodeOrDefault(Class<T> enumClass, Integer code, T defaultValue) {
        return fromCode(enumClass, code).orElse(defaultValue);
    }

    public static <T extends Enum<T> & CodeEnum> T fromDescriptionOrDefault(Class<T> enumClass, String description, T defaultValue) {
        return fromDescription(enumClass, description).orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & CodeEnum> T fastFromCode(Class<T> enumClass, Integer code) {
        if (enumClass == null || code == null) return null;
        Map<Integer, T> map = (Map<Integer, T>) CODE_CACHE.computeIfAbsent(enumClass, cls ->
                Arrays.stream(enumClass.getEnumConstants()).collect(Collectors.toUnmodifiableMap(
                        T::getCode,
                        e -> e,
                        (a, b) -> { throw new IllegalStateException("duplicate code: " + a.getCode()); }
                ))
        );
        return map.get(code);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & CodeEnum> T fastFromDescription(Class<T> enumClass, String description) {
        if (enumClass == null || description == null) return null;
        Map<String, T> map = (Map<String, T>) DESC_CACHE.computeIfAbsent(enumClass, cls ->
                Arrays.stream(enumClass.getEnumConstants()).collect(Collectors.toUnmodifiableMap(
                        T::getDescription,
                        e -> e,
                        (a, b) -> { throw new IllegalStateException("duplicate description: " + a.getDescription()); }
                ))
        );
        return map.get(description);
    }
}
