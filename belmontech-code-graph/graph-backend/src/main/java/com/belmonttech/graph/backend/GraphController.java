package com.belmonttech.graph.backend;

import java.nio.file.Path;
import java.util.Objects;

import com.belmonttech.graph.backend.dto.GraphSliceDto;
import com.belmonttech.graph.model.GraphSnapshot;
import com.belmonttech.graph.model.io.DataLoader;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/graph")
@CrossOrigin
public class GraphController {

    private final GraphService service;
    private final DataLoader dataLoader;

    public GraphController(GraphService service, DataLoader dataLoader) {
        this.service = service;
        this.dataLoader = dataLoader;
    }

    @GetMapping("/packages") public GraphSliceDto getPackages() {
        return service.getPackageLevelGraph();
    }

    @GetMapping("/packages/{pkgName}/classes") public GraphSliceDto getClasses(@PathVariable String pkgName) {
        return service.getClassesInPackage(pkgName);
    }

    @GetMapping("/nodes/{nodeId}/neighborhood") public GraphSliceDto getNeighborhood(@PathVariable String nodeId,
            @RequestParam(defaultValue = "2") int depth) {
        return service.getNeighborhood(nodeId, depth);
    }

    @GetMapping("/hotspots") public GraphSliceDto getHotspots(@RequestParam(defaultValue = "100") int limit) {
        return service.getHotspots(limit);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body("Empty file");
        try {
            GraphSnapshot snapshot = dataLoader.load(Path.of(Objects.requireNonNull(file.getOriginalFilename())));
            service.updateSnapshot(snapshot);
            return ResponseEntity.ok(
                    "Graph snapshot updated. Nodes: " + snapshot.nodes().size() + ", edges: " + snapshot.edges()
                            .size());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to parse uploaded file: " + ex.getMessage());
        }
    }
}
