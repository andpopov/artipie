package com.artipie.scripting;

import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;

public class ScriptMain {
    public static void main(String[] args) throws ScriptException {
        GroovyScript script = new GroovyScript();
        script
            .compile("println(1)")
            .call();

        script.call("println(2)");

        script
            .compile("println(a)")
            .call(Map.of("a", 3));

        script.call("println(a)", Map.of("a", 4));

        System.out.println(
            script.call("return a*2", Map.of("a", 5))
        );

        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 6);
        script.call("a = a * 2", variables);
        System.out.println(script.variable("a"));
    }
}
