#define real  double
#define real2 double2
#define PI M_PI

#define COMPL_ONE_2nd_ROOT_REAL -0.5
#define COMPL_ONE_2nd_ROOT_IMAG 0.8660254037844385965883020
#define COMPL_ONE_3rd_ROOT_REAL COMPL_ONE_2nd_ROOT_REAL
#define COMPL_ONE_3rd_ROOT_IMAG -COMPL_ONE_2nd_ROOT_IMAG

// const real COMPL_ONE_3rd_ROOT_IMAG = sin(PI / 3);

real2 cdiv(real2 a, real2 b) {
    real sq = b.x*b.x + b.y*b.y;
    real2 result = {(a.x*b.x + a.y*b.y) / sq, (a.y*b.x - a.x*b.y) / sq};
    return result;
}

void csqrt(real2 a, real2* roots) {
    roots[0].x = sqrt((  a.x + length(a)) / 2);
    roots[0].y = sign(a.y) * sqrt(( -a.x + length(a)) / 2);
    roots[1].x = -roots[0].x;
    roots[1].y = -roots[0].y;
}

void ccbrt(real2 a, real2* roots) {
    real cbrt_abs_a = cbrt(length(a));
    real phi = atan2(a.y, a.x) / 3.0;
    real temp = 0;
    roots[0].y = sincos(phi, &temp) * cbrt_abs_a;
    roots[0].x = temp * cbrt_abs_a;
    roots[1].y = sincos(phi + 2*PI/3.0, &temp) * cbrt_abs_a;
    roots[1].x = temp * cbrt_abs_a;
    roots[2].y = sincos(phi + 4*PI/3.0, &temp) * cbrt_abs_a;
    roots[2].x = temp * cbrt_abs_a;
}

// uses cardano's formulas to solve cubic equation given in form: z**3 + a*z**2 + b*z + c = 0
// params:
// a, b, c is the coefficients
// precision - is the required precision of floating points comparisons
// root - which (0, 1, 2) root to compute; -1 if want all roots
// roots - [out] computed roots; if only one root is specified,
// returns 0 if equation was successfully solved, 1 otherwise
int solve_cubic(real2 a, real2 b, real2 c, real precision, int root, real2* roots) {
    real2 p = {
        b.x - (a.x*a.x - a.y*a.y) / 3,
        b.y - a.x*a.y * (2.0 / 3)
    };
    real2 q = {
        c.x + a.x * (a.x*a.x - 3*a.y*a.y) * (2.0 / 27) - (a.x*b.x - a.y*b.y) / 3,
        c.y + a.y * (3*a.x*a.x - a.y*a.y) * (2.0 / 27) - (a.y*b.x + a.x*b.y) / 3
    };
    real2 D = {
        (q.x*q.x - q.y*q.y) / 4 + p.x * (p.x*p.x - 3*p.y*p.y) / 27,
        q.x*q.y / 2 + p.y * (3*p.x*p.x - p.y*p.y) / 27
    };
    real modulus = length(D);
    real2 first_root_of_D = {
        sqrt((D.x + modulus) / 2),
        sign(D.y) * sqrt((modulus - D.x) / 2)
    };
    real2 base_alpha = {
        -q.x / 2 - first_root_of_D.x,
        -q.y / 2 - first_root_of_D.y
    };
//    printf("base alpha: %.16v2f\n", base_alpha);
    real2 alpha[3];
    ccbrt(base_alpha, &alpha);
    real2 base_beta = {
        -q.x / 2 + first_root_of_D.x,
        -q.y / 2 + first_root_of_D.y
    };
    real2 beta[3];
    ccbrt(base_beta, &beta);
    // now we have alpha & beta values, lets combine them such as:
    // alpha[0]*beta[i] = -p / 3;
    int idx = -1;
    for (int i = 0; i < 3; ++i) {
//        printf("alpha %d: %.16v2f\n", i, alpha[i]);
        if (fabs( alpha[0].x*beta[i].x - alpha[0].y*beta[i].y + p.x / 3 ) < precision &&
            fabs( alpha[0].y*beta[i].x + alpha[0].x*beta[i].y + p.y / 3 ) < precision) {
            idx = i;
            break;
        }
    }
    if (idx == -1) {
        // cannot find b, such that alpha*beta = p/3 (although it should always exists);
        // maybe floating point error is greater than precision, but better not to risk and report error
        return 1;
    }
    // now corresponding beta is found for alpha[0]. other betas can be inferred from it
    if (root <= 0) {
        // alpha_1 + beta_1 - a/3
        roots[0] = alpha[0] + beta[idx] - a / 3;
    }
    if (root == 1 || root == -1) {
        // alpha_2 + beta_3 - a/3
        roots[1].x = alpha[1].x - a.x / 3 +
            COMPL_ONE_3rd_ROOT_REAL*beta[idx].x - COMPL_ONE_3rd_ROOT_IMAG*beta[idx].y;
        roots[1].y = alpha[1].y - a.y / 3 +
            COMPL_ONE_3rd_ROOT_REAL*beta[idx].y + COMPL_ONE_3rd_ROOT_IMAG*beta[idx].x;
    }
    if (root == 2 || root == -1) {
        // alpha_3 + beta_2 - a/3
        roots[2].x = alpha[2].x - a.x / 3 +
            COMPL_ONE_2nd_ROOT_REAL*beta[idx].x - COMPL_ONE_2nd_ROOT_IMAG*beta[idx].y;
        roots[2].y = alpha[2].y - a.y / 3 +
            COMPL_ONE_2nd_ROOT_REAL*beta[idx].y + COMPL_ONE_2nd_ROOT_IMAG*beta[idx].x;
    }
    return 0;
}