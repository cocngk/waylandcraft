#version 150

uniform mat4 transform;
in vec3 position;
in vec2 uv;

out vec2 texCoord;

void main() {
	gl_Position = transform * vec4(position, 1.0);
	texCoord = uv;
}