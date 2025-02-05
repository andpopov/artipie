/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.ArtipieAuthFactory;
import com.artipie.http.auth.AuthFactory;
import com.artipie.http.auth.Authentication;

/**
 * Factory for auth from environment.
 * @since 0.30
 */
@ArtipieAuthFactory("env")
public final class AuthFromEnvFactory implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping yaml) {
        return new AuthFromEnv();
    }
}
