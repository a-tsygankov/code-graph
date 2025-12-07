package com.belmonttech.graph.model.progress;

public class ProgressReporter {
    private final ProgressListener l;
    private final String phase;
    private final long total;
    private long current;

    public ProgressReporter(String p,long t,ProgressListener l){
        this.phase=p; this.total=t;
        this.l=l!=null?l:new NoOpProgressListener();
    }

    public void step(){ current++; l.onProgress(phase,current,total); }
    public void step(long n){ current+=n; l.onProgress(phase,current,total); }
}
