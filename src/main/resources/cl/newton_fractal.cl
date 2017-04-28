#include "complex.clh"
#include "random.clh"

// Draws newton fractal
// seed - seed value for pRNG; see "random.clh"
kernel void newton_fractal(
    float min_x, float max_x, float min_y, float max_y,
    global float* C_const,
    float t,
    uint runs_count,
    uint points_count,
    ulong seed,
    write_only image2d_t out_image)
{
    // const C
    const real2 C = {C_const[0], C_const[1]};
    // color
    const float4 color = {.5 + t / 2, fabs(sin(PI * t / 3)), .5 - t / 2, 1.0};
    // initialize pRNG
    uint2 rng_state;
    init_state(seed, &rng_state);
    // spans, scales, sizes
    float span_x = max_x - min_x;
    float span_y = max_y - min_y;
    int image_width = get_image_width(out_image);
    int image_height = get_image_height(out_image);
    float scale_x = span_x / image_width;
    float scale_y = span_y / image_height;
    int2 coord;
    // for each run
    real2 roots[3];
    real2 a;
    const real2 b = {0, 0};
    const real2 c = C * (-t) / (3 - t);
    const real a_modifier = (-3) / (3 - t);
//////////////////////
        if (get_global_id(0) == 0) {
            printf("[OCL INFO] bounds = (%f,%f,%f,%f); c = (%.2v2f); t = %f; runs = %dx%d; img_size = %dx%d\n",
                min_x, max_x, min_y, max_y, C, t, runs_count, points_count, image_width, image_height
            );
        }
/////////////////////
    for (int run = 0; run < runs_count; ++run) {
        // choose starting point
        real2 starting_point = {
            random(&rng_state) * span_x + min_x,
            random(&rng_state) * span_y + min_y
        };

        int frozen = 0;
        // iterate through solutions of cubic equation
        for (int i = 0; i < points_count; ++i) {
            // compute next point:
            a = starting_point * a_modifier;
            uint root_number = as_uint(random(&rng_state)) % 3;
            solve_cubic(a, b, c, 1e-5, root_number, &roots);
            starting_point = roots[root_number];

            // transform coords:
            coord.x = (starting_point.x - min_x) / scale_x;
            coord.y = image_height - 1 - (int)((starting_point.y - min_y) / scale_y);

            // draw next point:
            if (coord.x < image_width && coord.y < image_height && coord.x >= 0 && coord.y >= 0) {
                write_imagef(out_image, coord, color);
                frozen = 0;
            } else {
                if (++frozen > 15) {
                    printf("[OCL] error at slave %d: frozen!\n", get_global_id(0));
                    break;
                }
            }
        }
    }
}