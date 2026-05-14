package com.astreonix.watcher.audio;

final class RobotEffect implements AudioEffect {
    @Override
    public float[] process(float[] input, int sampleRate, int channels) {
        float[] output = new float[input.length];
        float carrierFrequency = 42.0f;
        for (int i = 0; i < input.length; i++) {
            float time = (i / Math.max(1.0f, channels)) / Math.max(1.0f, sampleRate);
            float carrier = (float) Math.sin(2.0f * Math.PI * carrierFrequency * time);
            output[i] = input[i] * carrier;
        }
        return output;
    }
}
