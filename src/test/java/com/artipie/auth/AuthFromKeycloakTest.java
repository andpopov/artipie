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
     * Keycloak container.
     */
    @Container
    private static GenericContainer<?> keycloak = new GenericContainer<>(
        DockerImageName.parse("quay.io/keycloak/keycloak:latest")
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

    @BeforeAll
    static void init() throws Throwable {
        prepareJarsAndSources();
        final List<CodeBlob> blobs = compileKeycloakInitializer();
        final CodeClassLoader codeClassloader = initCodeClassloader(blobs);
        final MethodHandle main = mainMethod(codeClassloader);
        inializeKeycloak(codeClassloader, main);
    }

    @Test
    void docker() {
        final String user = "user1";
        final String pass = "password";
        final YamlSettings settings = AuthFromKeycloakTest.settings(
                keycloakUrl(),
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

    private static void prepareJarsAndSources() throws Throwable {
        final String resources = "auth/keycloak-docker-initializer";
        jars = paths(
            new TestResource(String.format("%s/lib", resources)).asPath(), ".jar"
        );
        sources = paths(
            new TestResource(String.format("%s/src", resources)).asPath(), ".java"
        );
    }

    private static List<CodeBlob> compileKeycloakInitializer() throws Throwable {
        final CompilerTool compiler = new CompilerTool();
        compiler.addClasspaths(jars.stream().toList());
        compiler.addSources(sources.stream().toList());
        compiler.compile();
        return compiler.blobs();
    }

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

    private static MethodHandle mainMethod(final CodeClassLoader codeClassloader) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        Class<?> cls = Class.forName("keycloak.KeycloakDockerInitializer", true, codeClassloader);
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType mt = MethodType.methodType(void.class, String[].class);
        return publicLookup.findStatic(cls, "main", mt);
    }

    private static void inializeKeycloak(CodeClassLoader codeClassloader, final MethodHandle main) throws Throwable {
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

    private static Set<URL> paths(final Path dir, final String ext) throws IOException {
        Set<URL> files = new HashSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws MalformedURLException {
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

    private static String keycloakUrl() {
        return String.format("http://localhost:%s", keycloak.getMappedPort(KEYCLOAK_PORT));
    }
}
