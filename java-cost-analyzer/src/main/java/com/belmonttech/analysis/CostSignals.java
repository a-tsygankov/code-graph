package com.belmonttech.analysis;

import java.util.Locale;

/**
 * Helper for cost-related checks (DB, REST, JDBC, RPC) with
 * case-insensitive pattern matching based on CodePatterns.
 */
public final class CostSignals {

    private CostSignals() {
    }

    private static boolean containsAny(String lowerText, Iterable<String> patterns) {
        for (String p : patterns) {
            if (lowerText.contains(p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDbAccess(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        // Mongo + JDBC are both "DB"
        return containsAny(lower, CodePatterns.MONGO_PATTERNS)
                || containsAny(lower, CodePatterns.JDBC_PATTERNS);
    }

    public static boolean hasRestAccess(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return containsAny(lower, CodePatterns.REST_PATTERNS);
    }

    public static boolean hasRpcAccess(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return containsAny(lower, CodePatterns.RPC_PATTERNS);
    }

    /**
     * Any external IO: DB, REST, RPC.
     */
    public static boolean hasExternalIo(String text) {
        return hasDbAccess(text) || hasRestAccess(text) || hasRpcAccess(text);
    }
}
