package com.atherion.andromeda.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public final class ControllerUtils {

    private ControllerUtils() {}

    public static ResponseEntity<Map<String, String>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
    }

    public static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    public static ResponseEntity<Map<String, String>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
    }

    public static ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
    }

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
