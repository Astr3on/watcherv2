package com.astreonix.watcher.ui;

import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.encode.ExportState;
import com.astreonix.watcher.encode.ExportStatus;
import com.astreonix.watcher.playback.PlaybackManager;
import com.astreonix.watcher.record.RecorderManager;
import com.astreonix.watcher.record.RecordingSession;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class MainMenuRecordingsScreen extends Screen {
    private final Screen parent;
    private final RecorderManager recorder;
    private final PlaybackManager playback;
    private final WatcherConfig config;
    private RecordingSession selectedSession;

    public MainMenuRecordingsScreen(Screen parent, RecorderManager recorder, PlaybackManager playback, WatcherConfig config) {
        super(Text.literal("Watcher"));
        this.parent = parent;
        this.recorder = recorder;
        this.playback = playback;
        this.config = config;
    }

    @Override
    protected void init() {
        List<RecordingSession> sessions = recorder.sessions();
        if (selectedSession == null && !sessions.isEmpty()) {
            selectedSession = sessions.get(sessions.size() - 1);
        }

        int top = 18;
        int buttonWidth = 92;
        addDrawableChild(ButtonWidget.builder(Text.literal("Open Recording"), button -> openSelected()).dimensions(width - 340, top, 132, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Export"), button -> exportSelected()).dimensions(width - 198, top, buttonWidth, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> deleteSelected()).dimensions(width - 96, top, buttonWidth, 26).build());

        int listTop = 76;
        for (int i = 0; i < Math.min(8, sessions.size()); i++) {
            RecordingSession session = sessions.get(i);
            int y = listTop + 12 + i * 34;
            addDrawableChild(ButtonWidget.builder(Text.literal(shortId(session)), button -> {
                selectedSession = session;
                clearAndInit();
            }).dimensions(44, y, 108, 22).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent)).dimensions(width / 2 - 40, height - 32, 80, 22).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, 0x80000000);

        int margin = 14;
        int top = 18;
        drawSearchBox(context, margin, top, Math.min(310, width / 3), 26);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Watcher"), width / 2, top + 3, 0xF4F7FA);

        int panelLeft = margin;
        int panelTop = 58;
        int panelRight = width - margin;
        int panelBottom = height - 44;
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xB8121212);
        context.fill(panelLeft, panelTop, panelRight, panelTop + 2, 0xFFE8E8E8);
        context.fill(panelLeft, panelBottom - 2, panelRight, panelBottom, 0xFF5F5F5F);
        context.fill(panelLeft, panelTop, panelLeft + 2, panelBottom, 0xFFE8E8E8);
        context.fill(panelRight - 2, panelTop, panelRight, panelBottom, 0xFF5F5F5F);
        context.fill(panelLeft + 8, panelTop + 8, panelRight - 8, panelBottom - 8, 0xAA101214);

        drawSessions(context, recorder.sessions(), panelLeft + 20, panelTop + 18, panelRight - 20);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSearchBox(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xB8101010);
        context.fill(x, y, x + width, y + 2, 0xFFE8E8E8);
        context.fill(x, y + height - 2, x + width, y + height, 0xFF5F5F5F);
        context.fill(x, y, x + 2, y + height, 0xFFE8E8E8);
        context.fill(x + width - 2, y, x + width, y + height, 0xFF5F5F5F);
        context.drawTextWithShadow(textRenderer, Text.literal("Search Recordings"), x + 22, y + 9, 0x9CA3AF);
        context.drawTextWithShadow(textRenderer, Text.literal("O"), x + 7, y + 9, 0xC7CED6);
    }

    private void drawSessions(DrawContext context, List<RecordingSession> sessions, int x, int y, int right) {
        if (sessions.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No current-run recordings yet. Saved library loading comes next."), width / 2, height / 2, 0xC7CED6);
            return;
        }

        for (int i = 0; i < Math.min(8, sessions.size()); i++) {
            RecordingSession session = sessions.get(i);
            int rowY = y + i * 34;
            boolean selected = session == selectedSession;
            context.fill(x, rowY, right, rowY + 28, selected ? 0xCC2C3540 : 0x88202730);
            context.fill(x, rowY, x + 4, rowY + 28, selected ? 0xFFFF4A4A : 0xFF6B7280);
            context.drawTextWithShadow(textRenderer, Text.literal("Session " + shortId(session)), x + 118, rowY + 5, 0xF4F7FA);
            ExportStatus status = recorder.exportStatus(session);
            String detail = TimeFormatter.formatNanos(session.durationNanos()) + "  |  " + session.frames().size() + " frames  |  " + status.detail();
            context.drawTextWithShadow(textRenderer, Text.literal(detail), x + 118, rowY + 18, statusColor(status.state()));
        }
    }

    private void openSelected() {
        if (selectedSession != null) {
            client.setScreen(new PreviewScreen(this, playback, selectedSession));
        }
    }

    private void exportSelected() {
        if (selectedSession != null) {
            recorder.export(selectedSession, Path.of(config.outputDirectory, "watcher-" + selectedSession.id() + ".mp4"));
            clearAndInit();
        }
    }

    private void deleteSelected() {
        if (selectedSession != null) {
            recorder.delete(selectedSession);
            selectedSession = null;
            clearAndInit();
        }
    }

    private String shortId(RecordingSession session) {
        return session.id().toString().substring(0, 8);
    }

    private int statusColor(ExportState state) {
        return switch (state) {
            case COMPLETE -> 0xFF62D98F;
            case ENCODING, SAVING_SESSION, QUEUED -> 0xFF67E8F9;
            case FAILED -> 0xFFFF6B6B;
            case IDLE -> 0xFFC7CED6;
        };
    }
}
