package com.github.modelflat.coursework.core;

/**
 * Created on 09.05.2017.
 */
public interface EvolutionStrategy {

    void init(EvolvableParameter parameter);

    double evolve(double value, double lower, double upper);

}
