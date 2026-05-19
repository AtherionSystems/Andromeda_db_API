package com.atherion.andromeda.util;

public final class ControllerUtils {

    private ControllerUtils() {}

    public static Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public static String defaulted(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
