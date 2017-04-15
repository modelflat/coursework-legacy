kernel void f(global real* points, global real* result) {
    int id = get_global_id(0) * 4;
    int out_id = get_global_id(0) * 2;

    real2 x = {points[id], points[id + 1]};
    real2 y = {points[id + 2], points[id + 3]};

    real2 r = x + y;

    result[out_id] = r.x;
    result[out_id+1] = r.y;
}
