package com.github.modelflat.coursework2.core;

import static java.lang.Math.abs;
import static java.lang.Math.signum;

/**
 * Created on 09.05.2017.
 */
public class ApproachingEvolutionStrategy implements EvolutionStrategy {

    private double baseInc;
    private double inc;

    private double pointOfInterest;
    private double factor = 1. / 10.;
    private double granularity = 1e-12;

    private boolean mode = true;
    private boolean switchOnNextPass = false;

    public void setPointOfInterest(double v) {
        pointOfInterest = v;
    }

    public void setGranularity(double v) {
        granularity = v;
    }

    @Override
    public void init(EvolvableParameter parameter) {
        inc = baseInc = parameter.getInc();
    }

    @Override
    public double evolve(double value, double lower, double upper) {
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
                // TODO make inc restoration happen smoother
                inc = signum(inc) * abs(abs(baseInc) - abs(inc)) * factor;
                if (baseInc < inc) {
                    factor = 1. / factor;
                    inc = baseInc; // TODO fix leap
                    mode = !mode;
                }
            }
        }

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
