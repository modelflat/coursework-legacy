package com.github.modelflat.coursework.core;

import static java.lang.Math.abs;
import static java.lang.Math.signum;

/**
 * Created on 09.05.2017.
 */
public class ApproachingEvolutionStrategy implements EvolutionStrategy {

    private double baseInc = 0.0;
    private double inc = 0.0;

    private double pointOfInterest;
    private double factor = 1. / 100.;
    private double granularity = 1e-12;

    private boolean mode = true;
    private boolean switchOnNextPass = false;
    private boolean stopped = false;

    private InternalStrategy strategy;

    public ApproachingEvolutionStrategy(double pointOfInterest) {
        this(InternalStrategy.CYCLE, pointOfInterest);
    }

    public ApproachingEvolutionStrategy(InternalStrategy strategy, double pointOfInterest) {
        this.strategy = strategy;
        this.pointOfInterest = pointOfInterest;
    }

    public void setPointOfInterest(double v) {
        pointOfInterest = v;
    }

    public void setGranularity(double v) {
        granularity = v;
    }

    @Override
    public void init(EvolvableParameter parameter) {
        double t = parameter.getInc();
        if (inc == baseInc) {
            inc = baseInc = t;
        }
        baseInc = t;
    }

    @Override
    public double evolve(double value, double lower, double upper) {
        if (stopped) {
            if (strategy == InternalStrategy.TERMINATE) {
                System.exit(0);
            }
            return value;
        }

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

                    if (strategy == InternalStrategy.TERMINATE) {
                        stopped = true;
                    }
                }
            } else {
                switch (strategy) {
                    case CYCLE:
                        // TODO make inc restoration happen smoother
                        inc = signum(inc) * abs(abs(baseInc) - abs(inc)) * factor;
                        if (baseInc < inc) {
                            factor = 1. / factor;
                            inc = baseInc; // TODO fix leap
                            mode = !mode;
                        }
                        break;
                    case STOP_AT_POINT_OF_INTEREST:
                    case TERMINATE:
                        stopped = true;
                    default:
                        // do nothing
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

    public enum InternalStrategy {STOP_AT_POINT_OF_INTEREST, CYCLE, TERMINATE}
}
