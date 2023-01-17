package com.artipie.auth;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;

public class KeycloakAdminMain {
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";
    public static final String REALM = "master";
    public static final String ADMIN_LOGIN = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    public static void main(String[] args) {
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder();
        resteasyClientBuilder.connectionPoolSize(10);
        ClientHttpEngine clientHttpEngine = new ClientHttpEngineBuilder43().resteasyClientBuilder(resteasyClientBuilder).build();
        resteasyClientBuilder.httpEngine(clientHttpEngine);
        ResteasyClient resteasyClient = resteasyClientBuilder.build();

        String clientId = ADMIN_CLI_CLIENT_ID;
        String realm = REALM;
        String username = ADMIN_LOGIN;
        String userpassword = ADMIN_PASSWORD;
        Keycloak client = createAdminClient(realm, clientId, username, userpassword, resteasyClient);
        client.tokenManager().getAccessToken();

        Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:8080/auth")
            .grantType(OAuth2Constants.PASSWORD)
            .realm("master")
            .clientId("keycloak-admin")
            .username("username")
            .password("password")
            .resteasyClient(resteasyClient)
            .build();

        keycloak.tokenManager().getAccessToken();
        RealmResource realmResource = keycloak.realm("realm-name");
    }

    static Keycloak createAdminClient(String realm, String clientId, String username, String password, ResteasyClient resteasyClient) {
        return KeycloakBuilder.builder()
            .serverUrl("http://localhost:8080" + "/auth")
            .realm(realm)
            .username(username)
            .password(password)
            .clientId(clientId)
            .resteasyClient(resteasyClient)
            .build();
    }
}
