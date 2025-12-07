package com.belmonttech.graph.backend;

import com.belmonttech.graph.model.GraphSnapshot;
import com.belmonttech.graph.model.io.DataLoader;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class Application {

    @Autowired
    private GraphService graphService;
    @Autowired
    private DataLoader dataLoader;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void loadInitialSnapshot() {
        String pathProp = System.getProperty("toon.file");
        if (pathProp == null || pathProp.isBlank())
            return;
        try {
            Path path = Path.of(pathProp);
            if (!Files.exists(path)) {
                System.err.println("toon.file does not exist: " + path);
                return;
            }
            GraphSnapshot snapshot = dataLoader.load(path);
            graphService.updateSnapshot(snapshot);
            System.out.printf("Loaded initial snapshot from %s (nodes=%d, edges=%d)%n", path, snapshot.nodes().size(),
                    snapshot.edges().size());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Failed to load initial toon/graph file: " + ex.getMessage());
        }
    }
}
