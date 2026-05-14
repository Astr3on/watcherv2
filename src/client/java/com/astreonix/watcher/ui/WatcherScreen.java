package com.astreonix.watcher.ui;

import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.playback.PlaybackManager;
import com.astreonix.watcher.record.RecorderManager;
import com.astreonix.watcher.record.RecorderState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class WatcherScreen extends Screen {
    private final Screen parent;
    private final RecorderManager recorder;
    private final PlaybackManager playback;
    private final WatcherConfig config;

    public WatcherScreen(Screen parent, RecorderManager recorder, PlaybackManager playback, WatcherConfig config) {
        super(Text.translatable("screen.watcher.title"));
        this.parent = parent;
        this.recorder = recorder;
        this.playback = playback;
        this.config = config;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int top = height / 2 - 52;

        addDrawableChild(ButtonWidget.builder(primaryActionText(), button -> primaryAction()).dimensions(centerX - 156, top, 148, 24).build());
        addDrawableChild(ButtonWidget.builder(pauseActionText(), button -> pauseAction()).dimensions(centerX + 8, top, 148, 24).build());
        addDrawableChild(ButtonWidget.builder(timerText(), button -> toggleTimer()).dimensions(centerX - 156, top + 36, 148, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.watcher.settings"), button -> client.setScreen(new SettingsScreen(this, config))).dimensions(centerX + 8, top + 36, 148, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back to Game"), button -> client.setScreen(parent)).dimensions(centerX - 74, top + 100, 148, 24).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xE00B0F14);
        context.fill(width / 2 - 190, 34, width / 2 + 190, 48, 0xFF1E252E);
        context.fill(width / 2 - 190, 48, width / 2 + 190, height - 42, 0xFF111820);
        context.fill(width / 2 - 190, 48, width / 2 - 184, height - 42, statusColor() | 0xFF000000);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("WATCHER"), width / 2, 58, 0xF4F7FA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Blueful capture workspace"), width / 2, 72, 0x94A3B8);
        drawStat(context, width / 2 - 148, height / 2 - 92, "State", recorder.state().name(), statusColor());
        drawStat(context, width / 2 - 46, height / 2 - 92, "Time", TimeFormatter.formatNanos(recorder.recordedNanos()), 0x3DD6D0);
        drawStat(context, width / 2 + 56, height / 2 - 92, "Frames", activeFrameCount(), 0xF4F7FA);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStat(DrawContext context, int x, int y, String label, String value, int valueColor) {
        context.fill(x, y, x + 92, y + 44, 0xFF18212B);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 8, y + 7, 0x8A98A8);
        context.drawTextWithShadow(textRenderer, Text.literal(value), x + 8, y + 24, valueColor);
    }

    private Text primaryActionText() {
        return recorder.state() == RecorderState.STOPPED ? Text.literal("Start") : Text.literal("Stop");
    }

    private Text pauseActionText() {
        return recorder.state() == RecorderState.PAUSED ? Text.literal("Resume") : Text.literal("Pause");
    }

    private Text timerText() {
        return Text.literal("Timer: " + (config.timerOverlayEnabled ? "On" : "Off"));
    }

    private void primaryAction() {
        if (recorder.state() == RecorderState.STOPPED) {
            recorder.start();
            client.setScreen(null);
            return;
        } else {
            recorder.stop();
        }
        clearAndInit();
    }

    private void pauseAction() {
        recorder.togglePause();
        clearAndInit();
    }

    private void toggleTimer() {
        config.timerOverlayEnabled = !config.timerOverlayEnabled;
        WatcherConfig.save(config);
        clearAndInit();
    }

    private Text statusText() {
        RecorderState state = recorder.state();
        return Text.literal("State: " + state.name() + "  Time: " + TimeFormatter.formatNanos(recorder.recordedNanos()));
    }

    private String activeFrameCount() {
        return recorder.activeSession().map(session -> Integer.toString(session.frames().size())).orElse(Integer.toString(recorder.sessions().stream().mapToInt(session -> session.frames().size()).sum()));
    }

    private int statusColor() {
        return switch (recorder.state()) {
            case RECORDING -> 0xFF5555;
            case PAUSED -> 0xFFAA00;
            case STOPPED -> 0xAAAAAA;
        };
    }
}
