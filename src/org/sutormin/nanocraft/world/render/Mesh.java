package org.sutormin.nanocraft.world.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.sutormin.nanocraft.NanoCraft.SHADER;

public class Mesh {
    public static final int CHARS_PER_VERTEX = 7;
    private static final int STRIDE = CHARS_PER_VERTEX * Character.BYTES; // 14 bytes

    private final int vaoId;
    private final int vboId;
    private final int eboId;
    private int indexCount;
    public boolean generated = false;
    private int chunkX;
    private int chunkZ;

    public Mesh() {
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);

        // attribute 0: gx, gy, gz, uv (chars 0-3)
        glVertexAttribIPointer(0, 4, GL_UNSIGNED_SHORT, STRIDE, 0L);
        glEnableVertexAttribArray(0);

        // attribute 1: ao, texL, texH (chars 4-6, byte offset 8)
        glVertexAttribIPointer(1, 3, GL_UNSIGNED_SHORT, STRIDE, 4L * Character.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * @param vertices  interleaved vertex data, 7 chars per vertex
     * @param charCount number of chars actually written (vertexCount * 7)
     */
    public void updateMesh(char[] vertices, int charCount, int[] indices, int iCount) {

        if (charCount % CHARS_PER_VERTEX != 0) {
            throw new IllegalStateException("charCount " + charCount + " is not a multiple of " + CHARS_PER_VERTEX);
        }
        int verts = charCount / CHARS_PER_VERTEX;
        for (int k = 0; k < iCount; k++) {
            if (indices[k] < 0 || indices[k] >= verts) {
                throw new IllegalStateException("index " + indices[k] + " at " + k + " but only " + verts + " vertices");
            }
        }

        generated = true;

        if (charCount == 0 || iCount == 0) {
            this.indexCount = 0;
            return;
        }

        indexCount = iCount;

        // Vertex buffer: raw bytes, filled through a native-order char view
        ByteBuffer vBuffer = MemoryUtil.memAlloc(charCount * Character.BYTES);
        try {
            vBuffer.order(ByteOrder.nativeOrder());
            vBuffer.asCharBuffer().put(vertices, 0, charCount); // doesn't move vBuffer's position

            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, vBuffer, GL_DYNAMIC_DRAW);
        } finally {
            MemoryUtil.memFree(vBuffer);
        }

        // Index buffer
        IntBuffer iBuffer = MemoryUtil.memAllocInt(iCount);
        try {
            iBuffer.put(indices, 0, iCount).flip();

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuffer, GL_DYNAMIC_DRAW);
        } finally {
            MemoryUtil.memFree(iBuffer);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void setPos(int x, int z) {
        chunkX = x;
        chunkZ = z;
    }

    public void render() {
        //glDisable(GL_CULL_FACE);
        //glDisable(GL_DEPTH_TEST);
        glBindVertexArray(vaoId);
        SHADER.setUniform("uChunkOffset", chunkX * 16.0f, chunkZ * 16.0f);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
    }
}