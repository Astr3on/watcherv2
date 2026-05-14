package com.astreonix.watcher.config;

import com.astreonix.watcher.WatcherClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WatcherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("watcher.json");

    public int frameRate = 60;
    public int crf = 20;
    public String ffmpegPreset = "veryfast";
    public boolean captureHud = true;
    public boolean captureCursor = false;
    public boolean timerOverlayEnabled = true;
    public TimerCorner timerCorner = TimerCorner.TOP_LEFT;
    public boolean captureGameAudio = true;
    public boolean captureVoiceChat = true;
    public String voiceEffect = "none";
    public String ffmpegPath = "ffmpeg";
    public String outputDirectory = FabricLoader.getInstance().getGameDir().resolve("recordings").resolve("watcher").toString();

    public static WatcherConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new WatcherConfig();
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            WatcherConfig config = GSON.fromJson(reader, WatcherConfig.class);
            return config == null ? new WatcherConfig() : config.normalized();
        } catch (IOException exception) {
            WatcherClient.LOGGER.warn("Failed to load Watcher config, using defaults", exception);
            return new WatcherConfig();
        }
    }

    public static void save(WatcherConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config.normalized(), writer);
            }
        } catch (IOException exception) {
            WatcherClient.LOGGER.warn("Failed to save Watcher config", exception);
        }
    }

    public WatcherConfigSnapshot snapshot() {
        return new WatcherConfigSnapshot(frameRate, crf, ffmpegPreset, captureHud, captureCursor, timerOverlayEnabled, timerCorner, captureGameAudio, captureVoiceChat, voiceEffect, outputDirectory);
    }

    public WatcherConfig normalized() {
        frameRate = frameRate == 30 ? 30 : 60;
        crf = Math.max(0, Math.min(51, crf));
        if (ffmpegPreset == null || ffmpegPreset.isBlank()) {
            ffmpegPreset = "veryfast";
        }
        if (timerCorner == null) {
            timerCorner = TimerCorner.TOP_LEFT;
        }
        if (voiceEffect == null || voiceEffect.isBlank()) {
            voiceEffect = "none";
        }
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            ffmpegPath = "ffmpeg";
        }
        if (outputDirectory == null || outputDirectory.isBlank()) {
            outputDirectory = FabricLoader.getInstance().getGameDir().resolve("recordings").resolve("watcher").toString();
        }
        return this;
    }
}
