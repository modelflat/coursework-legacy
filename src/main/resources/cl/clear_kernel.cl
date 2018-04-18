#define COLOR (1.0, 1.0, 1.0, 0.0)

kernel void clear(write_only image2d_t image) {
    write_imagef(image, (int2)(get_global_id(0), get_global_id(1)), (float4)COLOR);
}