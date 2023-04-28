package com.artipie.scripting;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.Settings;
import java.util.Optional;
import javax.script.ScriptException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ScriptRunner implements Job {
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final Settings settings = (Settings) context.getJobDetail().getJobDataMap().get("settings");
        final Key key = new Key.From(context.getJobDetail().getJobDataMap().getString("key"));
        final BlockingStorage storage = new BlockingStorage(settings.configStorage());
        if (storage.exists(key)) {
            extension(key.toString())
                .flatMap(ext -> script(ext, new String(storage.value(key))))
                .map(script -> {
                    try {
                        return script.call();
                    } catch (ScriptException e) {
                        throw new ArtipieException(e);
                    }
                });
        }
    }

    private Optional<Script> script(final String ext, final String script) {
        final Optional<Script> res;
        switch (ext) {
            case "groovy":
                res = Optional.of(GroovyScript.newScript(script));
                break;
            case "py":
                res = Optional.of(PythonScript.newScript(script));
                break;
            case "ruby":
                res = Optional.of(RubyScript.newScript(script));
                break;
            case "mvel":
                res = Optional.of(MvelScript.newScript(script));
                break;
            default:
                res = Optional.empty();
        }
        return res;
    }

    private Optional<String> extension(final String key) {
        final int pos = key.lastIndexOf('.');
        final Optional<String> res;
        if (pos >= 0) {
            res = Optional.of(key.substring(pos + 1));
        } else {
            res = Optional.empty();
        }
        return res;
    }
}
