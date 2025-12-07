package com.belmonttech.graph.model.progress;

public class NoOpProgressListener implements ProgressListener{
    @Override public void onProgress(String p,long c,long t){}
}
