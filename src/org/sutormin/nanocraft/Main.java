package org.sutormin.nanocraft;

import java.nio.file.Path;

public class Main {
    static void main() {
        Options.load(Path.of("options.txt")); // before anything reads the options
        new NanoCraft().run();
    }
}
