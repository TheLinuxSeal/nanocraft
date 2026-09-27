package org.sutormin.nanocraft.world;

import org.joml.Matrix4f;
import org.sutormin.nanocraft.Options;
import org.sutormin.nanocraft.definitions.block.CommonBlocks;
import org.sutormin.nanocraft.render.Shader;
import org.sutormin.nanocraft.render.Textures;
import org.sutormin.nanocraft.world.chunk.Chunk;
import org.sutormin.nanocraft.world.chunk.ChunkLoader;
import org.sutormin.nanocraft.world.chunk.ChunkPos;
import org.sutormin.nanocraft.world.render.WorldShaders;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glPolygonOffset;
import static org.sutormin.nanocraft.NanoCraft.CAMERA;

public class World {

    public static Shader WORLD_SHADER;

    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final Set<ChunkPos> dirty = new LinkedHashSet<>();
    private final Map<ChunkPos, List<ChunkLoader.BlockChange>> pendingBlockChanges = new HashMap<>();
    private int generation; // ChunkLoader generation (dimension) of the loaded chunks

    // Meshing runs on worker threads; only captureNeighbors and uploads touch the main thread.
    // A chunk is in `meshing` from submission until its result is taken off `meshed`, so it never
    // has two builds at once. If it gets dirty meanwhile it stays in `dirty` and is resubmitted after.
    private final ExecutorService meshExecutor = Executors.newFixedThreadPool(Options.MESH_THREADS, r -> {
        Thread t = new Thread(r, "chunk-mesher");
        t.setDaemon(true);
        return t;
    });
    private final Set<Chunk> meshing = new HashSet<>();
    private final Queue<MeshResult> meshed = new ConcurrentLinkedQueue<>();

    private record MeshResult(Chunk chunk, Chunk.MeshData data) {}

    public World() {
    }

    public void init(){
        WORLD_SHADER = new Shader(WorldShaders.WORLD_VERTEX_SHADER,WorldShaders.WORLD_FRAGMENT_SHADER);
        WORLD_SHADER.createUniform("uProjection");
        WORLD_SHADER.createUniform("uView");
        WORLD_SHADER.createUniform("uChunkOffset");
        WORLD_SHADER.createUniform("uAlphaCutoff");
    }

    public void removeChunk(ChunkPos pos) {
        if (!chunks.containsKey(pos)) return;
        Chunk chunk = chunks.get(pos);
        chunk.cleanup();
        chunks.remove(pos);
    }

    public void addChunk(ChunkPos pos, Chunk chunk) {
        Chunk old = chunks.put(pos, chunk);
        if (old != null) old.cleanup();

        // Replay queued updates for this chunk
        List<ChunkLoader.BlockChange> pending = pendingBlockChanges.remove(pos);
        if (pending != null) {
            for (ChunkLoader.BlockChange change : pending) {
                int localX = Math.floorMod(change.x(), Chunk.SIZE_X);
                int localZ = Math.floorMod(change.z(), Chunk.SIZE_Z);
                chunk.setBlock(localX, change.y(), localZ, change.block());
            }
        }

        dirty.add(pos);
        dirty.add(pos.offset(-1, 0));
        dirty.add(pos.offset(1, 0));
        dirty.add(pos.offset(0, -1));
        dirty.add(pos.offset(0, 1));
    }

    /**
     * Drops every chunk when the player has changed dimension ({@link ChunkLoader#clear}). Anything
     * queued from an older generation is skipped; anything from a newer one clears first.
     *
     * @return false if something from {@code gen} is stale and must be skipped
     */
    private boolean syncGeneration(int gen) {
        if (gen < generation) return false;
        if (gen > generation) {
            for (Chunk chunk : chunks.values()) chunk.cleanup();
            chunks.clear();
            dirty.clear();
            pendingBlockChanges.clear();
            generation = gen;
        }
        return true;
    }

    public void drainNetworkChunks(int budget) {
        ChunkLoader.Pending p;
        while (budget-- > 0 && (p = ChunkLoader.poll()) != null) {
            if (!syncGeneration(p.generation())) continue;
            Chunk c = new Chunk(p.pos());
            c.setBlocks(p.blocks());
            c.setBiomes(p.biomes());
            addChunk(p.pos(), c);
        }
    }

    /** Hands dirty chunks to the mesh workers, keeping at most {@code maxInFlight} builds queued. */
    public void submitDirty(int maxInFlight) {
        Iterator<ChunkPos> it = dirty.iterator();
        while (it.hasNext() && meshing.size() < maxInFlight) {
            ChunkPos pos = it.next();
            Chunk c = chunks.get(pos);
            if (c == null) {
                it.remove();
                continue;
            }
            if (meshing.contains(c)) continue; // still building; resubmit once that finishes

            it.remove();
            meshing.add(c);
            c.captureNeighbors();
            meshExecutor.execute(() -> {
                Chunk.MeshData data = null;
                try {
                    data = c.buildMeshData();
                } catch (RuntimeException e) {
                    System.err.println("Failed to mesh chunk " + c.getPos());
                    e.printStackTrace();
                }
                meshed.add(new MeshResult(c, data));
            });
        }
    }

