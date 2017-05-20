const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_NONE | CLK_FILTER_LINEAR;

int scan_col(int x, int height, image2d_t image) {
    for (int2 coord = {x, 0}; coord.y < height; ++coord.y) {
        if (read_imagef(image, sampler, coord).w > 0.0) {
            return 1;
        }
    }
    return 0;
}

int scan_row(int y, int width, image2d_t image) {
    for (int2 coord = {0, y}; coord.x < width; ++coord.x) {
        if (read_imagef(image, sampler, coord).w > 0.0) {
            return 1;
        }
    }
    return 0;
}

kernel void compute_bounding_box(read_only image2d_t image, global int* bounding_box) {
    int id = get_global_id(0);

    if (id > 3) return;

    int mode = id % 4;
    int width = get_image_width(image);
    int height = get_image_height(image);

    // local int[width];

    // TODO optimize for work in local groups
    // int coord = get_local_id(0);

    switch (mode) {
        case 0:
            for (int coord = 0; coord < width / 2; ++coord) {
                if (scan_col(coord, height, image) == 1) {
                    bounding_box[0] = coord;
                    break;
                }
            }
            break;
        case 1:
            for (int coord = 0; coord < width / 2; ++coord) {
                if (scan_col(width - coord - 1, height, image) == 1) {
                    bounding_box[1] = width - coord - 1;
                    break;
                }
            }
            break;
        case 2:
            for (int coord = 0; coord < height / 2; ++coord) {
                if (scan_row(coord, width, image) == 1) {
                    bounding_box[2] = coord;
                    break;
                }
            }
            break;
        case 3:
            for (int coord = 0; coord < height / 2; ++coord) {
                if (scan_row(height - coord - 1, width, image) == 1) {
                    bounding_box[3] = height - coord - 1;
                    break;
                }
            }
            break;
        default:
            printf("[OCL] no way\n");
    }
}