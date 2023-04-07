package com.artipie.scripting;

public interface PythonScript {
    String NAME = "python";

    static Script newScript(final String script) {
        return Script.newScript(NAME, script);
    }

    static Script newCompiledScript(final String script) {
        return Script.newCompiledScript(NAME, script);
    }
}
