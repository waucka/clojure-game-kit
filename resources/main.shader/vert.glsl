#version 330

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;
in vec3 Position;
in vec2 in_tex_coord;
out vec2 tex_coord;

void main() {
  vec4 pos=vec4(Position.xyz, 1.0);
  gl_Position = proj * view * model * pos;
  tex_coord = in_tex_coord;
}
