package org.sutormin.nanocraft.resources;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.sutormin.nanocraft.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {
    private int texSize = 16;

    private int tex;

    private List<String> paths = new ArrayList<>();

    public int addTexture(String path){
        if (paths.contains(path)) return paths.indexOf(path);
        paths.add(path);
        return paths.size()-1;
    }

    public void loadTextures(){
        stbi_set_flip_vertically_on_load(true);
        this.tex = glGenTextures();

        glBindTexture(GL_TEXTURE_2D_ARRAY, this.tex);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, 4);
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA, texSize, texSize, paths.size(), 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        try (MemoryStack stack = stackPush()) {
            for (var i = 0; i < paths.size(); i++) {
                InputStream is = Main.class.getClassLoader().getResourceAsStream(paths.get(i));
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                byte[] bytes = is.readAllBytes();

                ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
                buf.put(bytes);
                buf.flip();

                ByteBuffer image = stbi_load_from_memory(buf, width, height, channels, 4);

                MemoryUtil.memFree(buf);

                int w = width.get(0);
                int h = height.get(0);
                glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, i, w, h, 1, GL_RGBA, GL_UNSIGNED_BYTE, image);
            }
        } catch (Exception e) {
            System.out.println("Error: could not load textures :(");
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