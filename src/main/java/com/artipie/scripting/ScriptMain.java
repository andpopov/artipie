package com.artipie.scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptMain {
    public static void main(String[] args) throws ScriptException {
        final ScriptEngineManager factory = new ScriptEngineManager();
        final ScriptEngine engine = factory.getEngineByName("ruby");
        engine.eval("puts 'Hello!'");
        System.out.println();

    }
}
