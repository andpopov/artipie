package com.artipie.auth;

import java.util.Collection;
import org.keycloak.TokenVerifier;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;

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

        final Keycloak keycloak = Keycloak.getInstance(
            "http://localhost:8080",
            "artipie_realm",
            "user1",
            "password1",
            "artipie_client",
            null,
            null,
            null,
            false,
            null
        );
        // now you can use the RPT to access protected resources on the resource server


        // introspect the token
        TokenIntrospectionResponse requestingPartyToken = authzClient.protection().introspectRequestingPartyToken(rpt);

        System.out.println("Token status is: " + requestingPartyToken.getActive());
        System.out.println("Permissions granted by the server: ");

        for (Permission granted : requestingPartyToken.getPermissions()) {
            System.out.println(granted);
        }
    }
}
