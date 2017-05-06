kernel void clear(write_only image2d_t image) {
    float4 color = {0.0, 0.0, 0.0, 0.0};
    write_imagef(image, (int2)(get_global_id(0), get_global_id(1)), color);
}