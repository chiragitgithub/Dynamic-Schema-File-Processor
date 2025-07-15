package com.example.fileprocessor.validator.impl;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class BasicFieldValidator {

    public static Object transformAndConvert(String value, String type, String transform) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = applyTransform(value.trim(), transform);

        return convertType(value, type);
    }

    private static String applyTransform(String value, String transform) {
        if (transform == null) return value;

        switch (transform.toLowerCase()) {
            case "uppercase":
                return value.toUpperCase();
            case "lowercase":
                return value.toLowerCase();
            case "trim":
                return value.trim();
            case "none":
            case "":
                return value;
            default:
                throw new IllegalArgumentException("Unsupported transform: " + transform);
        }
    }

    private static Object convertType(String value, String type) throws Exception {
        switch (type.toLowerCase()) {
            case "string":
                return value;
            case "int":
            case "integer":
                return Integer.parseInt(value);
            case "float":
                return Float.parseFloat(value);
            case "double":
                return Double.parseDouble(value);
            case "boolean":
                return Boolean.parseBoolean(value);
            case "date":
                return parseDate(value);
            case "timestamp":
                return parseFlexibleTimestamp(value);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private static Date parseDate(String value) throws ParseException {
        // Accepts yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date utilDate = sdf.parse(value);
        return new Date(utilDate.getTime());
    }

    private static Object parseFlexibleTimestamp(String value) {
        try {

            OffsetDateTime odt = OffsetDateTime.parse(value);
            return odt;
        } catch (Exception ignore) {
        }

        try {

            LocalDateTime ldt = LocalDateTime.parse(value);
            return ldt;
        } catch (Exception ignore) {
        }

        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date utilDate = sdf.parse(value);
            return new Timestamp(utilDate.getTime());
        } catch (Exception ignore) {
        }

        throw new IllegalArgumentException("Unparseable timestamp: \"" + value + "\"");
    }
}
