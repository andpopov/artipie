package keycloak;

import java.util.Collections;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class KeycloakDockerInitializer {
    private final static String KEYCLOAK_HOST = "http://localhost:8080";
    private final static String REALM = "test_realm";
    private final static String REALM_ROLE = "role_realm";
    private final static String CLIENT_ROLE = "client_role";
    private final static String CLIENT_ID = "test_client";
    private final static String CLIENT_SECRET = "secret";
    private final static String USER_ID = "user1";
    private final static String USER_PASSWORD = "password";

    public static void main(String[] args) {
        final String host;
        if (!Objects.isNull(args) && args.length > 0) {
            host = args[0];
        } else {
            host = KEYCLOAK_HOST;
        }
        new KeycloakDockerInitializer(host).init();
    }

    private final String host;

    public KeycloakDockerInitializer(final String host) {
        this.host = host;
    }

    public void init() {
        System.out.println(host);
//        Keycloak keycloak = Keycloak.getInstance(
//            host,
//            "master",
//            "admin",
//            "admin",
//            "admin-cli");
//        createRealm(keycloak);
//        createRealmRole(keycloak);
//        createClient(keycloak);
//        createClientRole(keycloak);
//        createUserNew(keycloak);
    }

    private void createRealm(final Keycloak keycloak) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realm.setEnabled(true);
        keycloak.realms().create(realm);
    }

    private void createRealmRole(final Keycloak keycloak) {
        keycloak.realm(REALM).roles().create(new RoleRepresentation(REALM_ROLE, null, false));
    }

    private void createClient(final Keycloak keycloak) {
        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setClientId(CLIENT_ID);
        client.setProtocol("openid-connect");
        client.setSecret(CLIENT_SECRET);
        client.setAuthorizationServicesEnabled(true);
        client.setServiceAccountsEnabled(true);
        keycloak.realm(REALM).clients().create(client);
    }

    private void createClientRole(final Keycloak keycloak) {
        RoleRepresentation clientRoleRepresentation = new RoleRepresentation();
        clientRoleRepresentation.setName(CLIENT_ROLE);
        clientRoleRepresentation.setClientRole(true);
        keycloak.realm(REALM)
            .clients()
            .findByClientId(CLIENT_ID)
            .forEach(clientRepresentation ->
                keycloak.realm(REALM)
                    .clients()
                    .get(clientRepresentation.getId())
                    .roles()
                    .create(clientRoleRepresentation)
            );
    }

    private void createUserNew(final Keycloak keycloak) {
        // Define user
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(USER_ID);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail(USER_ID + "@localhost");

        // Get realm
        RealmResource realmResource = keycloak.realm(REALM);
        UsersResource usersRessource = realmResource.users();

        // Create user (requires manage-users role)
        Response response = usersRessource.create(user);
        String userId = response.getLocation().getPath().substring(response.getLocation().getPath().lastIndexOf('/') + 1);

        // Define password credential
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(USER_PASSWORD);

        UserResource userResource = usersRessource.get(userId);

        // Set password credential
        userResource.resetPassword(passwordCred);

        // Get realm role "tester" (requires view-realm role)
        RoleRepresentation testerRealmRole = realmResource
            .roles()
            .get(REALM_ROLE)
            .toRepresentation();

        // Assign realm role tester to user
        userResource.roles().realmLevel().add(Collections.singletonList(testerRealmRole));

        // Get client
        ClientRepresentation appClient = realmResource
            .clients()
            .findByClientId(CLIENT_ID)
            .get(0);

        // Get client level role (requires view-clients role)
        RoleRepresentation userClientRole = realmResource
            .clients()
            .get(appClient.getId())
            .roles()
            .get(CLIENT_ROLE)
            .toRepresentation();

        // Assign client level role to user
        userResource
            .roles()
            .clientLevel(appClient.getId())
            .add(Collections.singletonList(userClientRole));
    }
}
