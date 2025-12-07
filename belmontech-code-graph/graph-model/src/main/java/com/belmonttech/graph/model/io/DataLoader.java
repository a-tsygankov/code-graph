package com.belmonttech.graph.model.io;

import com.belmonttech.graph.model.GraphSnapshot;

import java.nio.file.*;
import java.util.*;

import org.apache.commons.io.FilenameUtils;

public class DataLoader {
    private final Map<String, ISerializer> serializers = new HashMap<>();

    public void registerSerializer(String ext, ISerializer s) {
        serializers.put(ext.toLowerCase(), s);
    }

    public ISerializer getSerializer(String ext) {
        ISerializer serializer = serializers.get(ext.toLowerCase());
        return serializer;
    }

    public GraphSnapshot load(Path filePath) throws Exception {
        byte[] raw = Files.readAllBytes(filePath);
        ISerializer serializer = getSerializer(FilenameUtils.getExtension(filePath.toString()));
        return serializer.deserialize(raw);
    }
}
