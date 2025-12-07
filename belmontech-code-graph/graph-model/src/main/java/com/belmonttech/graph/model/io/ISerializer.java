package com.belmonttech.graph.model.io;

import com.belmonttech.graph.model.GraphSnapshot;

public interface ISerializer {
    GraphSnapshot deserialize(byte[] raw) throws Exception;
}
