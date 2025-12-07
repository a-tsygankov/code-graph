package com.belmonttech.graph.backend.dto;

import com.belmonttech.graph.model.GraphEdge;
import com.belmonttech.graph.model.GraphNode;
import java.util.List;

public record GraphSliceDto(List<GraphNode> nodes, List<GraphEdge> edges) {}
