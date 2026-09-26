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
     * Vertex and index data copied into native buffers, ready for {@link #upload}.
     * Preparing needs no GL context, so it can run on a worker thread.
     */
    public record Prepared(ByteBuffer vertices, IntBuffer indices, int indexCount) {
        /** Frees the native buffers; {@link #upload} does this for you. */
        public void free() {
            if (vertices != null) MemoryUtil.memFree(vertices);
            if (indices != null) MemoryUtil.memFree(indices);
        }
    }

    /**
     * @param vertices  interleaved vertex data, 7 chars per vertex
     * @param charCount number of chars actually written (vertexCount * 7)
     */
    public static Prepared prepare(char[] vertices, int charCount, int[] indices, int iCount) {
        if (charCount % CHARS_PER_VERTEX != 0) {
            throw new IllegalStateException("charCount " + charCount + " is not a multiple of " + CHARS_PER_VERTEX);
        }
        int verts = charCount / CHARS_PER_VERTEX;
        for (int k = 0; k < iCount; k++) {
            if (indices[k] < 0 || indices[k] >= verts) {
                throw new IllegalStateException("index " + indices[k] + " at " + k + " but only " + verts + " vertices");
            }
        }

        if (charCount == 0 || iCount == 0) {
            return new Prepared(null, null, 0);
        }

        // Vertex buffer: raw bytes, filled through a native-order char view
        ByteBuffer vBuffer = MemoryUtil.memAlloc(charCount * Character.BYTES);
        vBuffer.order(ByteOrder.nativeOrder());
        vBuffer.asCharBuffer().put(vertices, 0, charCount); // doesn't move vBuffer's position

        IntBuffer iBuffer = MemoryUtil.memAllocInt(iCount);
        iBuffer.put(indices, 0, iCount).flip();

        return new Prepared(vBuffer, iBuffer, iCount);
    }

    /** Uploads prepared data and frees its buffers. GL thread only. */
    public void upload(Prepared data) {
        generated = true;
        indexCount = data.indexCount();
        if (indexCount == 0) return;

        try {
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, data.vertices(), GL_DYNAMIC_DRAW);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indices(), GL_DYNAMIC_DRAW);
        } finally {
            data.free();
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /** {@link #prepare} and {@link #upload} in one go. GL thread only. */
    public void updateMesh(char[] vertices, int charCount, int[] indices, int iCount) {
        upload(prepare(vertices, charCount, indices, iCount));
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