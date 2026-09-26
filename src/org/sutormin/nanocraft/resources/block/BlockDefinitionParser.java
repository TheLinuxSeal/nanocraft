package org.sutormin.nanocraft.resources.block;

import org.sutormin.nanocraft.Main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BlockDefinitionParser {
    public record BlockDefinition(String shape, String[] tex){}
    private static final Map<String, BlockDefinition> defs = new HashMap<>();
    public static void loadFromIndex(){
        try (InputStream in = Main.class.getResourceAsStream(
                "/assets/indexes/blkdef.idx")) {

            if (in == null) {
                throw new RuntimeException("Resource /assets/indexes/blkdef.idx not found");
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

                defs.put(
                        data[0],
                        new BlockDefinition(
                                data[1],
                                Arrays.copyOfRange(data, 2, data.length)
                        )
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static BlockDefinition get(String name){
        BlockDefinition def = defs.get(name);

        if (def != null) {
            return def;
        }
        else {
            BlockDefinition d = defs.get("@default");
            if (d == null) {
                throw new RuntimeException(
                        "Block definition '" + name + "' not found and no @default is defined"
                );
            }
            String newShape = d.shape.replace("*", name);
            String[] newTex = new String[d.tex.length];
            for (int i = 0; i < d.tex.length; i++) {
                newTex[i] = d.tex[i].replace("*", name);
            }
            return new BlockDefinition(newShape,newTex);
        }
    }
    public static String[] getTexture(String name){
        return get(name).tex();
    }
}
