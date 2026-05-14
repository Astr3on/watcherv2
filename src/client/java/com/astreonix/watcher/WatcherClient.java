package com.astreonix.watcher;

import com.astreonix.watcher.capture.AudioCapturer;
import com.astreonix.watcher.capture.FrameCapturer;
import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.encode.FFmpegEncoder;
import com.astreonix.watcher.playback.PlaybackManager;
import com.astreonix.watcher.record.RecorderManager;
import com.astreonix.watcher.ui.MainMenuRecordingsScreen;
import com.astreonix.watcher.ui.WatcherScreen;
import com.astreonix.watcher.ui.overlay.TimerOverlay;
import com.astreonix.watcher.util.KeybindHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WatcherClient implements ClientModInitializer {
    public static final String MOD_ID = "watcher";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static RecorderManager recorderManager;
    private static PlaybackManager playbackManager;
    private static WatcherConfig config;

    @Override
    public void onInitializeClient() {
        config = WatcherConfig.load();

        FrameCapturer frameCapturer = new FrameCapturer(config);
        AudioCapturer audioCapturer = new AudioCapturer(config);
        FFmpegEncoder encoder = new FFmpegEncoder(config);

        recorderManager = new RecorderManager(config, frameCapturer, audioCapturer, encoder);
        playbackManager = new PlaybackManager(recorderManager);

        KeybindHandler.register(recorderManager);
        registerPauseMenuButton();
        registerTitleRecordingsButton();
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> TimerOverlay.render(drawContext, recorderManager, config));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            recorderManager.shutdown();
            WatcherConfig.save(config);
        });

        LOGGER.info("Watcher initialized");
    }

    public static RecorderManager recorder() {
        return recorderManager;
    }

    public static PlaybackManager playback() {
        return playbackManager;
    }

    public static WatcherConfig config() {
        return config;
    }

    private static void registerPauseMenuButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof GameMenuScreen)) {
                return;
            }

            int x = scaledWidth / 2 - 102;
            int y = scaledHeight / 4 + 144;
            ButtonWidget button = ButtonWidget.builder(Text.literal("Watcher"), ignored -> {
                MinecraftClient.getInstance().setScreen(new WatcherScreen(screen, recorderManager, playbackManager, config));
            }).dimensions(x, y, 204, 20).build();
            Screens.getButtons(screen).add(button);
        });
    }

    private static void registerTitleRecordingsButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) {
                return;
            }

            ButtonWidget button = ButtonWidget.builder(Text.literal("W"), ignored -> {
                MinecraftClient.getInstance().setScreen(new MainMenuRecordingsScreen(screen, recorderManager, playbackManager, config));
            }).dimensions(4, scaledHeight - 46, 22, 22).build();
            Screens.getButtons(screen).add(button);
        });
    }
}
