package com.belmonttech.graph.model;

public enum CostLevel {
    LOWEST(0, "LOWEST"),
    LOW(1, "LOW"),
    MEDIUM(2, "MEDIUM"),
    HIGH(3, "HIGH"),
    CRITICAL(4, "CRITICAL"),
    UNKNOWN(-1, "UNKNOWN");

    private final int code;
    private final String label;

    CostLevel(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static CostLevel fromAny(Object raw) {
        if (raw == null) return LOWEST;

        // numeric cost
        if (raw instanceof Number num) {
            return fromCode(num.intValue());
        }

        // string cost
        String s = raw.toString().trim().toUpperCase();

        return switch (s) {
            case "0", "LOWEST"   -> LOWEST;
            case "1", "LOW"      -> LOW;
            case "2", "MEDIUM"   -> MEDIUM;
            case "3", "HIGH"     -> HIGH;
            case "4", "CRITICAL" -> CRITICAL;
            default -> UNKNOWN;
        };
    }

    public static CostLevel fromCode(int c) {
        return switch (c) {
            case 0 -> LOWEST;
            case 1 -> LOW;
            case 2 -> MEDIUM;
            case 3 -> HIGH;
            case 4 -> CRITICAL;
            default -> UNKNOWN;
        };
    }
}
