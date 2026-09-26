package org.sutormin.nanocraft.world;

import org.sutormin.nanocraft.NanoCraft;
import org.sutormin.nanocraft.Options;
import org.sutormin.nanocraft.data.Registries;
import org.sutormin.nanocraft.data.quickaccess.QuickAccessBlocks;
import org.sutormin.nanocraft.world.chunk.Chunk;
import org.sutormin.nanocraft.world.chunk.ChunkLoader;
import org.sutormin.nanocraft.world.chunk.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class World {
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final Set<ChunkPos> dirty = new LinkedHashSet<>();
    private final Map<ChunkPos, List<ChunkLoader.BlockChange>> pendingBlockChanges = new HashMap<>();

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

    private void remesh(ChunkPos pos) {
        Chunk c = chunks.get(pos);
        if (c != null) c.buildMesh();
    }

    public void drainNetworkChunks(int budget) {
        ChunkLoader.Pending p;
        while (budget-- > 0 && (p = ChunkLoader.poll()) != null) {
            Chunk c = new Chunk(p.pos());
            c.setBlocks(p.blocks());
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

    //public void drainRemeshes(int budget) {
    //    ChunkPos change;
    //    while (budget-- > 0 && (change = ChunkLoader.pollRemesh()) != null) {
    //        dirty.add(change);
    //        //setBlockAt(change.x(), change.y(), change.z(), change.block());
    //    }
    //}

    public void drainBlockChanges(int budget) {
        ChunkLoader.BlockChange change;
        while (budget-- > 0 && (change = ChunkLoader.pollBlockChange()) != null) {
            setBlockAt(change.x(), change.y(), change.z(), change.block());
        }
    }

    public int chunkCount() {
        return chunks.size();
    }

    public char getBlockAt(int x, int y, int z) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return QuickAccessBlocks.NULL;

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
                .add(new ChunkLoader.BlockChange(x, y, z, block));
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

    /** Opaque and cutout geometry. Call before {@link #renderTranslucent}. */
    public void renderChunks() {
        for (Chunk chunk : chunks.values()) {
            chunk.render();
        }
    }

    /**
     * Translucent geometry, farthest chunk first so nearer water/glass blends over farther.
     * Faces inside one chunk aren't sorted, so overlapping translucent faces in the same
     * chunk can blend in the wrong order from some angles.
     */
    public void renderTranslucent() {
        float camX = NanoCraft.CAMERA.getX();
        float camZ = NanoCraft.CAMERA.getZ();
        List<Chunk> sorted = new ArrayList<>(chunks.values());
        sorted.sort(Comparator.comparingDouble((Chunk c) -> {
            float dx = c.getPos().x() * Chunk.SIZE_X + Chunk.SIZE_X / 2f - camX;
            float dz = c.getPos().z() * Chunk.SIZE_Z + Chunk.SIZE_Z / 2f - camZ;
            return dx * dx + dz * dz;
        }).reversed());
        for (Chunk chunk : sorted) {
            chunk.renderTranslucent();
        }
    }

    public void tick(){
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
    }
}