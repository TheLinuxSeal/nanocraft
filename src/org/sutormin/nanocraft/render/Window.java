package org.sutormin.nanocraft.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwInitHint;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.sutormin.nanocraft.NanoCraft.CAMERA;

public class Window {
    public static long WINDOW_ID;
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 576;
    private double lastMouseX = WIDTH / 2.0;
    private double lastMouseY = HEIGHT / 2.0;
    private boolean firstMouse = true;
    public void init(){
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("[ERROR] failed to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);

        WINDOW_ID = glfwCreateWindow(WIDTH, HEIGHT, "NanoCraft", NULL, NULL);
        if (WINDOW_ID == NULL) throw new RuntimeException("[ERROR] failed to create GLFW window");

        glfwSetInputMode(WINDOW_ID, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetCursorPosCallback(WINDOW_ID, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            float xOffset = (float) (xpos - lastMouseX);
            float yOffset = (float) (ypos - lastMouseY);
            lastMouseX = xpos;
            lastMouseY = ypos;

            CAMERA.processMouseInput(xOffset, yOffset);
        });

        org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback(WINDOW_ID, (windowHandle, width, height) -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);

                // Fetch the true pixel dimensions of the framebuffer
                GLFW.glfwGetFramebufferSize(windowHandle, pWidth, pHeight);

                // Update the viewport
                GL11.glViewport(0, 0, pWidth.get(0), pHeight.get(0));
            }
        });

        glfwMakeContextCurrent(WINDOW_ID);
        glfwSwapInterval(1);
        glfwShowWindow(WINDOW_ID);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL); // overlay faces (grass sides) lie exactly on the face below them
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.623f, 0.734f, 0.785f, 1.0f);


        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Fetch the true pixel dimensions of the framebuffer
            GLFW.glfwGetFramebufferSize(WINDOW_ID, pWidth, pHeight);

            // Update the viewport
            GL11.glViewport(0, 0, pWidth.get(0), pHeight.get(0));
        }

    }

    public void processInput(float dt) {
        if (glfwGetKey(WINDOW_ID, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            glfwSetWindowShouldClose(WINDOW_ID, true);
        }

        float forwardBack = 0.0f;
        float rightLeft = 0.0f;
        float upDown = 0.0f;
        float speed = 1f;

        if (glfwGetKey(WINDOW_ID, GLFW_KEY_W) == GLFW_PRESS) forwardBack += 1.0f;
        if (glfwGetKey(WINDOW_ID, GLFW_KEY_S) == GLFW_PRESS) forwardBack -= 1.0f;
        if (glfwGetKey(WINDOW_ID, GLFW_KEY_D) == GLFW_PRESS) rightLeft += 1.0f;
        if (glfwGetKey(WINDOW_ID, GLFW_KEY_A) == GLFW_PRESS) rightLeft -= 1.0f;
        if (glfwGetKey(WINDOW_ID, GLFW_KEY_SPACE) == GLFW_PRESS) upDown += 1.0f;
        if (glfwGetKey(WINDOW_ID, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) upDown -= 1.0f;

        if (glfwGetKey(WINDOW_ID, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
            speed = 1.5f;
        }

        CAMERA.updatePosition(forwardBack, rightLeft, upDown, speed, dt);
        CAMERA.tick();
    }
}
