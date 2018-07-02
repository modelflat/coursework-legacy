package com.github.modelflat.coursework.core;

/**
 * Created on 09.05.2017.
 */
public class LinearEvolutionStrategy implements EvolutionStrategy {

    private double inc;

    @Override
    public void init(EvolvableParameter parameter) {
        inc = parameter.getInc();
    }

    @Override
    public double evolve(double value, double lower, double upper) {
        value += inc;
        if (value < lower) {
            inc = -inc;
            return lower;
        }
        if (value > upper) {
            inc = -inc;
            return upper;
        }
        return value;
    }
}
