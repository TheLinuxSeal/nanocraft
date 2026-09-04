package org.sutormin.nanocraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.sutormin.nanocraft.world.Chunk;
import org.sutormin.nanocraft.world.ChunkPos;

public class Camera {
    private final Vector3f position = new Vector3f(8.0f, 20.0f, 25.0f);
    private final Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);

    private final Vector3f movement = new Vector3f();

    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private final float sensitivity = 0.1f;

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, movement.set(position).add(front), up);
    }

    public void updatePosition(float forwardBack, float rightLeft, float upDown, float speed, float dt) {
        float distance = speed * dt;

        if (forwardBack != 0.0f) position.add(movement.set(front.x, 0.0f, front.z).normalize().mul(forwardBack * distance));
        if (rightLeft != 0.0f) position.add(movement.set(right).mul(rightLeft * distance));
        if (upDown != 0.0f) position.add(movement.set(up).mul(upDown * distance));
    }

    public void processMouseInput(float xOffset, float yOffset) {
        yaw += xOffset * sensitivity;
        pitch -= yOffset * sensitivity;

        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));

        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();

        front.cross(0.0f, 1.0f, 0.0f, right).normalize();
    }

    public ChunkPos getChunkPos() {
        int chunkX = (int) Math.floor(position.x / Chunk.SIZE_X);
        int chunkZ = (int) Math.floor(position.z / Chunk.SIZE_Z);
        return new ChunkPos(chunkX, chunkZ);
    }
}