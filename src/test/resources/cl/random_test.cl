#include "random.clh"

kernel void f(ulong seed, const int count, global float* output) {
    int id = get_global_id(0);
    uint2 state;
    init_state(seed, &state);
    for (int i = 0; i < count; ++i) {
        float value = random(&state);
        output[id*count + i] = value;
        // printf("%d: %f\n", id, value);
    }
}