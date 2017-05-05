package com.github.modelflat.coursework2.core;

import static java.lang.Math.abs;
import static java.lang.Math.signum;

/**
 * Created on 06.05.2017.
 */
public class EvolvableParameter {

    private double factor = 1. / 10.;
    private Strategy strategy;

    public enum Strategy {
        LINEAR_CYCLE, APPROACH_CYCLE
    }

    private double pointOfInterest;
    private double granularity;
    private boolean mode = true;
    private boolean switchOnNextPass = false;
    private double baseInc;

    private double value;
    private double inc;
    private double lower;
    private double upper;

    public EvolvableParameter(double value, double inc, double lower, double upper, double pointOfInterest, double granularity) {
        this.value = value;
        this.inc = inc;
        this.lower = lower;
        this.upper = upper;
        this.pointOfInterest = pointOfInterest;
        this.granularity = granularity;
        strategy = Strategy.APPROACH_CYCLE;
        this.baseInc = inc;
    }

    public EvolvableParameter(double value, double inc, double lower, double upper) {
        this.value = value;
        this.inc = inc;
        this.lower = lower;
        this.upper = upper;
        strategy = Strategy.LINEAR_CYCLE;
    }

    /**
     * upper = -lower
     */
    public EvolvableParameter(double value, double inc, double lower) {
        this(value, inc, lower, -lower);
    }

    public void evolve() {
        switch (strategy) {
            case LINEAR_CYCLE:
                evolveLinear();
                break;
            case APPROACH_CYCLE:
                evolveApproaching();
                break;
            default:
        }
    }

    private void evolveApproaching() {
        boolean approaching =
                (value < pointOfInterest && value + 2 * inc >= pointOfInterest) ||
                        (value > pointOfInterest && value + 2 * inc <= pointOfInterest);

        if (switchOnNextPass && approaching) {
            mode = !mode;
            switchOnNextPass = false;
        } else {
            if (mode) {
                if (approaching) {
                    if (abs(inc) < granularity) {
                        factor = 1. / factor;
                        switchOnNextPass = true;
                    } else {
                        inc = signum(inc) * abs(abs(value) - abs(pointOfInterest)) * factor;
                    }
                }
            } else {
                // TODO make inc restoration happens smoother
                inc = signum(inc) * abs(abs(baseInc) - abs(inc)) * factor;
                if (baseInc < inc) {
                    factor = 1. / factor;
                    inc = baseInc; // TODO fix leap
                    mode = !mode;
                }
            }
        }

        evolveLinear();
    }

    public void evolveLinear() {
        value += inc;
        if (value < lower) {
            value = lower;
            inc = -inc;
        }
        if (value > upper) {
            value = upper;
            inc = -inc;
        }
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getInc() {
        return inc;
    }

    public void setInc(double inc) {
        this.inc = inc;
    }

    public double getLower() {
        return lower;
    }

    public void setLower(double lower) {
        this.lower = lower;
    }

    public double getUpper() {
        return upper;
    }

    public void setUpper(double upper) {
        this.upper = upper;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EvolvableParameter{");
        sb.append("strategy=").append(strategy);
        sb.append(", factor=").append(factor);
        sb.append(", pointOfInterest=").append(pointOfInterest);
        sb.append(", granularity=").append(granularity);
        sb.append(", value=").append(value);
        sb.append(", inc=").append(inc);
        sb.append(", lower=").append(lower);
        sb.append(", upper=").append(upper);
        sb.append('}');
        return sb.toString();
    }
}
