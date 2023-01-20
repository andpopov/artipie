package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import org.keycloak.authorization.client.Configuration;

import java.util.Map;
import java.util.Optional;

/**
 * docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:latest start-dev
 */
public class AuthFromKeycloakMain {
    public static void main(String[] args) {
        final Optional<Authentication.User> user = new AuthFromKeycloak(
                new Configuration(
                        "http://localhost:8080",
                        "test_realm",
                        "test_client",
                        Map.of("secret", "secret"),
                        null
                )
        ).user("user1", "password");
        user.map(u -> {
            System.out.println(u.name());
            System.out.println(u.groups());
            return 1;
        });
    }
}
