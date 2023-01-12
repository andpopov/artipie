/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.Optional;
import java.util.Set;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

/**
 * Authentication based on keycloak.
 * @since 0.28
 */
public final class AuthFromKeycloak implements Authentication {
    /**
     * Ctor.
     */
    public AuthFromKeycloak() {
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Optional<User> user(final String username, final String password) {
        AuthzClient authzClient = AuthzClient.create();
        AuthorizationRequest request = new AuthorizationRequest();
        try {
            AuthorizationResponse response = authzClient.authorization(username, password).authorize(request);
            AccessToken token = TokenVerifier.create(response.getToken(), AccessToken.class).getToken();
            final Set<String> roles = token.getRealmAccess().getRoles();
            return Optional.of(new User(username, roles));
        } catch (RuntimeException e) {
            throw e;
        } catch (VerificationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }
}
