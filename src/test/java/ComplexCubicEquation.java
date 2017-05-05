

import org.apache.commons.math3.complex.Complex;

import java.util.List;

public class ComplexCubicEquation {

    public static class SolutionException extends RuntimeException {

        public SolutionException(Complex[] equation) {
            super(String.format("Equation x**3 + %sx**2 + %sx + %s = 0 cannot be solved! ",
                    equation[0].toString(), equation[1].toString(), equation[2].toString()));
        }

    }

    private Complex a, b, c;
    private double precision = 1e-8;
    private boolean preciseZero = false;

    private static final Complex eps1 = Complex.ONE.nthRoot(3).get(1);
    private static final Complex eps2 = Complex.ONE.nthRoot(3).get(2);

    public ComplexCubicEquation(Complex a, Complex b, Complex c, Complex d) {
        // transform equation into form y**3 + ay**2 + by + c = 0
        this.a = b.divide(a);
        this.b = c.divide(a);
        this.c = d.divide(a);
    }

    public void solve(Complex[] roots, boolean trace) {
        // prepare equation for solving, i.e. transform it into x**3 + px + q = 0
        Complex p = b.subtract(a.pow(2).divide(3));
        Complex q = c.add(a.pow(3).multiply(2.0/27.0)).subtract(a.multiply(b).divide(3));
        //
        Complex rootOfD = ((q.pow(2).divide(4)).add(p.pow(3).divide(27.0))).nthRoot(2).get(0);
        if (trace) {
            System.out.println("rootOfD" + rootOfD);
        }
        //
        List<Complex> alpha = (q.negate().divide(2)).subtract(rootOfD).nthRoot(3);
        if (trace) {
            System.out.println(alpha);
        }
        List<Complex> beta = (q.negate().divide(2)).add(rootOfD).nthRoot(3);
        if (trace) {
            System.out.println(beta);
        }
        //
        int bI = 0;
        boolean found = false;
        for (Complex betaIth : beta) {
            Complex r = alpha.get(0).multiply(betaIth).add(p.divide(3));
            if (trace) {
                System.out.println(r);
            }
            if ((preciseZero && r.equals(Complex.ZERO)) ||
                    (Math.abs(r.getReal()) < precision && Math.abs(r.getImaginary()) < precision)) {
                found = true;
                break;
            }
            bI++;
        }
        //
        if (!found) {
            throw new SolutionException(new Complex[] {a, b, c});
        }
        //
        roots[0] = alpha.get(0).add(beta.get(bI)).subtract(a.divide(3)); // alpha_1 + beta_1 - a/3
        roots[1] = alpha.get(1).add(beta.get(bI).multiply(eps2)).subtract(a.divide(3)); // alpha_2 + beta_3 - a/3
        roots[2] = alpha.get(2).add(beta.get(bI).multiply(eps1)).subtract(a.divide(3)); // alpha_3 + beta_2 - a/3
    }

    public void solve(Complex[] roots) {
        solve(roots, false);
    }

    public Complex getA() {
        return a;
    }

    public Complex getB() {
        return b;
    }

    public Complex getC() {
        return c;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public boolean isPreciseZero() {
        return preciseZero;
    }

    public void setPreciseZero(boolean preciseZero) {
        this.preciseZero = preciseZero;
    }

    @Override
    public String toString() {
        return String.format("(1, 0), %s, %s, %s", a, b, c);
    }
}
