package com.astreonix.watcher.ui;

import com.astreonix.watcher.playback.PlaybackManager;
import com.astreonix.watcher.playback.PlaybackState;
import com.astreonix.watcher.record.RecordingSession;
import com.astreonix.watcher.record.VideoFrame;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class PreviewScreen extends Screen {
    private static final int SAMPLE_COLUMNS = 360;

    private final Screen parent;
    private final PlaybackManager playback;
    private final RecordingSession session;
    private final List<VideoFrame> frames;
    private final long firstFrameTimestampNanos;

    public PreviewScreen(Screen parent, PlaybackManager playback, RecordingSession session) {
        super(Text.literal("Watcher Preview"));
        this.parent = parent;
        this.playback = playback;
        this.session = session;
        this.frames = new ArrayList<>(session.frames());
        this.firstFrameTimestampNanos = frames.isEmpty() ? 0L : frames.getFirst().timestampNanos();
    }

    @Override
    protected void init() {
        playback.play(session);
        int y = height - 34;
        addDrawableChild(ButtonWidget.builder(Text.literal("Play/Pause"), button -> togglePlayback()).dimensions(width / 2 - 150, y, 96, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Restart"), button -> playback.play(session)).dimensions(width / 2 - 46, y, 92, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            playback.stop();
            client.setScreen(parent);
        }).dimensions(width / 2 + 54, y, 96, 22).build());
    }

    @Override
    public void removed() {
        playback.stop();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF0080C10);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("WATCHER PREVIEW"), width / 2, 18, 0xF4F7FA);

        if (frames.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("This session has no captured frames."), width / 2, height / 2, 0xFF4A4A);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int frameIndex = currentFrameIndex();
        VideoFrame frame = frames.get(frameIndex);
        int previewWidth = Math.min(width - 80, 720);
        int previewHeight = previewWidth * frame.height() / Math.max(1, frame.width());
        if (previewHeight > height - 112) {
            previewHeight = height - 112;
            previewWidth = previewHeight * frame.width() / Math.max(1, frame.height());
        }

        int x = (width - previewWidth) / 2;
        int y = 44;
        context.fill(x - 3, y - 3, x + previewWidth + 3, y + previewHeight + 3, 0xFF26313D);
        drawSampledFrame(context, frame, x, y, previewWidth, previewHeight);

        String status = playback.state().name() + "  |  " + TimeFormatter.formatNanos(playback.elapsedNanos()) + " / " + TimeFormatter.formatNanos(session.durationNanos()) + "  |  " + (frameIndex + 1) + "/" + frames.size();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, y + previewHeight + 12, 0x94A3B8);
        super.render(context, mouseX, mouseY, delta);
    }

    private int currentFrameIndex() {
        long elapsedNanos = Math.min(playback.elapsedNanos(), Math.max(0L, session.durationNanos()));
        long targetTimestamp = firstFrameTimestampNanos + elapsedNanos;
        int low = 0;
        int high = frames.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            long timestamp = frames.get(middle).timestampNanos();
            if (timestamp <= targetTimestamp) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return Math.max(0, Math.min(frames.size() - 1, high));
    }

    private void togglePlayback() {
        if (playback.state() == PlaybackState.PLAYING) {
            playback.pause();
        } else if (playback.state() == PlaybackState.PAUSED) {
            playback.resume();
        } else {
            playback.play(session);
        }
    }

    private void drawSampledFrame(DrawContext context, VideoFrame frame, int x, int y, int previewWidth, int previewHeight) {
        int columns = Math.min(SAMPLE_COLUMNS, Math.min(previewWidth, frame.width()));
        int rows = Math.max(1, columns * frame.height() / Math.max(1, frame.width()));
        int cellWidth = Math.max(1, (int) Math.ceil(previewWidth / (double) columns));
        int cellHeight = Math.max(1, (int) Math.ceil(previewHeight / (double) rows));
        for (int row = 0; row < rows; row++) {
            int sourceY = Math.min(frame.height() - 1, row * frame.height() / rows);
            for (int column = 0; column < columns; column++) {
                int sourceX = Math.min(frame.width() - 1, column * frame.width() / columns);
                int color = frame.argbPixels()[sourceY * frame.width() + sourceX] | 0xFF000000;
                int left = x + column * previewWidth / columns;
                int top = y + row * previewHeight / rows;
                context.fill(left, top, Math.min(x + previewWidth, left + cellWidth), Math.min(y + previewHeight, top + cellHeight), color);
            }
        }
    }
}
