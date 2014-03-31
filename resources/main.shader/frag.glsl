#version 330

out vec4 fragment_color;
in vec2 tex_coord;
uniform sampler2D tex;
uniform float tex_scale;

void main() {
  fragment_color = texture(tex, tex_coord * tex_scale);//vec2(tex_coord.x * tex_scale, tex_coord.y * tex_scale));
  //fragment_color = vec4(tex_coord.x, tex_coord.y, 0.0, 1.0);
}
