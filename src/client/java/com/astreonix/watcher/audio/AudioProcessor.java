package com.astreonix.watcher.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AudioProcessor {
    private final List<AudioEffect> effects = new ArrayList<>();

    public void configure(String effectName) {
        effects.clear();
        String normalized = effectName == null ? "none" : effectName.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "pitch_up" -> effects.add(new PitchShiftEffect(1.18f));
            case "pitch_down", "deep" -> effects.add(new PitchShiftEffect(0.82f));
            case "distortion" -> effects.add(new DistortionEffect(2.5f));
            case "robot" -> effects.add(new RobotEffect());
            case "child" -> effects.add(new PitchShiftEffect(1.35f));
            default -> {
            }
        }
    }

    public float[] process(float[] input, int sampleRate, int channels) {
        float[] buffer = input.clone();
        for (AudioEffect effect : effects) {
            buffer = effect.process(buffer, sampleRate, channels);
        }
        return buffer;
    }
}
