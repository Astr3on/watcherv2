package com.astreonix.watcher.record;

import com.astreonix.watcher.config.WatcherConfigSnapshot;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class RecordingSession {
    private final UUID id;
    private final WatcherConfigSnapshot settings;
    private final Instant createdAt;
    private final Path directory;
    private final Queue<VideoFrame> frames = new ConcurrentLinkedQueue<>();
    private final Queue<AudioChunk> audioChunks = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final AtomicLong durationNanos = new AtomicLong(0L);

    private RecordingSession(UUID id, WatcherConfigSnapshot settings, Instant createdAt) {
        this.id = id;
        this.settings = settings;
        this.createdAt = createdAt;
        this.directory = Path.of(settings.outputDirectory(), id.toString());
    }

    public static RecordingSession create(WatcherConfigSnapshot settings, Instant createdAt) {
        return new RecordingSession(UUID.randomUUID(), settings, createdAt);
    }

    public UUID id() {
        return id;
    }

    public WatcherConfigSnapshot settings() {
        return settings;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Path directory() {
        return directory;
    }

    public Queue<VideoFrame> frames() {
        return frames;
    }

    public Queue<AudioChunk> audioChunks() {
        return audioChunks;
    }

    public boolean isComplete() {
        return complete.get();
    }

    public long durationNanos() {
        return durationNanos.get();
    }

    public void addFrame(VideoFrame frame) {
        if (!complete.get()) {
            frames.offer(frame);
        }
    }

    public void addAudio(AudioChunk chunk) {
        if (!complete.get()) {
            audioChunks.offer(chunk);
        }
    }

    public void complete(long recordedNanos) {
        durationNanos.set(recordedNanos);
        complete.set(true);
    }

    public void release() {
        frames.clear();
        audioChunks.clear();
    }
}
