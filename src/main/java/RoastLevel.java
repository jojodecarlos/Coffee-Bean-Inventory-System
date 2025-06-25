package com.example.coffeedms;

/**
 * Joses De Carlos, CEN3024C 31774, 06-18-2025
 * Enumeration of possible roast levels for coffee beans.
 */
public enum RoastLevel {
    LIGHT, MEDIUM, DARK;

    /**
     * fromString
     *
     * Converts a text value into a RoastLevel constant.
     *
     * @param value text to parse
     * @return matching RoastLevel
     * @throws IllegalArgumentException if no match
     */
    public static RoastLevel fromString(String value) {
        for (RoastLevel level : values()) {
            if (level.name().equalsIgnoreCase(value.trim())) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid roast level: " + value);
    }
}
