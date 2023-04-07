/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GroovyScript}.
 *
 * @since 0.28
 */
public class MvelScriptTest {
    @Test
    public void standardScript() throws ScriptException {
        MvelScript.newScript("System.out.println(1)").call();
        MvelScript.newScript("System.out.println(a)").call(Map.of("a", 2));
        System.out.println(
            MvelScript.newScript("a * 2").call(Map.of("a", 3)).value()
        );
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        System.out.println(
            MvelScript.newScript("a = a * 3").call(variables).variable("a")
        );
    }

    @Test
    public void precompiledScript() throws ScriptException {
        MvelScript.newCompiledScript("System.out.println(1)").call();
        MvelScript.newCompiledScript("System.out.println(a)").call(Map.of("a", 2));
        System.out.println(
            MvelScript.newCompiledScript("a * 2").call(Map.of("a", 3)).value()
        );
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        System.out.println(
            MvelScript.newCompiledScript("a = a * 3").call(variables).variable("a")
        );
    }
}
