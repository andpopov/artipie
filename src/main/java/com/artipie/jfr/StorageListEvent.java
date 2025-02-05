/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Storage event for the {@code list} operation.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 * @checkstyle MemberNameCheck (500 lines)
 */
@Name("artipie.StorageList")
@Label("Storage List")
@Category({"Artipie", "Storage"})
@Description("Get the list of keys that start with this prefix")
public final class StorageListEvent extends AbstractStorageEvent {

    @Label("Keys Count")
    public volatile int keysCount;

}
