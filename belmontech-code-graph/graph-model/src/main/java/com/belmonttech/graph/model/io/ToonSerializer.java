package com.belmonttech.graph.model.io;

import com.belmonttech.graph.model.*;

public class ToonSerializer implements ISerializer {

    @Override
    public GraphSnapshot deserialize(byte[] raw) throws Exception {
        return new GraphSnapshot(java.util.Map.of(), java.util.List.of());
    }

    public static String encodeSnapshot(GraphSnapshot s){
        return "(nodes []) (edges [])";
    }
}
