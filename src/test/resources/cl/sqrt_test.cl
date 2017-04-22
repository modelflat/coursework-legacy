#include "complex.clh"

kernel void f(global real* points, global real* result) {
    int id = get_global_id(0) * 2;
    int out_id = get_global_id(0) * 4;

    real2 x = {points[id], points[id + 1]};
    real2 roots[2];

    csqrt(x, &roots); // take complex square root

    result[out_id  ] = roots[0].x;
    result[out_id+1] = roots[0].y;
    result[out_id+2] = roots[1].x;
    result[out_id+3] = roots[1].y;
}
