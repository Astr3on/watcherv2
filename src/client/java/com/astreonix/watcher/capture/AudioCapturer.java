package com.astreonix.watcher.capture;

import com.astreonix.watcher.audio.AudioProcessor;
import com.astreonix.watcher.config.WatcherConfig;
import com.astreonix.watcher.record.AudioChunk;
import com.astreonix.watcher.record.AudioStreamType;
import com.astreonix.watcher.record.RecordingSession;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AudioCapturer {
    private final WatcherConfig config;
    private final AudioProcessor processor = new AudioProcessor();
    private final AtomicBoolean acceptingAudio = new AtomicBoolean(false);

    private volatile RecordingSession session;

    public AudioCapturer(WatcherConfig config) {
        this.config = config;
    }

    public void start(RecordingSession session) {
        this.session = session;
        processor.configure(config.voiceEffect);
        acceptingAudio.set(true);
    }

    public void pause() {
        acceptingAudio.set(false);
    }

    public void resume() {
        acceptingAudio.set(true);
    }

    public void stop() {
        acceptingAudio.set(false);
        session = null;
    }

    public void pushGameAudio(float[] samples, int sampleRate, int channels, long timestampNanos) {
        if (config.captureGameAudio) {
            push(samples, sampleRate, channels, timestampNanos, AudioStreamType.GAME);
        }
    }

    public void pushVoiceAudio(float[] samples, int sampleRate, int channels, long timestampNanos) {
        if (!config.captureVoiceChat) {
            return;
        }

        float[] processed = processor.process(samples, sampleRate, channels);
        push(processed, sampleRate, channels, timestampNanos, AudioStreamType.VOICE_CHAT);
    }

    public void shutdown() {
        stop();
    }

    private void push(float[] samples, int sampleRate, int channels, long timestampNanos, AudioStreamType type) {
        RecordingSession current = session;
        if (!acceptingAudio.get() || current == null || samples.length == 0) {
            return;
        }

        current.addAudio(new AudioChunk(timestampNanos, sampleRate, channels, samples.clone(), type));
    }
}
