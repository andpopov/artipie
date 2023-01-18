package com.artipie.auth;

import java.util.Collection;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;

public class AuthMain {
    public static void main(String[] args) {
//        AuthzClient authzClient = AuthzClient.create();
//        AuthorizationResource authorizationResource = authzClient.authorization("user1", "password1");
//        System.out.println(authorizationResource);
//
//        final AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken();
//        System.out.println(accessTokenResponse.getIdToken());

        // create a new instance based on the configuration defined in keycloak.json
        AuthzClient authzClient = AuthzClient.create();

        // create an authorization request
        AuthorizationRequest request = new AuthorizationRequest();

        // send the entitlement request to the server in order to
        // obtain an RPT with all permissions granted to the user
        AuthorizationResponse response = authzClient.authorization("user1", "password", "openid").authorize(request);
        String rpt = response.getToken();

        System.out.println("You got an RPT: " + rpt);

        {
            try {
                AccessToken token = TokenVerifier.create(rpt, AccessToken.class).getToken();
                System.out.printf("Realm 'foo' = Roles %s%n", token.getRealmAccess().getRoles());
                token.getResourceAccess().forEach((k, v) -> System.out.printf("Client '%s' = Roles '%s'%n", k, v.getRoles()));

                final Collection<Permission> perms = token.getAuthorization().getPermissions();
                System.out.println(perms);
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        }

//        Keycloak keycloak = Keycloak.getInstance(
//            "http://localhost:8080",
//            "master",
//            "admin",
//            "admin",
//            "admin-cli");
//        RealmRepresentation realm = keycloak.realm("artipie-realm").toRepresentation();

    }
}
