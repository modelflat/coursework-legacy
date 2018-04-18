#version 400

in vec2 fragmentUV;
in vec2 texCoords;

uniform sampler2D tex;

const int base_x_deviation = 3;
const int base_y_deviation = base_x_deviation;

// factor determines which percent of transparent fragments are required to
// pass statistical test. for example: if both deviations are 3,
// then total_count = 48, and required_count = 4; i.e. if there are less than 4
// opaque (with respect to alpha factor) pixels in x +- 3, y +- 3 neighbourhood
// of point (x, y), then the texture color value for this point is discarded and
// replaced with clear_color.
const float factor = .1;
const float alpha_factor = .1;
const vec4 clear_color = vec4(1.0, 1.0, 1.0, 1.0);

void main() {
    vec4 texColor = texture(tex, fragmentUV);

    if (texColor.a > 0) {
        ivec2 size = textureSize(tex, 0);
        vec2 scale = vec2(1.0 / size.x, 1.0 / size.y);

        int total_count = (base_x_deviation*2 + 1)*(base_y_deviation*2 + 1) - 1;
        int required_count = int(factor * total_count);
        int non_transparent_count = 0;
        for (int i = -base_x_deviation; i <= base_x_deviation; ++i) {
            for (int j = -base_y_deviation; j <= base_y_deviation; ++j) {
                if (j == 0 && i == 0) {
                    continue;
                }
                if (texture(tex, vec2(fragmentUV.x + i*scale.x, fragmentUV.y + j*scale.y)).a >= alpha_factor) {
                    ++non_transparent_count;
                }
            }
        }

        if (non_transparent_count < required_count) {
            texColor = clear_color;
        }
    }

    gl_FragColor = texColor;
}