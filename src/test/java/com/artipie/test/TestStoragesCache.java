/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import com.artipie.asto.factory.StoragesLoader;
import com.artipie.settings.cache.CachedStorages;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test storages caches.
 * @since 0.28
 */
public final class TestStoragesCache extends CachedStorages {

    /**
     * Counter for `invalidateAll()` method calls.
     */
    private final AtomicInteger cnt;

    /**
     * Ctor.
     * Here an instance of cache is created. It is important that cache
     * is a local variable.
     */
    public TestStoragesCache() {
        super(new StoragesLoader());
        this.cnt = new AtomicInteger(0);
    }

    @Override
    public void invalidateAll() {
        this.cnt.incrementAndGet();
        super.invalidateAll();
    }

    /**
     * Was this case invalidated?
     *
     * @return True, if it was invalidated once
     */
    public boolean wasInvalidated() {
        return this.cnt.get() == 1;
    }
}
