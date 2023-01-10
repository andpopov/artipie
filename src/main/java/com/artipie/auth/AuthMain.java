package com.artipie.auth;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.representations.AccessTokenResponse;
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
        AuthorizationResponse response = authzClient.authorization("user1", "password1").authorize(request);
        String rpt = response.getToken();

        System.out.println("You got an RPT: " + rpt);

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
