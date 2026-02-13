package com.sshexecutor.util;

public final class OutputLimiter {

    private static final String TRUNCATION_MARKER = "[OUTPUT TRUNCATED]";

    private OutputLimiter() {
    }

    public static String limit(String output, int maxBytes) {
        if (output == null) {
            return "";
        }
        byte[] bytes = output.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return output;
        }
        String truncated = new String(bytes, 0, maxBytes, java.nio.charset.StandardCharsets.UTF_8);
        return truncated + "\n" + TRUNCATION_MARKER;
    }
}
