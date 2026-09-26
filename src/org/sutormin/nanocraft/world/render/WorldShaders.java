package org.sutormin.nanocraft.world.render;

public class WorldShaders {
    public static final String WORLD_VERTEX_SHADER = """
    #version 330 core

    layout (location = 0) in uvec4 aPosUv;  // gx, gy, gz, uv
    layout (location = 1) in uvec3 aAoTex;  // ao, texL, texH

    out vec3 TexCoord;
    out vec3 FragPosView;
    out float vAO;

    uniform mat4 uProjection;
    uniform mat4 uView;
    uniform vec2 uChunkOffset;

    void main() {
        // Positions are chunk-local at 1/128 block precision
        vec3 localPos = vec3(aPosUv.xyz) / 128.0;
        vec3 worldPos = localPos + vec3(uChunkOffset.x, 0.0, uChunkOffset.y);

        vec4 viewPos = uView * vec4(worldPos, 1.0);
        FragPosView = viewPos.xyz;
        gl_Position = uProjection * viewPos;

        // uv was packed as u * 129 + v, with u and v in 0..128
        uint u = aPosUv.w / 129u;
        uint v = aPosUv.w % 129u;

        // texture layer was split into two 16-bit halves
        uint layer = aAoTex.y | (aAoTex.z << 16u);

        TexCoord = vec3(float(u) / 128.0, float(v) / 128.0, float(layer));

        // ao was stored as brightness * 65535
        vAO = float(aAoTex.x) / 65535.0;
    }
""";
    public static final String WORLD_FRAGMENT_SHADER = """
    #version 330 core
    in vec3 TexCoord;
    in vec3 FragPosView;
    in float vAO;
    out vec4 FragColor;

    uniform sampler2DArray uTexture;
    //uniform vec3 uFogColor;
    //uniform float uFogNear;
    //uniform float uFogFar;

    void main() {
        vec4 texColor = texture(uTexture, TexCoord);
        //if (texColor.a < 0.1) discard; // buggy bc of mipmaps

        // fog (optional)
        //float dist = length(FragPosView);
        //float fogFactor = clamp((uFogFar - dist) / (uFogFar - uFogNear), 0.0, 1.0);
        //vec3 finalColor = mix(uFogColor, texColor.rgb * vAO, fogFactor);
        //FragColor = vec4(TexCoord.xy, 0.0, 1.0);
        //FragColor = vec4(1.0, 0.0, 1.0, 1.0);
        FragColor = vec4(texColor.rgb * vAO, texColor.a);
    }
  """;
}