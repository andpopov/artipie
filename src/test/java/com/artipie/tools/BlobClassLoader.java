package com.artipie.tools;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BlobClassLoader extends ClassLoader {
    private final Map<String, Blob> blobs = new TreeMap<String, Blob>();

    public BlobClassLoader() {
    }

    public void addBlobs(Blob... blobs) {
        this.addBlobs(List.of(blobs));
    }

    public void addBlobs(List<Blob> blobs) {
        blobs.forEach(blob -> this.blobs.put(blob.classname(), blob));
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (blobs.containsKey(name)) {
            final byte[] code = blobs.get(name).bytes();
            return defineClass(name, code, 0, code.length);
        } else {
            return super.findClass(name);
        }
    }
}
