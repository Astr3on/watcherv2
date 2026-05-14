package com.astreonix.watcher.audio;

final class DistortionEffect implements AudioEffect {
    private final float drive;

    DistortionEffect(float drive) {
        this.drive = drive;
    }

    @Override
    public float[] process(float[] input, int sampleRate, int channels) {
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = Math.max(-1.0f, Math.min(1.0f, input[i] * drive));
        }
        return output;
    }
}
