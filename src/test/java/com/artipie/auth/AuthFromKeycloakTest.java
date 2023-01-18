package com.artipie.auth;

import com.artipie.asto.test.TestResource;
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
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthFromKeycloak}
 */
public class AuthFromKeycloakTest {
    private static BlobClassLoader loader;
    private static MethodHandle main;

    @BeforeAll
    static void init() throws Throwable {
        loadClass();
    }

    private static void loadClass() throws Throwable {
        final String resources = "auth/keycloak-docker-initializer";
        final Set<Path> jars = paths(
            new TestResource(String.format("%s/lib", resources)).asPath(), ".jar"
        );
        final Set<Path> sources = paths(
            new TestResource(String.format("%s/src", resources)).asPath(), ".java"
        );
        final CompilerTool compiler = new CompilerTool();
        compiler.addClasspaths(jars.stream().map(Path::toFile).toList());
        compiler.addSources(sources.stream().map(Path::toFile).toList());
        compiler.compile();
        final URLClassLoader urlclassloader = new URLClassLoader(jars.stream().map(file -> {
            try {
                return file.toFile().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList().toArray(new URL[0]), null);
        loader = new BlobClassLoader(urlclassloader);
        loader.addBlobs(compiler.blobs());
        Class<?> cls = Class.forName("keycloak.KeycloakDockerInitializer", true, loader);
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType mt = MethodType.methodType(void.class, String[].class);
        main = publicLookup.findStatic(cls, "main", mt);
    }

    void initializeKeycloak() throws Throwable {
        final Thread thread = new Thread(() -> {
            try {
                main.invoke(new String[]{"http://localhost:8080"});
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        thread.setContextClassLoader(loader);
        thread.start();
        thread.join();
    }

    @Test
    void docker() throws Throwable {
        initializeKeycloak();
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
