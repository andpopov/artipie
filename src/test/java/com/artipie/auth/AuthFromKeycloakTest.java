/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.YamlSettings;
import com.artipie.settings.users.Users;
import com.artipie.tools.CodeBlob;
import com.artipie.tools.CodeClassLoader;
import com.artipie.tools.CompilerTool;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Test for {@link AuthFromKeycloak}
 */
@Testcontainers
public class AuthFromKeycloakTest {
    /**
     * Keycloak port.
     */
    private final static int KEYCLOAK_PORT = 8080;

    /**
     * Keycloak admin login.
     */
    private final static String KEYCLOAK_ADMIN_LOGIN = "admin";

    /**
     * Keycloak admin password.
     */
    private final static String KEYCLOAK_ADMIN_PASSWORD = KEYCLOAK_ADMIN_LOGIN;

    /**
     * Keycloak docker container.
     */
    @Container
    private static GenericContainer<?> keycloak = new GenericContainer<>(
        DockerImageName.parse("quay.io/keycloak/keycloak:20.0.1")
    )
        .withEnv("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN_LOGIN)
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PASSWORD)
        .withExposedPorts(KEYCLOAK_PORT)
        .withCommand("start-dev");

    /**
     * Jars of classpath used for compilation java sources and loading of compiled classes.
     */
    private static Set<URL> jars;

    /**
     * Sources of java-code for compilation.
     */
    private static Set<URL> sources;

    /**
     * Compiles, loads 'keycloak.KeycloakDockerInitializer' class and start 'main'-method.
     * Runtime compilation is required because 'keycloak.KeycloakDockerInitializer' class
     * has a clash of dependencies with artipie's dependency 'com.jcabi:jcabi-github:1.3.2'.
     *
     */
    @BeforeAll
    static void init() throws Throwable {
        AuthFromKeycloakTest.prepareJarsAndSources();
        final List<CodeBlob> blobs = AuthFromKeycloakTest.compileKeycloakInitializer();
        final CodeClassLoader codeClassloader = AuthFromKeycloakTest.initCodeClassloader(blobs);
        final MethodHandle main = AuthFromKeycloakTest.mainMethod(codeClassloader);
        AuthFromKeycloakTest.initializeKeycloakInstance(codeClassloader, main);
    }

    @Test
    /**
     * Authenticates user by using keycloak authentication.
     */
    void authenticateUserByKeycloakReturningExpectedUserWithRealmAndClientRoles() {
        final String login = "user1";
        final String password = "password";
        final YamlSettings settings = AuthFromKeycloakTest.settings(
                AuthFromKeycloakTest.keycloakUrl(),
                "test_realm",
                "test_client",
                "secret"
        );
        final AtomicReference<Authentication.User> ref = new AtomicReference<>();
        settings
            .credentials()
            .thenCompose(Users::auth)
            .thenAccept(auth -> ref.set(auth.user(login, password).get()));
        Awaitility.waitAtMost(3_000, TimeUnit.MILLISECONDS)
            .until(() -> ref.get() != null);
        MatcherAssert.assertThat(
            ref.get(),
            Is.is(IsNull.notNullValue())
        );
        final Authentication.User user = ref.get();
        MatcherAssert.assertThat(
            user.name(),
            Is.is(login)
        );
        MatcherAssert.assertThat(user.groups().contains("role_realm"), new IsEqual<>(true));
        MatcherAssert.assertThat(user.groups().contains("client_role"), new IsEqual<>(true));
    }

