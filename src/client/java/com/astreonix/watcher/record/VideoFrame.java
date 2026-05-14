package com.astreonix.watcher.record;

public record VideoFrame(int index, long timestampNanos, int width, int height, int[] argbPixels) {
}
