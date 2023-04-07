package com.artipie.scripting;

import java.util.Map;
import java.util.Objects;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
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

    public Object call(final Map<String, Object> vars) throws ScriptException {
        Objects.requireNonNull(this.script, "Script is null");
        final SimpleBindings bindings = new SimpleBindings(vars);
        this.engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        final Object res;
        if (this.engine instanceof Compilable && this.compiledScript != null) {
            res = this.compiledScript.eval(bindings);
        } else {
            res = this.engine.eval(this.script);
        }
        return res;
    }

    public Object call() throws ScriptException {
        Objects.requireNonNull(this.script, "Script is null");
        final SimpleBindings bindings = new SimpleBindings();
        this.engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        final Object res;
        if (this.engine instanceof Compilable && this.compiledScript != null) {
            res = this.compiledScript.eval(bindings);
        } else {
            res = this.engine.eval(this.script);
        }
        return res;
    }

    public Object call(final String script, final Map<String, Object> variables)
        throws ScriptException {
        this.engine.setBindings(new SimpleBindings(variables), ScriptContext.ENGINE_SCOPE);
        return this.engine.eval(script);
    }

    public Object call(final String script)
        throws ScriptException {
        this.engine.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        return this.engine.eval(script);
    }

    public Object variable(final String name) {
        return this.engine.getBindings(ScriptContext.ENGINE_SCOPE).get(name);
//        return this.engine.get(name);
    }
}
