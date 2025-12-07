package com.belmonttech.graph.model;

public record GraphEdge(
        String id,
        String sourceId,
        String targetId,
        EdgeType type,
        int weight
) {
}
