package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.Optional;

public class AuthFromKeycloakMain {
    public static void main(String[] args) {
        final Optional<Authentication.User> user = new AuthFromKeycloak().user("user1", "password1");
        user.map(u -> {
            System.out.println(u.name());
            System.out.println(u.groups());
            return 1;
        });
    }
}
