package com.astreonix.watcher.record;

public record AudioChunk(long timestampNanos, int sampleRate, int channels, float[] samples, AudioStreamType streamType) {
}
