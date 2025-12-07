package com.belmonttech.graph.model;

import tools.jackson.databind.*;

import java.io.File;

/**
 * Simple default Toon parser that assumes the Toon file is a JSON
 * serialization of {@link GraphSnapshot}. Replace this with a real
 * parser for your custom Toon format if needed.
 */
public class DefaultToonParser implements ToonGraphLoader {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public GraphSnapshot load(String toonFilePath) {
        try {
            return JSON.readValue(new File(toonFilePath), GraphSnapshot.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new GraphSnapshot(java.util.Map.of(), java.util.List.of());
        }
    }
}
