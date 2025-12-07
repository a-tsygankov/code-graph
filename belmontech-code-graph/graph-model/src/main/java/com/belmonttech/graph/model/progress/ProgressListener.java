package com.belmonttech.graph.model.progress;

public interface ProgressListener {
    void onProgress(String phase,long current,long total);
}
