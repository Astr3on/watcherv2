package com.astreonix.watcher.playback;

import com.astreonix.watcher.record.RecorderManager;
import com.astreonix.watcher.record.RecordingSession;
import java.util.Optional;

public final class PlaybackManager {
    private final RecorderManager recorder;
    private PlaybackState state = PlaybackState.STOPPED;
    private RecordingSession session;
    private long playbackStartedNanos;
    private long pausedAtNanos;

    public PlaybackManager(RecorderManager recorder) {
        this.recorder = recorder;
    }

    public PlaybackState state() {
        return state;
    }

    public Optional<RecordingSession> session() {
        return Optional.ofNullable(session);
    }

    public void play(RecordingSession session) {
        recorder.stop();
        this.session = session;
        this.playbackStartedNanos = System.nanoTime();
        this.pausedAtNanos = 0L;
        this.state = PlaybackState.PLAYING;
    }

    public void pause() {
        if (state == PlaybackState.PLAYING) {
            pausedAtNanos = elapsedNanos();
            state = PlaybackState.PAUSED;
        }
    }

    public void resume() {
        if (state == PlaybackState.PAUSED) {
            playbackStartedNanos = System.nanoTime() - pausedAtNanos;
            state = PlaybackState.PLAYING;
        }
    }

    public void stop() {
        state = PlaybackState.STOPPED;
        session = null;
        pausedAtNanos = 0L;
    }

    public long elapsedNanos() {
        if (state == PlaybackState.PAUSED) {
            return pausedAtNanos;
        }
        if (state == PlaybackState.PLAYING) {
            return System.nanoTime() - playbackStartedNanos;
        }
        return 0L;
    }
}
