package org.sutormin.nanocraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Settings, read from options.txt in the working directory at startup ({@link #load}). The file is
 * created with the defaults below if it doesn't exist.
 */
public class Options {
    public static String SERVER_IP = "127.0.0.1";
    public static int PORT = 25565;

    public static String PLAYER_USERNAME = "Player";
    /** Offline-mode UUID, derived from the username the way vanilla servers do. */
    public static UUID PLAYER_UUID = offlineUuid(PLAYER_USERNAME);
    public static byte VIEW_DISTANCE = 10;
    public static float RECEIVE_CHUNKS_PER_TICK = 5;

    /** Background threads that build chunk meshes. */
    public static int MESH_THREADS = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 8);
    /** Max time per frame spent uploading finished chunk meshes to the GPU, in milliseconds. */
    public static float MESH_UPLOAD_BUDGET_MS = 4.0f;

    /** See-through leaves, like vanilla's "fancy" leaves; off draws them opaque ("fast"), which is quicker. */
    public static boolean TRANSPARENT_LEAVES = false;

    // Debugging
    /** Prints every server packet NanoCraft doesn't handle (they're skipped). */
    public static boolean DEBUG_LOG_UNKNOWN_S2C_PACKETS = false;
    /** Prints every server packet NanoCraft handles. Chunk packets make this busy. */
    public static boolean DEBUG_LOG_KNOWN_S2C_PACKETS = false;
    /** Prints every packet sent to the server. Positions and tick ends go out 20 times a second. */
    public static boolean DEBUG_LOG_C2S_PACKETS = false;
    /** Prints the name of each texture that isn't found (they show the null.png fallback). */
    public static boolean DEBUG_LOG_MISSING_TEXTURES = false;
    /** Shows FPS, loaded chunks and position in the window title. */
    public static boolean DEBUG_SHOW_FPS = false;
    /** Draws the world as wireframe triangles. */
    public static boolean DEBUG_WIREFRAME = false;

    private static final String DEFAULTS = """
            # NanoCraft options. Lines are key=value; # starts a comment.

            # server to join: a Minecraft 26.3 server in offline mode
            server=%s
            port=%d
            # your name on the server: 3-16 letters, digits or _
            username=%s

            # chunks the server sends around you
            view_distance=%d
            # background threads that build chunk meshes (default: CPU cores - 1, at most 8)
            mesh_threads=%d
            # max milliseconds per frame spent uploading chunk meshes to the GPU
            mesh_upload_budget_ms=%s
            # see-through leaves like vanilla's "fancy" leaves (false: opaque "fast" leaves, quicker)
            transparent_leaves=false

            # debugging (true/false)
            # print server packets NanoCraft skips / handles, and packets sent to the server
            debug_log_unknown_s2c_packets=false
            debug_log_known_s2c_packets=false
            debug_log_c2s_packets=false
            # print each texture that isn't found, instead of just how many
            debug_log_missing_textures=false
            # FPS, loaded chunks and position in the window title
            debug_show_fps=false
            # draw the world as wireframe
            debug_wireframe=false
            """;

    /** Reads the options file, or writes one with the defaults if there isn't any. */
    public static void load(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.writeString(file, DEFAULTS.formatted(SERVER_IP, PORT, PLAYER_USERNAME, VIEW_DISTANCE,
                        MESH_THREADS, MESH_UPLOAD_BUDGET_MS));
                System.out.println("[Client] Wrote default options to " + file.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("[Client] Couldn't write " + file + ": " + e);
            }
            return;
        }

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) {
                    System.err.println("[Client] " + file + ": ignoring \"" + line + "\" (expected key=value)");
                    continue;
                }
                set(file, line.substring(0, eq).strip(), line.substring(eq + 1).strip());
            }
        } catch (IOException e) {
            System.err.println("[Client] Couldn't read " + file + ", using the defaults: " + e);
        }

        if (!PLAYER_USERNAME.matches("[A-Za-z0-9_]{3,16}")) {
            System.err.println("[Client] Username \"" + PLAYER_USERNAME + "\" isn't 3-16 letters, digits or _;"
                    + " servers will probably refuse it");
        }
        PLAYER_UUID = offlineUuid(PLAYER_USERNAME);
    }

    private static void set(Path file, String key, String value) {
        try {
            switch (key) {
                case "server" -> SERVER_IP = value;
                case "port" -> PORT = Integer.parseInt(value);
                case "username" -> PLAYER_USERNAME = value;
                case "view_distance" -> VIEW_DISTANCE = (byte) Math.clamp(Integer.parseInt(value), 2, 32);
                case "mesh_threads" -> MESH_THREADS = Math.max(1, Integer.parseInt(value));
                case "mesh_upload_budget_ms" -> MESH_UPLOAD_BUDGET_MS = Float.parseFloat(value);
                case "transparent_leaves" -> TRANSPARENT_LEAVES = bool(value);
                case "debug_log_unknown_s2c_packets" -> DEBUG_LOG_UNKNOWN_S2C_PACKETS = bool(value);
                case "debug_log_known_s2c_packets" -> DEBUG_LOG_KNOWN_S2C_PACKETS = bool(value);
                case "debug_log_c2s_packets" -> DEBUG_LOG_C2S_PACKETS = bool(value);
                case "debug_log_missing_textures" -> DEBUG_LOG_MISSING_TEXTURES = bool(value);
                case "debug_show_fps" -> DEBUG_SHOW_FPS = bool(value);
                case "debug_wireframe" -> DEBUG_WIREFRAME = bool(value);
                default -> System.err.println("[Client] " + file + ": unknown option \"" + key + "\"");
            }
        } catch (NumberFormatException e) {
            System.err.println("[Client] " + file + ": \"" + key + "\" needs a number, not \"" + value + "\"");
        } catch (IllegalArgumentException e) {
            System.err.println("[Client] " + file + ": \"" + key + "\" needs true or false, not \"" + value + "\"");
        }
    }

    private static boolean bool(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(value);
    }

    private static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
