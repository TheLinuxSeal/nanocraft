package org.sutormin.nanocraft.data.types;

import org.sutormin.nanocraft.data.registry.RegistryType;
import org.sutormin.nanocraft.resources.texture.Texture;
import org.sutormin.nanocraft.resources.texture.Textures;

public class Block extends RegistryType {

    /** Which render pass the block belongs to. */
    public enum RenderLayer { OPAQUE, CUTOUT, TRANSLUCENT, INVISIBLE }

    // ---- Shape & rendering ----
    private BlockShape shape;
    /**
     * Texture paths, one per face of the shape (face order is defined by the BlockShape).
     * A single entry means "use this texture on every face".
     */
    private String[] texturePaths = new String[0];
    /** Atlas indices matching texturePaths, filled in when the atlas is built. */
    private int[] textures = new int[0];
    private RenderLayer renderLayer = RenderLayer.OPAQUE;
    private int tintColor = 0xFFFFFF; // e.g. for grass / leaves

    // ---- Mining (client predicts break time / crack animation; server validates) ----
    private float hardness = 1.0f;         // < 0 means unbreakable
    private String effectiveTool = null;   // e.g. "pickaxe"; null = no tool speeds it up

    // ---- Client-side movement / collision prediction ----
    private boolean solid = true;
    private boolean liquid = false;
    private boolean climbable = false;
    private float friction = 0.6f;   // ice would be ~0.98
    private float speedFactor = 1.0f;

    // ---- Lighting ----
    private int lightEmission = 0;   // 0..15
    private int lightOpacity = 15;   // 0..15

    // ---- Presentation ----
    private String soundGroup = "stone";
    private String displayName = null;

    public Block(int id) { super(id); }

    @Override
    public Block copy(int newId) {
        Block b = new Block(newId);
        b.shape = shape;
        b.texturePaths = texturePaths.clone();
        b.textures = textures.clone();
        b.renderLayer = renderLayer;
        b.tintColor = tintColor;
        b.hardness = hardness;
        b.effectiveTool = effectiveTool;
        b.solid = solid;
        b.liquid = liquid;
        b.climbable = climbable;
        b.friction = friction;
        b.speedFactor = speedFactor;
        b.lightEmission = lightEmission;
        b.lightOpacity = lightOpacity;
        b.soundGroup = soundGroup;
        b.displayName = displayName;
        return b;
    }

    // ================= Shape =================
    public BlockShape getShape() { return shape; }
    public Block setShape(BlockShape shape) { this.shape = shape; return this; }

    // ================= Textures =================
    /** Same texture on every face, regardless of how many faces the shape has. */
    public Block setTexture(String path) {
        return setTextures(path);
    }

    /** One texture per face, in the face order of the shape. Any number of faces. */
    public Block setTextures(String... paths) {
        if (paths == null || paths.length == 0)
            throw new IllegalArgumentException("Block needs at least one texture");
        this.texturePaths = paths.clone();
        this.textures = new int[paths.length];
        for (int i = 0; i< paths.length; i++){
            this.textures[i] = Textures.BLOCK.addTexture("assets/texture/block/"+paths[i]+".png");
        }
        return this;
    }


    public int getTextureCount() { return texturePaths.length; }

    public String getTexturePath(int face) { return texturePaths[resolve(face, texturePaths.length)]; }

    public String[] getTexturePaths() { return texturePaths.clone(); }

    /** Called by the atlas builder: atlas index for each entry of getTexturePaths(). */
    public void setTextureIds(int... ids) {
        if (ids.length != texturePaths.length)
            throw new IllegalArgumentException("Expected " + texturePaths.length
                    + " texture ids, got " + ids.length);
        this.textures = ids.clone();
    }

    /** Atlas index of the texture for the given face. */
    public int getTexture(int which) {
        if (textures.length == 0) return Textures.BLOCK_NOT_FOUND;
        if (textures.length == 1) return textures[0];          // single texture applies to every face
        if (which < textures.length) return textures[which];
        return Textures.BLOCK_NOT_FOUND;                        // incomplete multi-face def, e.g. azalea
    }

    /** Direct access for the mesher; do not modify. */
    public int[] getTextures() { return textures; }

    private static int resolve(int face, int length) {
        if (length == 0) throw new IllegalStateException("Block has no textures set");
        if (length == 1) return 0;
        if (face < 0 || face >= length)
            throw new IllegalArgumentException("Invalid face " + face + " (block has " + length + " textures)");
        return face;
    }

    public RenderLayer getRenderLayer() { return renderLayer; }
    public Block setRenderLayer(RenderLayer layer) { this.renderLayer = layer; return this; }

    public int getTintColor() { return tintColor; }
    public Block setTintColor(int rgb) { this.tintColor = rgb & 0xFFFFFF; return this; }

    // ================= Mining =================
    public float getHardness() { return hardness; }
    public Block setHardness(float hardness) { this.hardness = hardness; return this; }
    public Block setUnbreakable() { this.hardness = -1f; return this; }
    public boolean isUnbreakable() { return hardness < 0; }
    /** True if the block breaks immediately (no mining packets needed beyond start). */
    public boolean isInstantBreak() { return hardness == 0f; }

    public String getEffectiveTool() { return effectiveTool; }
    public Block setEffectiveTool(String tool) { this.effectiveTool = tool; return this; }

    // ================= Movement / collision =================
    public boolean isSolid() { return solid; }
    public Block setSolid(boolean solid) { this.solid = solid; return this; }

    public boolean isLiquid() { return liquid; }
    public Block setLiquid(boolean liquid) { this.liquid = liquid; return this; }

    public boolean isClimbable() { return climbable; }
    public Block setClimbable(boolean c) { this.climbable = c; return this; }

    public float getFriction() { return friction; }
    public Block setFriction(float friction) { this.friction = friction; return this; }

    public float getSpeedFactor() { return speedFactor; }
    public Block setSpeedFactor(float f) { this.speedFactor = f; return this; }

    // ================= Lighting =================
    public int getLightEmission() { return lightEmission; }
    public Block setLightEmission(int level) { this.lightEmission = clampLight(level); return this; }

    public int getLightOpacity() { return lightOpacity; }
    public Block setLightOpacity(int opacity) { this.lightOpacity = clampLight(opacity); return this; }

    private static int clampLight(int v) { return Math.max(0, Math.min(15, v)); }

    /** Glass-like preset: see-through, no light blocking, cutout rendering. */
    public Block setTransparent() {
        this.lightOpacity = 0;
        this.renderLayer = RenderLayer.CUTOUT;
        return this;
    }

    public boolean isOpaque() { return renderLayer == RenderLayer.OPAQUE && lightOpacity >= 15; }

    // ================= Presentation =================
    public String getSoundGroup() { return soundGroup; }
    public Block setSoundGroup(String group) { this.soundGroup = group; return this; }

    public String getDisplayName() { return displayName; }
    public Block setDisplayName(String name) { this.displayName = name; return this; }
}