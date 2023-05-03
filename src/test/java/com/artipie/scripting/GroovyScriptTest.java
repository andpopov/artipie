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
 * @since 0.30
 */
public class GroovyScriptTest {
    @Test
    public void standardScript() throws ScriptException {
        GroovyScript.newScript("println(1)").call();
        GroovyScript.newScript("println(a)").call(Map.of("a", 2));
        System.out.println(
            GroovyScript.newScript("a * 2").call(Map.of("a", 3)).value()
        );
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        System.out.println(
            GroovyScript.newScript("a = a * 3").call(variables).variable("a")
        );
    }

    @Test
    public void precompiledScript() throws ScriptException {
        GroovyScript.newCompiledScript("println(1)").call();
        GroovyScript.newCompiledScript("println(a)").call(Map.of("a", 2));
        System.out.println(
            GroovyScript.newCompiledScript("a * 2").call(Map.of("a", 3)).value()
        );
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        System.out.println(
            GroovyScript.newCompiledScript("a = a * 3").call(variables).variable("a")
        );
    }
}
