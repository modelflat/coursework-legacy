#version 400

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 vertexUV;

out vec2 fragmentUV;
out vec2 texCoords;

void main() {
    gl_Position = vec4(position, 1.0, 1.0);
    fragmentUV = vertexUV;
    texCoords = vec2(gl_MultiTexCoord0);
}