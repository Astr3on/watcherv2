package com.astreonix.watcher.config;

public record WatcherConfigSnapshot(
        int frameRate,
        int crf,
        String ffmpegPreset,
        boolean captureHud,
        boolean captureCursor,
        boolean timerOverlayEnabled,
        TimerCorner timerCorner,
        boolean captureGameAudio,
        boolean captureVoiceChat,
        String voiceEffect,
        String outputDirectory
) {
}
