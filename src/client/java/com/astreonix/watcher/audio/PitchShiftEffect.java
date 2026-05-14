package com.astreonix.watcher.audio;

final class PitchShiftEffect implements AudioEffect {
    private final float ratio;

    PitchShiftEffect(float ratio) {
        this.ratio = ratio;
    }

    @Override
    public float[] process(float[] input, int sampleRate, int channels) {
        if (input.length == 0 || ratio == 1.0f) {
            return input;
        }

        float[] output = new float[input.length];
        for (int i = 0; i < output.length; i += Math.max(1, channels)) {
            float sourceFrame = i * ratio;
            int sourceIndex = Math.min(input.length - channels, Math.max(0, (int) sourceFrame));
            for (int channel = 0; channel < channels && i + channel < output.length; channel++) {
                output[i + channel] = input[Math.min(input.length - 1, sourceIndex + channel)];
            }
        }
        return output;
    }
}
