#include "complex.clh"
#include "random.clh"

#ifdef cl_khr_fp64
    #define PRECISION 1e-9
#else
    #define PRECISION 1e-4
#endif

#define DYNAMIC_COLOR 1

// Draws newton fractal
kernel void newton_fractal(
    // plane bounds
    real min_x, real max_x, real min_y, real max_y,
    // fractal parameters
    global real* C_const, real t,
    // how many initial points select
    uint runs_count,
    // how many times solve equation for certain initial point
    uint points_count,
    // how many initial steps will be skipped
    uint iter_skip,
    // seed - seed value for pRNG; see "random.clh"
    ulong seed,
    // image buffer for output
    write_only image2d_t out_image)
{
    // const C
    const real2 C = {C_const[0], C_const[1]};
    // color
    #if (DYNAMIC_COLOR)
        float4 color = {1.0, 1.0, 1.0, 1.0};//{fabs(sin(PI * t / 3.)), fabs(cos(PI * t / 3.)), 0.0, 0.0};
    #else
        float4 color = {1.0, 1.0, 1.0, 1.0};
    #endif
    // initialize pRNG
    uint2 rng_state;
    init_state(seed, &rng_state);
    // spans, scales, sizes
    real span_x = max_x - min_x;
    real span_y = max_y - min_y;
    int image_width = get_image_width(out_image);
    int image_height = get_image_height(out_image);
    real scale_x = span_x / image_width;
    real scale_y = span_y / image_height;
    int2 coord;
    // for each run
    real2 roots[3];
    real2 a;
    const real2 c = C * (-t) / (3 - t);
    const real a_modifier = (-3) / (3 - t);
    const real color_alpha_increment = (real)(1.0 / (points_count + 1));
    // TODO run count was proved to be inefficient. remove?
    for (int run = 0; run < runs_count; ++run) {
        // choose starting point
        real2 starting_point = {
            ((random(&rng_state)) * span_x + min_x) / 2,
            ((random(&rng_state)) * span_y + min_y) / 2
        };
        uint is = iter_skip;
        int frozen = 0;

        #if (DYNAMIC_COLOR)
            //color.w = color_alpha_increment;
        #endif

        // iterate through solutions of cubic equation
        for (int i = 0; i < points_count; ++i) {
            // compute next point:
            a = starting_point * a_modifier;
            uint root_number = (as_uint(random(&rng_state)) >> 7) % 3;
            solve_cubic_newton_fractal_optimized(a, c, 1e-8, root_number, roots);
            starting_point = roots[root_number];

            // the first iter_skip points will  be skipped
            if (is == 0) {
                // transform coords:
                coord.x = (starting_point.x - min_x) / scale_x;
                coord.y = image_height - 1 - (int)((starting_point.y - min_y) / scale_y);

                // draw next point:
                #if (DYNAMIC_COLOR)
                    color.yz -= color_alpha_increment;
                #endif
                if (coord.x < image_width && coord.y < image_height && coord.x >= 0 && coord.y >= 0) {
                    write_imagef(out_image, coord, color);
                    frozen = 0;
                } else {
                    if (++frozen > 15) {
                        // this generally means that solution is going to approach infinity
                        //printf("[OCL] error at slave %d: frozen!\n", get_global_id(0));
                        break;
                    }
                }
            } else {
                --is;
            }
        }
    }
}