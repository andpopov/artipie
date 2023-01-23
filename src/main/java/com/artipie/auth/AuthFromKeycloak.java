/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

/**
 * Authentication based on keycloak.
 * @since 0.28.0
 */
public final class AuthFromKeycloak implements Authentication {
    private final Configuration configuration;

    /**
     * Ctor.
     */
    public AuthFromKeycloak(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Optional<User> user(final String username, final String password) {
        AuthzClient authzClient = AuthzClient.create(configuration);
        AuthorizationRequest request = new AuthorizationRequest();
        try {
            AuthorizationResponse response = authzClient.authorization(username, password, "openid").authorize(request);
            AccessToken token = TokenVerifier.create(response.getToken(), AccessToken.class).getToken();
            final Set<String> roles = new HashSet<>();
            roles.addAll(realmRoles(token));
            roles.addAll(clientRoles(token));
            return Optional.of(new User(username, roles));
        } catch (VerificationException exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Retrieves realm roles.
     * @param token {@link AccessToken}
     * @return Realm roles.
     */
    private Set<String> realmRoles(final AccessToken token) {
        return token.getRealmAccess().getRoles();
    }

    /**
     * Retrieves client application roles.
     * @param token {@link AccessToken}
     * @return Client application roles.
     */
    private Set<String> clientRoles(final AccessToken token) {
        final Set<String> roles = new HashSet<>();
        token.getResourceAccess().forEach((k, v) -> roles.addAll(v.getRoles()));
        return roles;
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }
}
