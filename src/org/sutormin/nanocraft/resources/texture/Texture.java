package org.sutormin.nanocraft.resources.texture;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.sutormin.nanocraft.Main;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {
    private int texSize = 16;

    private int tex;

    private List<String> paths = new ArrayList<>();
    // path -> layer; missing files map to the fallback layer so they don't each take a layer
    private final Map<String, Integer> layers = new HashMap<>();
    private int fallback = -1;
    private int missingCount = 0;

    /** Registers the texture used for any path that doesn't exist. Call before addTexture. */
    public int setFallbackTexture(String path) {
        fallback = addTexture(path);
        return fallback;
    }

    public int addTexture(String path){
        Integer layer = layers.get(path);
        if (layer != null) return layer;

        if (fallback >= 0 && Main.class.getClassLoader().getResource(path) == null) {
            missingCount++;
            layers.put(path, fallback);
            return fallback;
        }

        paths.add(path);
        layers.put(path, paths.size()-1);
        return paths.size()-1;
    }

    public void loadTextures() {
        //System.out.println(paths);
        int maxLayers = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);
        System.out.println("Texture array: " + paths.size() + " layers (max " + maxLayers + "), "
                + missingCount + " missing textures use the fallback");
        if (paths.size() > maxLayers) {
            throw new RuntimeException("Too many textures for one texture array: "
                    + paths.size() + " > " + maxLayers);
        }

        // No flip: block shape UVs put v=0 at the top of the image (Minecraft convention)
        stbi_set_flip_vertically_on_load(false);
        this.tex = glGenTextures();

        glBindTexture(GL_TEXTURE_2D_ARRAY, this.tex);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, 4);

        glTexImage3D(
                GL_TEXTURE_2D_ARRAY,
                0,
                GL_RGBA,
                texSize,
                texSize,
                paths.size(),
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );

        for (int i = 0; i < paths.size(); i++) {
            ByteBuffer image = null;
            ByteBuffer buf = null;

            try (MemoryStack stack = stackPush()) {
                InputStream is = Main.class
                        .getClassLoader()
                        .getResourceAsStream(paths.get(i));

                if (is == null) {
                    throw new RuntimeException("Texture not found: " + paths.get(i));
                }

                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                byte[] bytes = is.readAllBytes();

                buf = MemoryUtil.memAlloc(bytes.length);
                buf.put(bytes).flip();

                image = stbi_load_from_memory(
                        buf,
                        width,
                        height,
                        channels,
                        4
                );

                if (image == null) {
                    throw new RuntimeException(
                            "STB failed to load " + paths.get(i) +
                                    ": " + stbi_failure_reason()
                    );
                }

                int w = width.get(0);
                int h = height.get(0);

                if (w != texSize || h != texSize) {
                    throw new RuntimeException("Texture is " + w + "x" + h + ", expected " + texSize + "x" + texSize);
                }

                glTexSubImage3D(
                        GL_TEXTURE_2D_ARRAY,
                        0,
                        0,
                        0,
                        i,
                        w,
                        h,
                        1,
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        image
                );

            } catch (Exception e) {
                System.err.println(
                        "Error loading texture " + paths.get(i) + ":"
                );
                e.printStackTrace();

            } finally {
                if (image != null) {
                    stbi_image_free(image);
                }

                if (buf != null) {
                    MemoryUtil.memFree(buf);
                }
            }
        }

        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, this.tex);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    public void cleanup() {
        glDeleteTextures(this.tex);
    }
}