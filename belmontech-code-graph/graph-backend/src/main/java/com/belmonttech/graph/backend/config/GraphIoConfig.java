package com.belmonttech.graph.backend.config;

import com.belmonttech.graph.model.io.DataLoader;
import com.belmonttech.graph.model.io.JsonSerializer;
import com.belmonttech.graph.model.io.ToonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphIoConfig {
  @Bean public DataLoader dataLoader() {
    DataLoader loader = new DataLoader();
    loader.registerSerializer("json", new JsonSerializer());
    loader.registerSerializer("toon", new ToonSerializer());
    return loader; }
}
