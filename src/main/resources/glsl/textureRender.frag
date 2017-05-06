#version 400

in vec2 fragmentUV;

uniform sampler2D tex;

void main() {
    gl_FragColor = texture(tex, fragmentUV);
}