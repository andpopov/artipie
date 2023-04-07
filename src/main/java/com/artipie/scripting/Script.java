package com.artipie.scripting;

import com.artipie.ArtipieException;
import java.util.Map;
import java.util.Objects;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

public interface Script {
    ScriptEngineManager manager = new ScriptEngineManager();

    Result call() throws ScriptException;

    Result call(final Map<String, Object> vars) throws ScriptException;

    static Script newScript(final String name, final String script) {
        final ScriptEngine engine = manager.getEngineByName(name);
        return new StandardScript(engine, script);
    }

    static Script newCompiledScript(final String name, final String script) {
        final ScriptEngine engine = manager.getEngineByName(name);
        return new PrecompiledScript(engine, script);
    }

    class StandardScript implements Script {
        private final ScriptEngine engine;
        private final String script;

        public StandardScript(final ScriptEngine engine, final String script) {
            this.engine = engine;
            this.script = Objects.requireNonNull(script, "Script is null");
        }

        @Override
        public Result call() throws ScriptException {
            final Result result = new Result();
            result.value(this.engine.eval(this.script, result.context()));
            return result;
        }

        @Override
        public Result call(final Map<String, Object> vars) throws ScriptException {
            final Result result = new Result(vars);
            result.value(this.engine.eval(this.script, result.context()));
            return result;
        }
    }

    class PrecompiledScript implements Script {
        private final CompiledScript script;

        public PrecompiledScript(final ScriptEngine engine, final String script) {
            if (!(engine instanceof Compilable)) {
                throw new ArtipieException(
                    String.format("Scripting engine '%s' does not support compilation", engine)
                );
            }
            try {
                this.script = ((Compilable) engine).compile(script);
            } catch (ScriptException exc) {
                throw new ArtipieException(exc);
            }
        }

        @Override
        public Result call() throws ScriptException {
            final Result result = new Result();
            result.value(this.script.eval(result.context()));
            return result;
        }

        public Result call(final Map<String, Object> vars) throws ScriptException {
            final Result result = new Result(vars);
            result.value(this.script.eval(result.context()));
            return result;
        }
    }

    class Result {
        private final ScriptContext context;
        private Object value;

        public Result() {
            this.context = new SimpleScriptContext();
        }

        public Result(final Map<String, Object> vars) {
            this();
            context.setBindings(new SimpleBindings(vars), ScriptContext.ENGINE_SCOPE);
        }

        public Object value() {
            return this.value;
        }

        public Object variable(final String name) {
            return this.context.getBindings(ScriptContext.ENGINE_SCOPE).get(name);
        }

        private ScriptContext context() {
            return context;
        }

        private void value(final Object value) {
            this.value = value;
        }
    }
}
