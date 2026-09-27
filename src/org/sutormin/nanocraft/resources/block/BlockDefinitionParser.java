package org.sutormin.nanocraft.resources.block;

import org.sutormin.nanocraft.Main;
import org.sutormin.nanocraft.data.types.Block;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockDefinitionParser {
    /**
     * One .def line: {@code <name> <shape> <textures...> [options]}. Options are key=value tokens
     * after the textures; the only one so far is {@code rotate=random|random_all}.
     */
    public record BlockDefinition(String shape, String[] tex, Block.TextureRotation rotation){}

    /**
     * A definition for some states of a block, e.g. "oak_door[half=upper]" or "carrots[age=0|1]".
     * Matches any state whose listed properties have one of the listed values; other properties are ignored.
     */
    private record StatePattern(Map<String, String[]> props, BlockDefinition def){
        boolean matches(Map<String, String> state) {
            for (Map.Entry<String, String[]> e : props.entrySet()) {
                String value = state.get(e.getKey());
                if (value == null || !Arrays.asList(e.getValue()).contains(value)) return false;
            }
            return true;
        }
    }

    // definitions without a state tag, keyed by block name (and "@default")
    private static final Map<String, BlockDefinition> defs = new HashMap<>();
    // definitions with a state tag, keyed by base block name, in file order
    private static final Map<String, List<StatePattern>> patterns = new HashMap<>();
    public static void loadFromIndex(){
        try (InputStream in = Main.class.getResourceAsStream(
                "/assets/indexes/def.idx")) {

            if (in == null) {
                throw new RuntimeException("Resource /assets/indexes/def.idx not found");
            }

            String strs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String str : strs.split("\\R")) {
                str = str.trim();

                if (str.isEmpty()) {continue;}

                if (str.startsWith("//")){continue;}

                if (str.startsWith("#")){continue;}

                loadFromFile(str);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void loadFromFile(String file){
        try (InputStream in = Main.class.getResourceAsStream(
                "/assets/"+file)) {

            if (in == null) {
                throw new RuntimeException("Resource not found");
            }

            String strs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String str : strs.split("\\R")) {
                str = str.trim();

                if (str.isEmpty() || str.startsWith("//") || str.startsWith("#")) {
                    continue;
                }

                String[] data = str.split("\\s+");

                List<String> textures = new ArrayList<>();
                Block.TextureRotation rotation = Block.TextureRotation.NONE;
                for (String token : Arrays.copyOfRange(data, 2, data.length)) {
                    if (!token.contains("=")) {
                        textures.add(token);
                    } else if (token.equals("rotate=random")) {
                        rotation = Block.TextureRotation.RANDOM_TOP_BOTTOM;
                    } else if (token.equals("rotate=random_all")) {
                        rotation = Block.TextureRotation.RANDOM_ALL;
                    } else {
                        throw new RuntimeException("Unknown option '" + token + "' in " + file + ": " + str);
                    }
                }

                BlockDefinition def = new BlockDefinition(data[1], textures.toArray(new String[0]), rotation);

                int bracket = data[0].indexOf('[');
                if (bracket < 0) {
                    defs.put(data[0], def);
                } else {
                    Map<String, String[]> props = new HashMap<>();
                    for (Map.Entry<String, String> e : parseState(data[0].substring(bracket)).entrySet()) {
                        props.put(e.getKey(), e.getValue().split("\\|"));
                    }
                    patterns.computeIfAbsent(data[0].substring(0, bracket), k -> new ArrayList<>())
                            .add(new StatePattern(props, def));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /** "[facing=north,half=upper]" -> {facing=north, half=upper} */
    private static Map<String, String> parseState(String tag) {
        Map<String, String> state = new HashMap<>();
        String inner = tag.substring(1, tag.length() - 1);
        if (inner.isEmpty()) return state;
        for (String kv : inner.split(",")) {
            int eq = kv.indexOf('=');
            if (eq < 0) throw new RuntimeException("Invalid block state property '" + kv + "' in " + tag);
            state.put(kv.substring(0, eq), kv.substring(eq + 1));
        }
        return state;
    }

    /**
     * Looks up the definition for a block state, e.g. "oak_door[facing=north,half=upper,...]":
     * the matching state pattern with the most properties ("oak_door[half=upper]"; ties go to the
     * one defined first), then the base block name ("oak_door"), then @default.
     */
    public static BlockDefinition get(String name){
        // * = base block name ("grass_block"), ^ = state tag ("[snowy=true]", or "" if stateless)
        int bracket = name.indexOf('[');
        String base = bracket < 0 ? name : name.substring(0, bracket);
        String state = bracket < 0 ? "" : name.substring(bracket);

        BlockDefinition d = null;
        List<StatePattern> candidates = patterns.get(base);
        if (candidates != null && bracket >= 0) {
            Map<String, String> props = parseState(state);
            int best = -1;
            for (StatePattern p : candidates) {
                if (p.props().size() > best && p.matches(props)) {
                    d = p.def();
                    best = p.props().size();
                }
            }
        }
        if (d == null) d = defs.get(base);
        if (d == null) d = defs.get("@default");
        if (d == null) {
            throw new RuntimeException(
                    "Block definition '" + name + "' not found and no @default is defined"
            );
        }

        String newShape = d.shape.replace("*", base).replace("^", state);
        String[] newTex = new String[d.tex.length];
        for (int i = 0; i < d.tex.length; i++) {
            newTex[i] = d.tex[i].replace("*", base).replace("^", state);
        }
        return new BlockDefinition(newShape, newTex, d.rotation());
    }
    public static String[] getTexture(String name){
        return get(name).tex();
    }
}
