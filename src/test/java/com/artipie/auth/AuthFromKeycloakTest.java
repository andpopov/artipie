package com.artipie.auth;

import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.tools.Blob;
import com.artipie.tools.BlobClassLoader;
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
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthFromKeycloak}
 */
public class AuthFromKeycloakTest {
    private static Set<Path> jars;
    private static Set<Path> sources;
    private static BlobClassLoader blobClassloader;
    private static List<Blob> blobs;
    private static MethodHandle main;

    @BeforeAll
    static void init() throws Throwable {
        prepareJarsAndSources();
        compileKeycloakInitializer();
        initBlobClassloader();
        inializeKeycloak();
    }

    @Test
    void docker() {
        final Optional<Authentication.User> user = new AuthFromKeycloak().user("user1", "password");
        user.map(u -> {
            System.out.println(u.name());
            System.out.println(u.groups());
            return 1;
        });
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
        blobClassloader = new BlobClassLoader(urlclassloader);
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
            main.invoke(new String[]{"http://localhost:8080"});
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
