package com.astreonix.watcher.ui.overlay;

import com.astreonix.watcher.config.TimerCorner;
import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.record.RecorderManager;
import com.astreonix.watcher.record.RecorderState;
import com.astreonix.watcher.ui.TimeFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class TimerOverlay {
    private TimerOverlay() {
    }

    public static void render(DrawContext context, RecorderManager recorder, WatcherConfig config) {
        if (!config.timerOverlayEnabled || recorder.state() == RecorderState.STOPPED) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String value = TimeFormatter.formatNanos(recorder.recordedNanos());
        String label = recorder.state() == RecorderState.PAUSED ? "PAUSED " + value : "REC " + value;
        int textWidth = client.textRenderer.getWidth(label);
        int x = x(config.timerCorner, context.getScaledWindowWidth(), textWidth + 12);
        int y = y(config.timerCorner, context.getScaledWindowHeight());
        int color = recorder.state() == RecorderState.RECORDING ? 0xFF5555 : 0xFFAA00;
        context.fill(x, y, x + textWidth + 12, y + 16, 0xB0000000);
        context.fill(x + 3, y + 5, x + 7, y + 9, color | 0xFF000000);
        context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 10, y + 4, 0xFFFFFF);
    }

    private static int x(TimerCorner corner, int width, int textWidth) {
        return switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> 8;
            case TOP_RIGHT, BOTTOM_RIGHT -> width - textWidth - 8;
        };
    }

    private static int y(TimerCorner corner, int height) {
        return switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> 8;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> height - 18;
        };
    }
}
