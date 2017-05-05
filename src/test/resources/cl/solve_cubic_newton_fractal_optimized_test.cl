#include "complex.clh"

kernel void f(global real* points, global real* result) {
    int id = get_global_id(0) * 4;
    int out_id = get_global_id(0) * 6;

    real2 a = {points[id], points[id + 1]};
    real2 c = {points[id + 2], points[id + 3]};
    real2 roots[3];

    #ifdef cl_khr_fp64
        const real precision = 1e-8;
    #else
        const real precision = 1e-4f;
    #endif

    if (solve_cubic_newton_fractal_optimized( a, c, precision, 0, &roots ) != 0) {
        printf("[OPENCL solve_cubic_newton_fractal_optimized] solution error for id %d (root 0)!\n", get_global_id(0));
    }
    if (solve_cubic_newton_fractal_optimized( a, c, precision, 1, &roots ) != 0) {
        printf("[OPENCL solve_cubic_newton_fractal_optimized] solution error for id %d (root 1)!\n", get_global_id(0));
    }
    if (solve_cubic_newton_fractal_optimized( a, c, precision, 2, &roots ) != 0) {
        printf("[OPENCL solve_cubic_newton_fractal_optimized] solution error for id %d (root 2)!\n", get_global_id(0));
    }

    result[out_id  ] = roots[0].x;
    result[out_id+1] = roots[0].y;
    result[out_id+2] = roots[1].x;
    result[out_id+3] = roots[1].y;
    result[out_id+4] = roots[2].x;
    result[out_id+5] = roots[2].y;
}