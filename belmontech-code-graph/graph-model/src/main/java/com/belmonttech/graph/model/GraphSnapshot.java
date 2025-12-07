package com.belmonttech.graph.model;

import java.util.List;
import java.util.Map;

public record GraphSnapshot(
        Map<String, GraphNode> nodes,
        List<GraphEdge> edges
) {
}
