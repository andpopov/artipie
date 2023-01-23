package com.artipie.tools;

import java.util.Objects;

/**
 * Class stores classname and it's compiled byte code.
 */
public class CodeBlob {
    /**
     * Class name of class.
     * It is used by class loader as classname.
     */
    private final String classname;

    /**
     * Byte code of class.
     */
    private final byte[] bytes;

    /**
     * Ctor.
     * @param classname Class name of class.
     * @param bytes Byte code of class
     */
    public CodeBlob(final String classname, final byte[] bytes) {
        this.classname = classname;
        this.bytes = bytes;
    }

    /**
     * Class name of class.
     * @return Class name.
     */
    public String classname() {
        return classname;
    }

    /**
     * Byte code of class.
     * @return Byte code.
     */
    public byte[] bytes() {
        return bytes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CodeBlob codeBlob = (CodeBlob) o;
        return Objects.equals(classname, codeBlob.classname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classname);
    }
}
