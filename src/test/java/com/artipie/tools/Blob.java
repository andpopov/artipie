package com.artipie.tools;

public class Blob {
    private final String classname;
    private final byte[] bytes;

    public Blob(final String classname, final byte[] bytes) {
        this.classname = classname;
        this.bytes = bytes;
    }

    public String classname() {
        return classname;
    }

    public byte[] bytes() {
        return bytes;
    }
}
