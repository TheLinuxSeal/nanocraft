package org.sutormin.nanocraft;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {
    //private final int textureId;
    private int texSize = 128;

    private int tex;

    public Texture(String filePath) {
        stbi_set_flip_vertically_on_load(true);
        createTexture();
        /*textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 4);

        stbi_set_flip_vertically_on_load(true);

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = stbi_load(filePath, width, height, channels, 4);
            if (image == null) {
                throw new RuntimeException("[ERROR] failed to load texture file '" + filePath + "': " + stbi_failure_reason());
            }

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);

            stbi_image_free(image);
        }*/
    }

    private void createTexture() {
        System.out.println("createTexture()");
        this.tex = glGenTextures();
        System.out.println("genT()");
        glBindTexture(GL_TEXTURE_2D_ARRAY, this.tex);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, 4);
        System.out.println("tParam()");
        /* Add maximum anisotropic filtering, if available */
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA, texSize, texSize, 1, 0, GL_RGB,
                GL_UNSIGNED_BYTE, (ByteBuffer) null);
        System.out.println("something()");
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer image = stbi_load("res/atlas.png", width, height, channels, 4);
            System.out.println("load()");
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, texSize, texSize, 1, GL_RGBA, GL_UNSIGNED_BYTE, image);
            System.out.println("subimage()");
        }
        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
        System.out.println("END createTexture()");
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