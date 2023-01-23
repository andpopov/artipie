package com.artipie.tools;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Classloader of dynamically compiled classes.
 */
public class CodeClassLoader extends ClassLoader {
    /**
     * Code blobs.
     */
    private final Map<String, CodeBlob> blobs = new TreeMap<>();

    /**
     * Ctor.
     */
    public CodeClassLoader() {
        super();
    }

    /**
     * Ctor.
     * @param parent Parent class loader.
     */
    public CodeClassLoader(final ClassLoader parent) {
        super(parent);
    }

    /**
     * Adds code blobs.
     * @param blobs Code blobs.
     */
    public void addBlobs(CodeBlob... blobs) {
        this.addBlobs(List.of(blobs));
    }

    /**
     * Adds code blobs.
     * @param blobs Code blobs.
     */
    public void addBlobs(List<CodeBlob> blobs) {
        blobs.forEach(blob -> this.blobs.put(blob.classname(), blob));
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (this.blobs.containsKey(name)) {
            final byte[] code = this.blobs.get(name).bytes();
            return defineClass(name, code, 0, code.length);
        } else {
            return super.findClass(name);
        }
    }
}
