package com.inoek.apps.fullycloakedtv_free_version;

/**
 * Created by harshasanthosh on 26/06/17.
 */

public class AVInterpolator implements android.view.animation.Interpolator {
    private double mAmplitude = 1;
    private double mFrequency = 10;

    public AVInterpolator(double amplitude, double frequency) {
        mAmplitude = amplitude;
        mFrequency = frequency;
    }

    @Override
    public float getInterpolation(float input) {
        return (float) (-1 * Math.pow(Math.E, -input/ mAmplitude) *
                Math.cos(mFrequency * input) + 1);
    }
}
