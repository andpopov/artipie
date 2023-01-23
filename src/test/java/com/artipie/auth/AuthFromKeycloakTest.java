package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.test.TestResource;
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
     * Keycloak container.
     */
    @Container
    private static GenericContainer<?> keycloak = new GenericContainer<>(
        DockerImageName.parse("quay.io/keycloak/keycloak:latest")
    )
        .withEnv("KEYCLOAK_ADMIN", "admin")
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
        .withExposedPorts(8080)
        .withCommand("start-dev");

    private static Set<Path> jars;

    private static Set<Path> sources;

    private static CodeClassLoader blobClassloader;

    private static List<CodeBlob> blobs;

    private static MethodHandle main;

    @BeforeAll
    static void init() throws Throwable {
        prepareJarsAndSources();
        compileKeycloakInitializer();
        initBlobClassloader();
        inializeKeycloak();
    }

    @Test
    void docker() throws IOException {
        final String user = "user1";
        final String pass = "password";
        final YamlSettings settings = AuthFromKeycloakTest.settings(
                String.format("http://localhost:%s", keycloak.getMappedPort(8080)),
                "test_realm",
                "test_client",
                "secret"
        );
        settings
                .credentials()
                .thenCompose(Users::auth)
                .thenApply(auth -> auth.user(user, pass));
    }

    /**
     * Composes yaml settings.
     * @param url Keycloak server url
     * @param realm Keycloak realm
     * @param clientId Keycloak client application ID
     * @param clientPassword Keycloak client application password
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    private static YamlSettings settings(final String url, final String realm, final String clientId, final String clientPassword) throws IOException {
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

    private static void prepareJarsAndSources() throws Throwable {
        final String resources = "auth/keycloak-docker-initializer";
        jars = paths(
            new TestResource(String.format("%s/lib", resources)).asPath(), ".jar"
        );
        sources = paths(
            new TestResource(String.format("%s/src", resources)).asPath(), ".java"
        );
    }

    private static void compileKeycloakInitializer() throws Throwable {
        final CompilerTool compiler = new CompilerTool();
        compiler.addClasspaths(jars.stream().map(Path::toFile).toList());
        compiler.addSources(sources.stream().map(Path::toFile).toList());
        compiler.compile();
        blobs = compiler.blobs();
    }

    private static void initBlobClassloader() throws Throwable {
        final URLClassLoader urlclassloader = new URLClassLoader(jars.stream().map(file -> {
            try {
                return file.toFile().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList().toArray(new URL[0]), null);
        blobClassloader = new CodeClassLoader(urlclassloader);
        blobClassloader.addBlobs(blobs);
        Class<?> cls = Class.forName("keycloak.KeycloakDockerInitializer", true, blobClassloader);
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType mt = MethodType.methodType(void.class, String[].class);
        main = publicLookup.findStatic(cls, "main", mt);
    }

    private static void inializeKeycloak() throws Throwable {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(blobClassloader);
            main.invoke(new String[]{String.format("http://localhost:%s", keycloak.getMappedPort(8080))});
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private static Set<Path> paths(final Path dir, final String ext) throws IOException {
        Set<Path> files = new HashSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!Files.isDirectory(file)) {
                    if (ext == null || file.toString().endsWith(ext)) {
                        files.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}
