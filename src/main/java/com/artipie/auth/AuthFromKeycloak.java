/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.Objects;
import java.util.Optional;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

/**
 * Authentication based on keycloak.
 * @since 0.3
 */
public final class AuthFromKeycloak implements Authentication {
    /**
     * Default ctor with system environment.
     */
    public AuthFromKeycloak() {
    }


    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Optional<User> user(final String username, final String password) {
        final Optional<User> result;

        AuthzClient authzClient = AuthzClient.create();

        // create an authorization request
        AuthorizationRequest request = new AuthorizationRequest();

        // send the entitlement request to the server in order to
        // obtain an RPT with all permissions granted to the user
        AuthorizationResponse response = authzClient.authorization(username, password).authorize(request);
        String rpt = response.getToken();

        if (Objects.equals(Objects.requireNonNull(username), username)
            && Objects.equals(Objects.requireNonNull(password), password)) {
            result = Optional.of(new User(username));
        } else {
            result = Optional.empty();
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }
}
