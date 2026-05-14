package com.astreonix.watcher.ui;

public final class TimeFormatter {
    private TimeFormatter() {
    }

    public static String formatNanos(long nanos) {
        long totalSeconds = nanos / 1_000_000_000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
