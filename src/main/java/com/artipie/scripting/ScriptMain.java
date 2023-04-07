package com.artipie.scripting;

import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;

public class ScriptMain {
    public static void main(String[] args) throws ScriptException {
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
}
