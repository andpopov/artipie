/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.file.Path;
import javax.script.ScriptException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link GroovyScript}.
 *
 * @since 0.30
 * @checkstyle MagicNumberCheck (500 lines)
 */
public class ScriptRunnerTest {
    /**
     * Temp dir.
     * @checkstyle VisibilityModifierCheck (500 lines)
     */
    @TempDir
    Path temp;

    /**
     * Test blocking storage.
     */
    private BlockingStorage bstorage;

    /**
     * Test settings storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.bstorage = new BlockingStorage(this.storage);
    }

    @Test
    public void standardScript() throws ScriptException {
        final Key key = new Key.From("sample.groovy");
        this.bstorage.save(key, "println(1)".getBytes());
        final ScriptRunner runner = new ScriptRunner();
    }
}
