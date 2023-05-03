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
 * @checkstyle MagicNumberCheck (500 lines)
 */
public class RubyScriptTest {
    @Test
    public void standardScript() throws ScriptException {
        final Map<String, Object> variables = new HashMap<>();
        RubyScript.newScript("puts 1").call();
        variables.put("a", 2);
        RubyScript.newScript("puts a").call(variables);
        variables.put("a", 3);
        System.out.println(
            RubyScript.newScript("a * 2").call(variables).value()
        );
        variables.put("a", 4);
        System.out.println(
            RubyScript.newScript("a = a * 3").call(variables).variable("a")
        );
    }

    @Test
    public void precompiledScript() throws ScriptException {
        final Map<String, Object> variables = new HashMap<>();
        RubyScript.newCompiledScript("puts 1").call();
        variables.put("a", 2);
        RubyScript.newCompiledScript("puts a").call(variables);
        variables.put("a", 3);
        System.out.println(
            RubyScript.newCompiledScript("a * 2").call(variables).value()
        );
        variables.put("a", 4);
        System.out.println(
            RubyScript.newCompiledScript("a = a * 3").call(variables).variable("a")
        );
    }
}
