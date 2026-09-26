package org.sutormin.nanocraft;

import java.util.UUID;

public class Options {
    public static String SERVER_IP = "127.0.0.1";
    public static int PORT = 25565;

    public static String PLAYER_USERNAME = "TheLinuxSeal";
    public static UUID PLAYER_UUID = UUID.randomUUID();
    public static byte VIEW_DISTANCE = 2;
    public static float RECEIVE_CHUNKS_PER_TICK = 5;

    /** Background threads that build chunk meshes. */
    public static int MESH_THREADS = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
    /** Max time per frame spent uploading finished chunk meshes to the GPU, in milliseconds. */
    public static float MESH_UPLOAD_BUDGET_MS = 4.0f;
}
