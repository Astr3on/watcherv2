package com.astreonix.watcher.ui;

import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.encode.ExportState;
import com.astreonix.watcher.encode.ExportStatus;
import com.astreonix.watcher.playback.PlaybackManager;
import com.astreonix.watcher.record.RecorderManager;
import com.astreonix.watcher.record.RecordingSession;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.List;

public final class RecordingsScreen extends Screen {
    private final Screen parent;
    private final RecorderManager recorder;
    private final PlaybackManager playback;
    private final WatcherConfig config;

    public RecordingsScreen(Screen parent, RecorderManager recorder, PlaybackManager playback, WatcherConfig config) {
        super(Text.translatable("screen.watcher.recordings"));
        this.parent = parent;
        this.recorder = recorder;
        this.playback = playback;
        this.config = config;
    }

    @Override
    protected void init() {
        List<RecordingSession> sessions = recorder.sessions();
        int top = 70;
        for (int i = 0; i < Math.min(6, sessions.size()); i++) {
            RecordingSession session = sessions.get(i);
            int y = top + i * 42;
            addDrawableChild(ButtonWidget.builder(Text.literal("Preview"), button -> client.setScreen(new PreviewScreen(this, playback, session))).dimensions(width / 2 + 94, y + 10, 64, 20).build());
            addDrawableChild(ButtonWidget.builder(exportButtonText(session), button -> {
                recorder.export(session, defaultOutput(session));
                clearAndInit();
            }).dimensions(width / 2 + 164, y + 10, 64, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> {
                recorder.delete(session);
                clearAndInit();
            }).dimensions(width / 2 + 234, y + 10, 64, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> client.setScreen(parent)).dimensions(width / 2 - 50, height - 32, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xE00B0F14);
        context.fill(width / 2 - 330, 28, width / 2 + 330, height - 48, 0xFF101820);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("WATCHER RECORDINGS"), width / 2, 42, 0xF4F7FA);

        List<RecordingSession> sessions = recorder.sessions();
        if (sessions.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No recordings yet. Start a capture, stop with F9, then come back here."), width / 2, height / 2, 0x94A3B8);
        }

        for (int i = 0; i < Math.min(6, sessions.size()); i++) {
            RecordingSession session = sessions.get(i);
            int y = 70 + i * 42;
            context.fill(width / 2 - 304, y, width / 2 + 304, y + 36, 0xFF18212B);
            ExportStatus status = recorder.exportStatus(session);
            context.fill(width / 2 - 304, y, width / 2 - 298, y + 36, statusColor(status.state()));
            context.drawTextWithShadow(textRenderer, Text.literal("Session " + shortId(session)), width / 2 - 286, y + 6, 0xF4F7FA);
            String label = TimeFormatter.formatNanos(session.durationNanos()) + "  |  " + session.frames().size() + " frames  |  " + status.detail();
            context.drawTextWithShadow(textRenderer, Text.literal(label), width / 2 - 286, y + 21, 0x94A3B8);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private Path defaultOutput(RecordingSession session) {
        return Path.of(config.outputDirectory, "watcher-" + session.id() + ".mp4");
    }

    private String shortId(RecordingSession session) {
        return session.id().toString().substring(0, 8);
    }

    private Text exportButtonText(RecordingSession session) {
        ExportState state = recorder.exportStatus(session).state();
        if (state == ExportState.ENCODING || state == ExportState.SAVING_SESSION || state == ExportState.QUEUED) {
            return Text.literal("Busy");
        }
        if (state == ExportState.COMPLETE) {
            return Text.literal("Done");
        }
        return Text.literal("Export");
    }

    private int statusColor(ExportState state) {
        return switch (state) {
            case COMPLETE -> 0xFF2ED47A;
            case ENCODING, SAVING_SESSION, QUEUED -> 0xFF3DD6D0;
            case FAILED -> 0xFFFF4A4A;
            case IDLE -> 0xFFFFAA3D;
        };
    }
}