    /** Uploads finished meshes until {@code budgetNanos} has passed; the rest wait for the next frame. */
    public void uploadMeshes(long budgetNanos) {
        long deadline = System.nanoTime() + budgetNanos;
        MeshResult result;
        while (System.nanoTime() < deadline && (result = meshed.poll()) != null) {
            Chunk c = result.chunk();
            meshing.remove(c);
            if (result.data() == null) continue;
            // skip chunks that were unloaded or replaced while building
            if (chunks.get(c.getPos()) == c) c.uploadMesh(result.data());
            else result.data().free();
        }
    }

    public void drainUnloads(int budget) {
        ChunkPos pos;
        while (budget-- > 0 && (pos = ChunkLoader.pollUnload()) != null) {
            removeChunk(pos);
            dirty.add(pos.offset(-1, 0));
            dirty.add(pos.offset(1, 0));
            dirty.add(pos.offset(0, -1));
            dirty.add(pos.offset(0, 1));
        }
    }

    public void drainBlockChanges(int budget) {
        ChunkLoader.BlockChange change;
        while (budget-- > 0 && (change = ChunkLoader.pollBlockChange()) != null) {
            if (!syncGeneration(change.generation())) continue;
            setBlockAt(change.x(), change.y(), change.z(), change.block());
        }
    }

    public int chunkCount() {
        return chunks.size();
    }

    public char getBlockAt(int x, int y, int z) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return CommonBlocks.NULL;

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        return chunk.getBlock(localX, y, localZ);
    }

    public void setBlockAt(int x, int y, int z, char block) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);

        if (chunk == null) {
            // Queue the block change until the chunk is added to the world
            pendingBlockChanges.computeIfAbsent(chunkPos, k -> new ArrayList<>())
                .add(new ChunkLoader.BlockChange(x, y, z, block, generation));
            return;
        }

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        chunk.setBlock(localX, y, localZ, block);
        dirty.add(chunkPos);

        if (localX == 0) dirty.add(chunkPos.offset(-1, 0));
        if (localX == Chunk.SIZE_X - 1) dirty.add(chunkPos.offset(1, 0));
        if (localZ == 0) dirty.add(chunkPos.offset(0, -1));
        if (localZ == Chunk.SIZE_Z - 1) dirty.add(chunkPos.offset(0, 1));
    }


    public Chunk getChunk(ChunkPos pos){
        return chunks.get(pos);
    }

    public ChunkPos getChunkPosFromBlock(int x, int z) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
        return new ChunkPos(chunkX, chunkZ);
    }

    public void render(Matrix4f projection){
        Textures.BLOCK.bind();
        WORLD_SHADER.bind();
        WORLD_SHADER.setUniform("uProjection", projection);
        WORLD_SHADER.setUniform("uView", CAMERA.getViewMatrix());

        if (Options.DEBUG_WIREFRAME) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        WORLD_SHADER.setUniform("uAlphaCutoff", 0.5f);
        renderChunks();

        // Translucent pass: blended over the opaque scene, depth-tested but not written,
        // so translucent faces don't hide each other. Pushed slightly back in depth so a solid
        // face lying in the same plane (the side of waterlogged stairs) always wins instead of
        // flickering against the water.
        WORLD_SHADER.setUniform("uAlphaCutoff", 0.004f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        renderTranslucent();
        glDisable(GL_POLYGON_OFFSET_FILL);
        glDepthMask(true);
        glDisable(GL_BLEND);

        if (Options.DEBUG_WIREFRAME) glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        Textures.BLOCK.unbind();
        WORLD_SHADER.unbind();
    }

    /** Opaque and cutout geometry. Call before {@link #renderTranslucent}. */
    public void renderChunks() {
        for (Chunk chunk : chunks.values()) {
            chunk.render();
        }
    }

    /**
     * Translucent geometry, farthest chunk first so nearer water/glass blends over farther.
     * Each chunk also sorts its own faces back to front (e.g. water seen through ice).
     */
    public void renderTranslucent() {
        float camX = CAMERA.getX();
        float camY = CAMERA.getY();
        float camZ = CAMERA.getZ();
        List<Chunk> sorted = new ArrayList<>(chunks.values());
        sorted.sort(Comparator.comparingDouble((Chunk c) -> {
            float dx = c.getPos().x() * Chunk.SIZE_X + Chunk.SIZE_X / 2f - camX;
            float dz = c.getPos().z() * Chunk.SIZE_Z + Chunk.SIZE_Z / 2f - camZ;
            return dx * dx + dz * dz;
        }).reversed());
        for (Chunk chunk : sorted) {
            chunk.renderTranslucent(camX, camY, camZ);
        }
    }

    public void tick(){
        syncGeneration(ChunkLoader.generation());
        drainNetworkChunks(100);
        drainUnloads(50);
        drainBlockChanges(200);
        submitDirty(Options.MESH_THREADS * 2);
        uploadMeshes((long) (Options.MESH_UPLOAD_BUDGET_MS * 1_000_000));
    }

    public void cleanup() {
        meshExecutor.shutdownNow();
        MeshResult result;
        while ((result = meshed.poll()) != null) {
            if (result.data() != null) result.data().free();
        }
        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();
        WORLD_SHADER.cleanup();
    }
}