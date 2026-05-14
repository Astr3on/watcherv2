package com.astreonix.watcher.audio;

interface AudioEffect {
    float[] process(float[] input, int sampleRate, int channels);
}
