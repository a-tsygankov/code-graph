package com.belmonttech.graph.model;

import com.belmonttech.graph.model.io.*;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

public class GraphIo {
    private final DataLoader loader;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public GraphIo() {
        this.loader = new DataLoader();
        loader.registerSerializer("json", new JsonSerializer());
        loader.registerSerializer("toon", new ToonSerializer());
    }

    public GraphSnapshot load(Path file) throws Exception {
        String ext = getExtension(file);
        ISerializer s = loader.getSerializer(ext);
        if (s == null)
            throw new IllegalArgumentException("Unsupported format: " + ext);
        return s.deserialize(Files.readAllBytes(file));
    }

    public void save(Path file, GraphSnapshot s) throws IOException {
        String ext = getExtension(file);
        if ("json".equals(ext)) {
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), s);
        } else if ("toon".equals(ext)) {
            Files.writeString(file, ToonSerializer.encodeSnapshot(s));
        } else
            throw new IllegalArgumentException("Unsupported output: " + ext);
    }

    private static String getExtension(Path f) {
        String n = f.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i == -1 ? "" : n.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
