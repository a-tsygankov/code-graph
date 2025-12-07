package com.belmonttech.graph.model.io;

import com.belmonttech.graph.model.*;

import tools.jackson.databind.ObjectMapper;

public class JsonSerializer implements ISerializer {
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonSerializer() {
    }

    @Override
    public GraphSnapshot deserialize(byte[] raw) throws Exception {
        return mapper.readValue(raw, GraphSnapshot.class);
    }
}
