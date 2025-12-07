# BelmontTech Code Graph – Core IO & Progress Subsystem

This module provides foundational components for loading, saving, and processing large-scale
source‑code graphs (200k+ nodes) using JSON or TOON formats. It includes:

- Graph snapshot model (`GraphSnapshot`, `GraphNode`, `GraphEdge`)
- Serialization system with pluggable formats (`JsonSerializer`, `ToonSerializer`)
- Central orchestration (`GraphIo`)
- Progress‑reporting subsystem (CLI/REST/SSE‑friendly)
- Extensible loader registry (`DataLoader`)

---

## 📂 Project Structure

```
graph-model/
  src/main/java/com/belmonttech/graph/model/
      GraphSnapshot.java
      GraphNode.java
      GraphEdge.java
      GraphIo.java

  src/main/java/com/belmonttech/graph/model/io/
      ISerializer.java
      JsonSerializer.java
      ToonSerializer.java
      DataLoader.java

  src/main/java/com/belonttech/graph/model/progress/
      ProgressListener.java
      ProgressReporter.java
      NoOpProgressListener.java
```

---

## 🎯 Goals

This module is designed to:

1. Load huge code-analysis graphs efficiently  
2. Support multiple serialization formats  
3. Allow streaming UI updates during heavy operations  
4. Be reusable across:
   - Code-analysis pipelines
   - Graph viewer UI
   - Static analysis tasks
   - Performance hot‑spot visualizers

---

## 🔌 Supported File Formats

| Format | Extension | Read | Write | Notes |
|-------|-----------|------|-------|-------|
| JSON  | `.json`   | ✔    | ✔     | Uses **Jackson 3.x (tools.jackson)** |
| TOON  | `.toon`   | ✔(placeholder) | ✔     | Stub only—awaiting full JToon schema |

Unsupported extensions throw:

```
IllegalArgumentException("Unsupported graph format")
```

---

## 🚀 Usage Examples

### Load a graph

```java
GraphIo io = new GraphIo();
GraphSnapshot snapshot = io.load(Path.of("graph.json"));
```

### Save a graph

```java
io.save(Path.of("out.graph.json"), snapshot);
```

### Register a custom serializer

```java
DataLoader dl = io.getLoader();
dl.register("bin", new CustomBinarySerializer());
```

---

## 📡 Progress Reporting

Use `ProgressReporter` inside heavy loops:

```java
ProgressReporter pr = new ProgressReporter("ParsingNodes", totalNodes, listener);

for (...) {
    pr.step();        // +1
    pr.step(16);      // +16 bytes etc.
}
```

Implement a listener:

```java
class ConsoleListener implements ProgressListener {
    @Override
    public void onProgress(String phase, long cur, long total) {
        System.out.printf("%s: %.2f%%%n", phase, 100.0 * cur / total);
    }
}
```

---

## 🧩 Integration with Spring Boot

Recommended REST SSE progress endpoint:

```java
@GetMapping(value = "/progress", produces = TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> stream() { ... }
```

Backend operations accept `ProgressListener` for real‑time UI feedback.

---

## 🛠 Requirements

- **Java 21+**
- **Jackson 3.x (`tools.jackson.*`)**
- **Gradle 8.x**

---

## 🧬 Extension Ideas

- Implement full TOON parsing using JToon
- Add binary/protobuf serializers
- Add streaming parsers for files over 200 MB
- Add graph analysis modules (hotspots, cycles, dependency clusters)

---

## 📝 License

Proprietary © BelmontTech

---

## 📣 Support

For architecture help or questions, contact your project maintainer.
