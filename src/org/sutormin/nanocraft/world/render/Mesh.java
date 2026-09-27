package org.sutormin.nanocraft.world.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

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

    // back-to-front sorting, for translucent meshes only (see sortFor)
    private int[] indices;    // CPU copy of the index buffer, rewritten in sorted order
    private float[] centers;  // x, y, z of each triangle's centroid, chunk-local blocks
    private long[] sortKeys;
    private float sortedX = Float.NaN, sortedY, sortedZ;

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

        // attribute 1: ao, texture layer, flags (chars 4-6, byte offset 8)
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
    public record Prepared(ByteBuffer vertices, IntBuffer indices, int indexCount, int[] sortIndices, float[] centers) {
        Prepared(ByteBuffer vertices, IntBuffer indices, int indexCount) {
            this(vertices, indices, indexCount, null, null);
        }

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

    /**
     * Like {@link #prepare}, but also keeps what {@link #sortFor} needs to draw the triangles back
     * to front. For blended (translucent) meshes: their faces don't write depth, so the draw order
     * decides which one ends up in front.
     */
    public static Prepared prepareSorted(char[] vertices, int charCount, int[] indices, int iCount) {
        Prepared p = prepare(vertices, charCount, indices, iCount);
        if (p.indexCount() == 0) return p;
        float[] centers = new float[iCount];  // 3 floats per triangle = 1 per index
        for (int k = 0; k < iCount; k++) {
            int v = indices[k] * CHARS_PER_VERTEX;
            int c = k / 3 * 3;
            centers[c] += vertices[v];
            centers[c + 1] += vertices[v + 1];
            centers[c + 2] += vertices[v + 2];
        }
        for (int c = 0; c < centers.length; c++) centers[c] /= 3 * 128f; // positions are 1/128 block
        return new Prepared(p.vertices(), p.indices(), iCount, Arrays.copyOf(indices, iCount), centers);
    }

    /** Uploads prepared data and frees its buffers. GL thread only. */
    public void upload(Prepared data) {
        generated = true;
        indexCount = data.indexCount();
        indices = data.sortIndices();
        centers = data.centers();
        sortKeys = indices == null ? null : new long[indexCount / 3];
        sortedX = Float.NaN; // sort before the first draw
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

    /**
     * Reorders the triangles farthest first from the camera (render coordinates), like vanilla does
     * for translucent sections. Only meshes from {@link #prepareSorted}; skipped until the camera has
     * moved {@code minMove} blocks since the last sort. GL thread only.
     */
    public void sortFor(float camX, float camY, float camZ, float minMove) {
        if (indices == null || indexCount == 0) return;
        float mx = camX - sortedX, my = camY - sortedY, mz = camZ - sortedZ;
        if (mx * mx + my * my + mz * mz < minMove * minMove) return; // false for NaN: always sorts
        sortedX = camX;
        sortedY = camY;
        sortedZ = camZ;

        float lx = camX - chunkX * 16f, lz = camZ - chunkZ * 16f;
        int triangles = indexCount / 3;
        int[] old = Arrays.copyOf(indices, indexCount);
        for (int t = 0; t < triangles; t++) {
            float dx = centers[t * 3] - lx, dy = centers[t * 3 + 1] - camY, dz = centers[t * 3 + 2] - lz;
            // distance bits of a positive float sort like the float; negated for farthest first
            sortKeys[t] = ((long) ~Float.floatToRawIntBits(dx * dx + dy * dy + dz * dz) << 32) | t;
        }
        Arrays.sort(sortKeys, 0, triangles);
        for (int t = 0; t < triangles; t++) {
            int from = (int) sortKeys[t] * 3;
            System.arraycopy(old, from, indices, t * 3, 3);
            sortKeys[t] = from; // reused below to move the centers along with their triangles
        }
        float[] oldCenters = Arrays.copyOf(centers, centers.length);
        for (int t = 0; t < triangles; t++) System.arraycopy(oldCenters, (int) sortKeys[t], centers, t * 3, 3);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void setPos(int x, int z) {
        chunkX = x;
        chunkZ = z;
    }

    public void render() {
        if (indexCount == 0) return;
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