    /**
     * Composes yaml settings.
     * @param url Keycloak server url
     * @param realm Keycloak realm
     * @param clientId Keycloak client application ID
     * @param clientPassword Keycloak client application password
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    private static YamlSettings settings(final String url, final String realm, final String clientId, final String clientPassword) {
        return new YamlSettings(
                Yaml.createYamlMappingBuilder().add(
                        "meta",
                        Yaml.createYamlMappingBuilder().add(
                                "credentials",
                                Yaml.createYamlSequenceBuilder()
                                        .add(
                                                Yaml.createYamlMappingBuilder()
                                                        .add("type", "keycloak")
                                                        .add("url", url)
                                                        .add("realm", realm)
                                                        .add("client-id", clientId)
                                                        .add("client-password", clientPassword)
                                                        .build()
                                        ).build()
                        ).build()
                ).build(),
                null
        );
    }

    /**
     * Loads dependencies from jar-files and java-sources for compilation.
     * @throws Throwable Exception.
     */
    private static void prepareJarsAndSources() throws Throwable {
        final String resources = "auth/keycloak-docker-initializer";
        jars = files(
            new TestResource(String.format("%s/lib", resources)).asPath(), ".jar"
        );
        sources = files(
            new TestResource(String.format("%s/src", resources)).asPath(), ".java"
        );
    }

    /**
     * Compiles 'keycloak.KeycloakDockerInitializer' class from sources.
     * @return List of compiled classes as CodeBlobs.
     * @throws Throwable Exception.
     */
    private static List<CodeBlob> compileKeycloakInitializer() throws Throwable {
        final CompilerTool compiler = new CompilerTool();
        compiler.addClasspaths(jars.stream().toList());
        compiler.addSources(sources.stream().toList());
        compiler.compile();
        return compiler.classesToCodeBlobs();
    }

    /**
     * Create instance of CodeClassLoader.
     * @param blobs Code blobs.
     * @return CodeClassLoader
     */
    private static CodeClassLoader initCodeClassloader(final List<CodeBlob> blobs) {
        final URLClassLoader urlclassloader = new URLClassLoader(jars.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException | URISyntaxException exc) {
                throw new RuntimeException(exc);
            }
        }).toList().toArray(new URL[0]), null);
        CodeClassLoader codeClassloader = new CodeClassLoader(urlclassloader);
        codeClassloader.addBlobs(blobs);
        return codeClassloader;
    }

    /**
     * Lookups 'public static void main(String[] args)' method
     * of 'keycloak.KeycloakDockerInitializer' class.
     * @param codeClassloader CodeClassLoader
     * @return 'public static void main(String[] args)' method
     * of 'keycloak.KeycloakDockerInitializer' class
     * @throws ClassNotFoundException Exception.
     * @throws NoSuchMethodException Exception.
     * @throws IllegalAccessException Exception.
     */
    private static MethodHandle mainMethod(final CodeClassLoader codeClassloader)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        Class<?> cls = Class.forName("keycloak.KeycloakDockerInitializer", true, codeClassloader);
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType mt = MethodType.methodType(void.class, String[].class);
        return publicLookup.findStatic(cls, "main", mt);
    }

    /**
     * Starts 'keycloak.KeycloakDockerInitializer' class by passing url of keycloak server
     * in first argument of 'main'-method.
     * CodeClassLoader is used as context class loader.
     * @param codeClassloader CodeClassLoader.
     * @param main Main-method.
     * @throws Throwable Exception.
     */
    private static void initializeKeycloakInstance(CodeClassLoader codeClassloader,
        final MethodHandle main) throws Throwable {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(codeClassloader);
            main.invoke(
                new String[]{keycloakUrl()}
            );
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Lookup files in directory by specified extension.
     * @param dir Directory for listing.
     * @param ext Extension of files, example '.jar'
     * @return URLs of files.
     * @throws IOException
     */
    private static Set<URL> files(final Path dir, final String ext) throws IOException {
        Set<URL> files = new HashSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws MalformedURLException {
                if (!Files.isDirectory(file)) {
                    if (ext == null || file.toString().endsWith(ext)) {
                        files.add(file.toFile().toURI().toURL());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    /**
     * Keycloak server url loaded by docker container.
     * @return Keycloak server url.
     */
    private static String keycloakUrl() {
        return String.format("http://localhost:%s", keycloak.getMappedPort(KEYCLOAK_PORT));
    }
}
