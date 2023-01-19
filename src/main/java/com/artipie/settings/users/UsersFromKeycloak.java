/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.users;

import com.artipie.auth.AuthFromKeycloak;
import com.artipie.http.auth.Authentication;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Credentials from keycloak.
 * @since 0.28.0
 */
public final class UsersFromKeycloak implements Users {
    @Override
    public CompletionStage<List<User>> list() {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Listing users is not supported")
        );
    }

    @Override
    public CompletionStage<Void> add(final User user, final String pswd,
        final PasswordFormat format) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Adding users is not supported")
        );
    }

    @Override
    public CompletionStage<Void> remove(final String username) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException("Removing users is not supported")
        );
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return CompletableFuture.completedFuture(new AuthFromKeycloak());
    }
}
