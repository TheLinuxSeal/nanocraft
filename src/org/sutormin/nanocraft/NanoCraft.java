package org.sutormin.nanocraft;

import org.joml.Matrix4f;
import org.sutormin.nanocraft.data.Registries;
import org.sutormin.nanocraft.definitions.block.CommonBlocks;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.sutormin.nanocraft.networking.NetworkPhase;
import org.sutormin.nanocraft.networking.Networking;
import org.sutormin.nanocraft.networking.packets.play.player.C2SClientTickEnd;
import org.sutormin.nanocraft.render.Window;
import org.sutormin.nanocraft.world.Dimension;
import org.sutormin.nanocraft.resources.block.BlockDefinitionParser;
import org.sutormin.nanocraft.resources.block.BlockShapeParser;
import org.sutormin.nanocraft.render.Textures;
import org.sutormin.nanocraft.world.BlockStateMapper;
import org.sutormin.nanocraft.world.chunk.ChunkPos;
import org.sutormin.nanocraft.world.World;
import org.sutormin.nanocraft.world.biome.BiomeTint;
import static org.sutormin.nanocraft.render.Window.WIDTH;
import static org.sutormin.nanocraft.render.Window.HEIGHT;

import java.util.List;
import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.sutormin.nanocraft.render.Window.WINDOW_ID;

public class NanoCraft {

    public static Window WINDOW;
    public static World WORLD;
    public static Camera CAMERA;
    public static Networking NETWORKING;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        WINDOW = new Window();
        WORLD = new World();
        CAMERA = new Camera();
        NETWORKING = new Networking();

        WINDOW.init();

        System.out.println("Loading block definitions!");
        BlockDefinitionParser.loadFromIndex();
        System.out.println("Loading block shapes!");
        BlockShapeParser.loadFromIndex();

        System.out.println("Loading registries!");
        Registries.defineAll();
        CommonBlocks.loadFromRegistry();

        System.out.println("Loading biome colors!");
        BiomeTint.load();

        System.out.println("Loading blockstate map!");
        BlockStateMapper.load();

        System.out.println("Loading textures!");
        Textures.loadTextures();

        System.out.println("Loading world!");
        WORLD.init();

        System.out.println("Starting networking!");
        NETWORKING.init();
    }

    private void loop() {
        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(60.0f), (float) WIDTH / HEIGHT, 0.1f, 1000.0f
        );

        long lastTime = System.nanoTime();
        float clientTickTime = 0.0f;
        long fpsStart = lastTime;
        int frames = 0;
        while (!glfwWindowShouldClose(WINDOW_ID)) {
            glfwPollEvents();
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1000000000.0f;
            lastTime = now;

            frames++;
            if (now - fpsStart >= 1_000_000_000L) {
                if (Options.DEBUG_SHOW_FPS) {
                    glfwSetWindowTitle(WINDOW_ID, String.format("NanoCraft | %d FPS | %d chunks | %.1f %.1f %.1f",
                            frames, WORLD.chunkCount(), CAMERA.getX(), CAMERA.getY() + Dimension.minY(), CAMERA.getZ()));
                }
                frames = 0;
                fpsStart = now;
            }

            // fixed 20 Hz client ticks for the server, like vanilla, however fast frames are drawn
            clientTickTime += deltaTime;
            if (clientTickTime > 0.25f) clientTickTime = CLIENT_TICK; // after a stall, don't send a burst
            while (clientTickTime >= CLIENT_TICK) {
                clientTick();
                clientTickTime -= CLIENT_TICK;
            }

            WORLD.tick();

            WINDOW.processInput(deltaTime);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            WORLD.render(projection);

            glfwSwapBuffers(WINDOW_ID);

        }
    }

    private static final float CLIENT_TICK = 1.0f / 20.0f;

    /**
     * One client tick: at most one position update, then "tick end". Since 26.3 the server disconnects
     * clients that send more than one position between two tick ends.
     */
    private void clientTick() {
        if (NETWORKING.networkPhase != NetworkPhase.PLAY) return;
        CAMERA.sendPositionIfMoved();
        ByteBuf buf = Unpooled.buffer();
        C2SClientTickEnd.make(buf);
        NETWORKING.sendPacket(buf);
    }

    private List<ChunkPos> getChunksInRenderDistance(ChunkPos center, int renderDistance) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                chunks.add(new ChunkPos(center.x() + x, center.z() + z));
            }
        }
        return chunks;
    }

    private void cleanup() {
        Textures.cleanup();
        WORLD.cleanup();

        glfwFreeCallbacks(WINDOW_ID);
        glfwDestroyWindow(WINDOW_ID);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}