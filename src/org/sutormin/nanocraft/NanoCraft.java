package org.sutormin.nanocraft;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.util.List;
import java.util.ArrayList;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class NanoCraft {
  private long window;
  private final int width = 1024;
  private final int height = 576;

  private Texture texture;
  private Shader shader;
  private World world;
  private final Camera camera = new Camera();

  private double lastMouseX = width / 2.0;
  private double lastMouseY = height / 2.0;
  private boolean firstMouse = true;

  private ChunkPos lastCameraChunkPos = null;

  private static final String VERTEX_SHADER = """
    #version 330 core
    layout (location = 0) in vec3 aPos;
    layout (location = 1) in vec2 aTexCoord;

    out vec2 TexCoord;
    out vec3 FragPosView;

    uniform mat4 uProjection;
    uniform mat4 uView;

    void main() {
        vec4 viewPos = uView * vec4(aPos, 1.0);
        FragPosView = viewPos.xyz;
        gl_Position = uProjection * viewPos;
        TexCoord = aTexCoord;
    }
  """;

  private static final String FRAGMENT_SHADER = """
    #version 330 core
    in vec2 TexCoord;
    in vec3 FragPosView;
    out vec4 FragColor;

    uniform sampler2D uTexture;
    uniform vec3 uFogColor;
    uniform float uFogNear;
    uniform float uFogFar;

    void main() {
        vec4 texColor = texture(uTexture, TexCoord);
        if (texColor.a < 0.1) discard; // Prevent blending clear cut outlines

        // Distance from camera in view space
        float dist = length(FragPosView);
        
        // Linear fog interpolation factor (0.0 = full fog, 1.0 = full texture)
        float fogFactor = clamp((uFogFar - dist) / (uFogFar - uFogNear), 0.0, 1.0);

        vec3 finalColor = mix(uFogColor, texColor.rgb, fogFactor);
        FragColor = vec4(finalColor, texColor.a);
    }
  """;

  public void run() {
    init();
    loop();
    cleanup();
  }

  private void init() {
    GLFWErrorCallback.createPrint(System.err).set();
    if (!glfwInit()) throw new RuntimeException("[ERROR] failed to initialize GLFW");

    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

    window = glfwCreateWindow(width, height, "NanoCraft", NULL, NULL);
    if (window == NULL) throw new RuntimeException("[ERROR] failed to create GLFW window");

    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    /*glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
      if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
        // unimp
      }
      if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
        // unimp
      }
    });*/
    glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
      if (firstMouse) {
        lastMouseX = xpos;
        lastMouseY = ypos;
        firstMouse = false;
      }
      float xOffset = (float) (xpos - lastMouseX);
      float yOffset = (float) (ypos - lastMouseY);
      lastMouseX = xpos;
      lastMouseY = ypos;

      camera.processMouseInput(xOffset, yOffset);
    });

    glfwMakeContextCurrent(window);
    glfwSwapInterval(1);
    glfwShowWindow(window);

    GL.createCapabilities();
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glClearColor(0.623f, 0.734f, 0.785f, 1.0f);

    texture = new Texture("res/atlas.png");

    shader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
    shader.createUniform("uProjection");
    shader.createUniform("uView");
    shader.createUniform("uFogColor");
    shader.createUniform("uFogNear");
    shader.createUniform("uFogFar");

    world = new World();
  }

  private void loop() {
    Matrix4f projection = new Matrix4f().perspective(
      (float) Math.toRadians(60.0f), (float) width / height, 0.1f, 1000.0f
    );

    long lastTime = System.nanoTime();
    while (!glfwWindowShouldClose(window)) {
      long now = System.nanoTime();
      float deltaTime = (now - lastTime) / 1000000000.0f;
      lastTime = now;

      ChunkPos currentChunkPos = camera.getChunkPos();
      if (!currentChunkPos.equals(lastCameraChunkPos)) {
        world.loadChunksAndUnloadAllOtherChunks(getChunksInRenderDistance(currentChunkPos, 8));
        lastCameraChunkPos = currentChunkPos;
      }

      processInput(deltaTime);

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

      texture.bind();
      shader.bind();
      shader.setUniform("uProjection", projection);
      shader.setUniform("uView", camera.getViewMatrix());
      shader.setUniform("uFogColor", new org.joml.Vector3f(0.623f, 0.734f, 0.785f));
      shader.setUniform("uFogNear", 80.0f);
      shader.setUniform("uFogFar", 120.0f);

      world.render();

      glfwSwapBuffers(window);
      glfwPollEvents();
    }
  }

  private void processInput(float dt) {
    if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
      glfwSetWindowShouldClose(window, true);
    }

    float forwardBack = 0.0f;
    float rightLeft = 0.0f;
    float upDown = 0.0f;
    float speed = 10.0f;

    if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) forwardBack += 1.0f;
    if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) forwardBack -= 1.0f;
    if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) rightLeft += 1.0f;
    if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) rightLeft -= 1.0f;
    if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) upDown += 1.0f;
    if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) upDown -= 1.0f;

    if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
      speed = 15.0f;
    }

    camera.updatePosition(forwardBack, rightLeft, upDown, speed, dt);
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
    texture.cleanup();
    world.cleanup();
    shader.cleanup();

    glfwFreeCallbacks(window);
    glfwDestroyWindow(window);
    glfwTerminate();
    glfwSetErrorCallback(null).free();
  }
}