package com.belmonttech.graph.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GraphNode(
        String id,
        NodeLevel level,
        String displayName,
        String packageName,
        String className,
        String methodName,
        String signature,
        int cost,
        int complexity,
        boolean hotspot
) {
}
