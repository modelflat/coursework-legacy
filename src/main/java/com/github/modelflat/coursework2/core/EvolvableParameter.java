package com.github.modelflat.coursework2.core;

import static java.lang.Math.abs;

/**
 * Created on 06.05.2017.
 */
public class EvolvableParameter {

    private EvolutionStrategy strategy;

    private boolean evolve;

    private double value;
    private double inc;
    private double lower;
    private double upper;

    private boolean dataHasChanged = false;

    public EvolvableParameter() {
    }

    public EvolvableParameter(boolean evolve, EvolutionStrategy strategy,
                              double value, double inc, double lower, double upper) {
        this.evolve = evolve;

        this.value = value;
        this.inc = inc;
        this.lower = lower;
        this.upper = upper;

        this.strategy = strategy;
        strategy.init(this);
    }

    public EvolvableParameter(boolean evolve,
                              double value, double inc, double lower, double upper) {
        this(evolve, new LinearEvolutionStrategy(), value, inc, lower, upper);
    }

    public EvolvableParameter(EvolutionStrategy strategy,
                              double value, double inc, double lower, double upper) {
        this(true, strategy, value, inc, lower, upper);
    }

    public boolean evolve() {
        if (!evolve && !dataHasChanged) {
            return false;
        }
        if (dataHasChanged) {
            strategy.init(this);
            dataHasChanged = false;
        }
        value = strategy.evolve(value, lower, upper);
        return true;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        dataHasChanged = true;
    }

    public double getInc() {
        return inc;
    }

    public void setInc(double inc) {
        this.inc = inc;
        dataHasChanged = true;
    }

    public double getLower() {
        return lower;
    }

    public void setLower(double lower) {
        this.lower = lower;
        dataHasChanged = true;
    }

    public double getUpper() {
        return upper;
    }

    public void setUpper(double upper) {
        this.upper = upper;
        dataHasChanged = true;
    }

    public boolean evolving() {
        return evolve;
    }

    public void setDoEvolve(boolean doEvolve) {
        this.evolve = doEvolve;
        dataHasChanged = true;
    }

    public void incValue(double increment) {
        value += increment;
        if (value < lower) value = lower;
        if (value > upper) value = upper;
        dataHasChanged = true;
    }

    /**
     * Increments value on abs(inc) * count, i.e. inc sign is overridden from outside.
     *
     * @param count inc count
     */
    public void incValue(int count) {
        incValue(abs(inc) * count);
    }

    public void copy(EvolvableParameter parameter) {
        this.value = parameter.value;
        this.lower = parameter.lower;
        this.upper = parameter.upper;
        this.inc = parameter.inc;
        this.strategy = parameter.strategy;

        dataHasChanged = true;
    }

    @Override
    public String toString() {
        return "EvolvableParameter{" + "strategy=" + strategy +
                ", evolve=" + evolve +
                ", value=" + value +
                ", inc=" + inc +
                ", lower=" + lower +
                ", upper=" + upper +
                ", dataHasChanged=" + dataHasChanged +
                '}';
    }
}
