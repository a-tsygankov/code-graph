package com.belmonttech.graph.backend;

import com.belmonttech.graph.backend.dto.GraphSliceDto;
import com.belmonttech.graph.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphService {

  private volatile GraphSnapshot snapshot = new GraphSnapshot(Map.of(), List.of());

  public void updateSnapshot(GraphSnapshot newSnapshot) {
    this.snapshot = (newSnapshot == null) ? new GraphSnapshot(Map.of(), List.of()) : newSnapshot;
  }

  public GraphSliceDto getPackageLevelGraph() {
    GraphSnapshot current = snapshot;
    Map<String, GraphNode> allNodes = current.nodes();
    Map<String, Aggregation> packages = new HashMap<>();
    for (GraphNode node : allNodes.values()) {
      String pkg = node.packageName();
      if (pkg == null || pkg.isBlank()) continue;
      Aggregation agg = packages.computeIfAbsent(pkg, k -> new Aggregation());
      agg.cost += node.cost();
      agg.complexity += node.complexity();
      agg.count++;
    }
    List<GraphNode> packageNodes = new ArrayList<>();
    for (var e : packages.entrySet()) {
      String pkg = e.getKey(); Aggregation agg = e.getValue();
      int avgCost = agg.count == 0 ? 0 : agg.cost / agg.count;
      boolean hotspot = avgCost >= CostLevel.HIGH.code();
      packageNodes.add(new GraphNode("pkg:"+pkg, NodeLevel.PACKAGE, pkg, pkg, null, null, null, avgCost, agg.complexity, hotspot));
    }
    Map<String,Integer> edgeAgg = new HashMap<>();
    for (GraphEdge edge : current.edges()) {
      if (edge.type() != EdgeType.INVOCATION) continue;
      GraphNode src = allNodes.get(edge.sourceId());
      GraphNode tgt = allNodes.get(edge.targetId());
      if (src == null || tgt == null) continue;
      String sp = src.packageName(); String tp = tgt.packageName();
      if (sp == null || tp == null || sp.equals(tp)) continue;
      String key = sp+"->"+tp;
      edgeAgg.merge(key, edge.weight(), Integer::sum);
    }
    List<GraphEdge> packageEdges = new ArrayList<>(); int idx = 0;
    for (var e : edgeAgg.entrySet()) {
      String[] parts = e.getKey().split("->"); if (parts.length!=2) continue;
      packageEdges.add(new GraphEdge("pkgEdge:"+(idx++), "pkg:"+parts[0], "pkg:"+parts[1], EdgeType.INVOCATION, e.getValue()));
    }
    return new GraphSliceDto(packageNodes, packageEdges);
  }

  public GraphSliceDto getClassesInPackage(String pkgName) {
    GraphSnapshot current = snapshot;
    List<GraphNode> classNodes = current.nodes().values().stream()
      .filter(n -> (n.level()==NodeLevel.CLASS || n.level()==NodeLevel.INTERFACE) && pkgName.equals(n.packageName()))
      .collect(Collectors.toList());
    Set<String> ids = classNodes.stream().map(GraphNode::id).collect(Collectors.toSet());
    List<GraphEdge> edges = current.edges().stream()
      .filter(e -> ids.contains(e.sourceId()) && ids.contains(e.targetId()))
      .collect(Collectors.toList());
    return new GraphSliceDto(classNodes, edges);
  }

  public GraphSliceDto getNeighborhood(String nodeId, int depth) {
    GraphSnapshot current = snapshot;
    if (!current.nodes().containsKey(nodeId)) return new GraphSliceDto(List.of(), List.of());
    Map<String,GraphNode> allNodes = current.nodes();
    List<GraphEdge> allEdges = current.edges();
    Set<String> visited = new HashSet<>(); visited.add(nodeId);
    Deque<String> q = new ArrayDeque<>(); q.add(nodeId);
    int maxNodes = 5000;
    for (int d=0; d<depth; d++) {
      int sz = q.size();
      for (int i=0;i<sz;i++) {
        String cur = q.poll(); if (cur==null) continue;
        for (GraphEdge e : allEdges) {
          if (e.sourceId().equals(cur) || e.targetId().equals(cur)) {
            String other = e.sourceId().equals(cur)?e.targetId():e.sourceId();
            if (visited.add(other)) { q.add(other); if (visited.size()>=maxNodes) break; }
          }
        }
        if (visited.size()>=maxNodes) break;
      }
      if (visited.size()>=maxNodes) break;
    }
    List<GraphNode> nodes = visited.stream().map(allNodes::get).filter(Objects::nonNull).collect(Collectors.toList());
    List<GraphEdge> edges = allEdges.stream().filter(e -> visited.contains(e.sourceId()) && visited.contains(e.targetId())).collect(Collectors.toList());
    return new GraphSliceDto(nodes, edges);
  }

  public GraphSliceDto getHotspots(int limit) {
    GraphSnapshot current = snapshot;
    List<GraphNode> sorted = current.nodes().values().stream()
      .sorted(Comparator.comparingInt(GraphNode::cost).reversed().thenComparingInt(GraphNode::complexity).reversed())
      .limit(limit).toList();
    Set<String> ids = sorted.stream().map(GraphNode::id).collect(Collectors.toSet());
    List<GraphEdge> edges = current.edges().stream().filter(e -> ids.contains(e.sourceId()) || ids.contains(e.targetId())).toList();
    return new GraphSliceDto(sorted, edges);
  }

  private static class Aggregation { int cost, complexity, count; }
}
