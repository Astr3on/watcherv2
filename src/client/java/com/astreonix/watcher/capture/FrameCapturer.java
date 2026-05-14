package com.astreonix.watcher.capture;

import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.record.RecordingSession;
import com.astreonix.watcher.record.VideoFrame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class FrameCapturer {
    private final WatcherConfig config;
    private final AtomicBoolean acceptingFrames = new AtomicBoolean(false);

    private volatile RecordingSession session;
    private volatile long nextFrameNanos;
    private volatile AtomicInteger frameCounter = new AtomicInteger();

    public FrameCapturer(WatcherConfig config) {
        this.config = config;
    }

    public void start(RecordingSession session) {
        this.session = session;
        this.nextFrameNanos = 0L;
        this.frameCounter = new AtomicInteger();
        acceptingFrames.set(true);
    }

    public void pause() {
        acceptingFrames.set(false);
    }

    public void resume() {
        nextFrameNanos = 0L;
        acceptingFrames.set(true);
    }

    public void stop() {
        acceptingFrames.set(false);
        session = null;
    }

    public void captureFinalFramebuffer(int width, int height, long timestampNanos) {
        RecordingSession current = session;
        if (!acceptingFrames.get() || current == null || !shouldCapture(timestampNanos)) {
            return;
        }

        int frameIndex = frameCounter.incrementAndGet();
        ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer(), image -> addScreenshotFrame(current, image, frameIndex, timestampNanos));
    }

    public void shutdown() {
        stop();
    }

    private void addScreenshotFrame(RecordingSession current, NativeImage image, int frameIndex, long timestampNanos) {
        try {
            current.addFrame(new VideoFrame(frameIndex, timestampNanos, image.getWidth(), image.getHeight(), image.copyPixelsArgb()));
        } finally {
            image.close();
        }
    }

    private boolean shouldCapture(long timestampNanos) {
        long frameIntervalNanos = 1_000_000_000L / Math.max(1, config.frameRate);
        if (nextFrameNanos == 0L || timestampNanos >= nextFrameNanos) {
            nextFrameNanos = timestampNanos + frameIntervalNanos;
            return true;
        }
        return false;
    }
}
