package com.astreonix.watcher.ui;

import com.astreonix.watcher.config.TimerCorner;
import com.astreonix.watcher.config.WatcherConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final WatcherConfig config;

    public SettingsScreen(Screen parent, WatcherConfig config) {
        super(Text.translatable("screen.watcher.settings"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int x = width / 2 - 104;
        int y = height / 2 - 90;
        addDrawableChild(ButtonWidget.builder(Text.literal("FPS: " + config.frameRate), button -> {
            config.frameRate = config.frameRate == 30 ? 60 : 30;
            clearAndInit();
        }).dimensions(x, y, 204, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("CRF: " + config.crf), button -> {
            config.crf = config.crf >= 30 ? 16 : config.crf + 2;
            clearAndInit();
        }).dimensions(x, y + 24, 204, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("HUD: " + onOff(config.captureHud)), button -> {
            config.captureHud = !config.captureHud;
            clearAndInit();
        }).dimensions(x, y + 48, 204, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Timer: " + onOff(config.timerOverlayEnabled)), button -> {
            config.timerOverlayEnabled = !config.timerOverlayEnabled;
            clearAndInit();
        }).dimensions(x, y + 72, 204, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Timer Corner: " + config.timerCorner.name()), button -> {
            config.timerCorner = nextCorner(config.timerCorner);
            clearAndInit();
        }).dimensions(x, y + 96, 204, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Voice FX: " + config.voiceEffect), button -> {
            config.voiceEffect = nextEffect(config.voiceEffect);
            clearAndInit();
        }).dimensions(x, y + 120, 204, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent)).dimensions(width / 2 - 50, height - 32, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xE00B0F14);
        context.fill(width / 2 - 150, height / 2 - 122, width / 2 + 150, height / 2 + 88, 0xFF101820);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 108, 0xF4F7FA);
        super.render(context, mouseX, mouseY, delta);
    }

    private static String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    private static TimerCorner nextCorner(TimerCorner corner) {
        TimerCorner[] values = TimerCorner.values();
        return values[(corner.ordinal() + 1) % values.length];
    }

    private static String nextEffect(String effect) {
        return switch (effect) {
            case "none" -> "pitch_up";
            case "pitch_up" -> "deep";
            case "deep" -> "distortion";
            case "distortion" -> "robot";
            case "robot" -> "child";
            default -> "none";
        };
    }
}
