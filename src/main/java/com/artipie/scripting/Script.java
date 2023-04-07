package com.artipie.scripting;

import java.util.Map;
import java.util.Objects;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class Script {
    protected final ScriptEngine engine;
    protected String script;
    protected CompiledScript compiledScript;

    public Script(final String name) {
        this.engine = new ScriptEngineManager().getEngineByName(name);
    }

    public Script compile(final String script) throws ScriptException {
        this.script = script;
        if (this.engine instanceof Compilable) {
            this.compiledScript = ((Compilable) this.engine).compile(script);
        }
        return this;
    }

    public Object execute(final Map<String, Object> vars) throws ScriptException {
        Objects.requireNonNull(this.script, "Script is null");
        final Object res;
        if (this.engine instanceof Compilable && this.compiledScript != null) {
            res = this.compiledScript.eval(new SimpleBindings(vars));
        } else {
            res = this.engine.eval(this.script, new SimpleBindings(vars));
        }
        return res;
    }

    public Object execute(final String script, final Map<String, Object> variables)
        throws ScriptException {
        return this.engine.eval(script, new SimpleBindings(variables));
    }

    public Object variable(final String name) {
        return this.engine.get(name);
    }
}
