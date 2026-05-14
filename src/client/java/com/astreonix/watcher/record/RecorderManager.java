package com.astreonix.watcher.record;

import com.astreonix.watcher.capture.AudioCapturer;
import com.astreonix.watcher.capture.FrameCapturer;
import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.encode.FFmpegEncoder;
import com.astreonix.watcher.encode.ExportStatus;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class RecorderManager {
    private final WatcherConfig config;
    private final FrameCapturer frameCapturer;
    private final AudioCapturer audioCapturer;
    private final FFmpegEncoder encoder;
    private final AtomicReference<RecorderState> state = new AtomicReference<>(RecorderState.STOPPED);
    private final List<RecordingSession> sessions = new CopyOnWriteArrayList<>();

    private volatile RecordingSession activeSession;
    private volatile long resumeStartedNanos;
    private volatile long accumulatedRecordedNanos;

    public RecorderManager(WatcherConfig config, FrameCapturer frameCapturer, AudioCapturer audioCapturer, FFmpegEncoder encoder) {
        this.config = config;
        this.frameCapturer = frameCapturer;
        this.audioCapturer = audioCapturer;
        this.encoder = encoder;
    }

    public RecorderState state() {
        return state.get();
    }

    public Optional<RecordingSession> activeSession() {
        return Optional.ofNullable(activeSession);
    }

    public List<RecordingSession> sessions() {
        return Collections.unmodifiableList(new ArrayList<>(sessions));
    }

    public boolean start() {
        if (!state.compareAndSet(RecorderState.STOPPED, RecorderState.RECORDING)) {
            return false;
        }

        RecordingSession session = RecordingSession.create(config.snapshot(), Instant.now());
        activeSession = session;
        accumulatedRecordedNanos = 0L;
        resumeStartedNanos = System.nanoTime();
        frameCapturer.start(session);
        audioCapturer.start(session);
        return true;
    }

    public boolean pause() {
        if (!state.compareAndSet(RecorderState.RECORDING, RecorderState.PAUSED)) {
            return false;
        }

        accumulatedRecordedNanos += System.nanoTime() - resumeStartedNanos;
        frameCapturer.pause();
        audioCapturer.pause();
        return true;
    }

    public boolean resume() {
        if (!state.compareAndSet(RecorderState.PAUSED, RecorderState.RECORDING)) {
            return false;
        }

        resumeStartedNanos = System.nanoTime();
        frameCapturer.resume();
        audioCapturer.resume();
        return true;
    }

    public boolean stop() {
        RecorderState previous = state.getAndSet(RecorderState.STOPPED);
        if (previous == RecorderState.STOPPED) {
            return false;
        }

        if (previous == RecorderState.RECORDING) {
            accumulatedRecordedNanos += System.nanoTime() - resumeStartedNanos;
        }

        frameCapturer.stop();
        audioCapturer.stop();

        RecordingSession finished = activeSession;
        if (finished != null) {
            finished.complete(accumulatedRecordedNanos);
            sessions.add(finished);
            encoder.persistSession(finished);
        }

        activeSession = null;
        return true;
    }

    public void toggleRecording() {
        if (state() == RecorderState.STOPPED) {
            start();
        } else {
            stop();
        }
    }

    public void togglePause() {
        if (state() == RecorderState.RECORDING) {
            pause();
        } else if (state() == RecorderState.PAUSED) {
            resume();
        }
    }

    public long recordedNanos() {
        if (state() == RecorderState.RECORDING) {
            return accumulatedRecordedNanos + System.nanoTime() - resumeStartedNanos;
        }
        return accumulatedRecordedNanos;
    }

    public void captureRenderedFrame(int width, int height) {
        if (state() == RecorderState.RECORDING) {
            frameCapturer.captureFinalFramebuffer(width, height, System.nanoTime());
        }
    }

    public void export(RecordingSession session, Path outputFile) {
        encoder.export(session, outputFile);
    }

    public ExportStatus exportStatus(RecordingSession session) {
        return encoder.status(session);
    }

    public void delete(RecordingSession session) {
        sessions.remove(session);
        session.release();
    }

    public void shutdown() {
        stop();
        frameCapturer.shutdown();
        audioCapturer.shutdown();
        encoder.shutdown();
    }
}
