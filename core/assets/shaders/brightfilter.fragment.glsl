// bright filter


uniform vec2 u_resolution;
uniform sampler2D u_texture;

varying vec2 v_texCoord0;


void main()
{
	vec4 color = texture2D(u_texture, v_texCoord0);

    float brightness = (color.r * 0.2126) + (color.r * 0.7152) + (color.b * 0.0722);
    if(brightness < 0.6)
        color = vec4(vec3(0), 1);
    
    gl_FragColor = color;
}
