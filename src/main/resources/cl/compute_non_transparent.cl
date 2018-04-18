const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_NONE | CLK_FILTER_LINEAR;

kernel void compute(read_only image2d_t image, uint boxSize, global uint* counter) {
    int xid = get_global_id(0) * boxSize;
    int yid = get_global_id(1) * boxSize;
    int image_width = get_image_width(image);
    int image_height = get_image_height(image);

    if (xid > image_width - 1 || yid > image_height - 1) {
        return;
    }

    for (int x = xid; x < xid + boxSize; ++x) {
        for (int y = yid; y < yid + boxSize; ++y) {
            if (x > image_width - 1 || y > image_height - 1) {
                continue;
            }
            const float4 color = read_imagef(image, sampler, (int2)(x, y));
            if (color.w > 0.1) {
                atomic_inc( counter );
                return;
            }
        }
    }
}