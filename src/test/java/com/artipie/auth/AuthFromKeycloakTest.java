package com.artipie.auth;

import com.artipie.asto.test.TestResource;
import com.artipie.tools.CompilerTool;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthFromKeycloak}
 */
public class AuthFromKeycloakTest {
    @Test
    void docker() throws IOException {
        final String resources = "auth/keycloak-docker-initializer";
        final Set<Path> jars = paths(
            new TestResource(String.format("%s/lib", resources)).asPath(), ".jar"
        );
        final Set<Path> sources = paths(
            new TestResource(String.format("%s", resources)).asPath(), ".java"
        );
        final CompilerTool compiler = new CompilerTool();
        compiler.addClasspaths(jars.stream().map(Path::toFile).toList());
        compiler.addSources(sources.stream().map(Path::toFile).toList());
        compiler.compile();
        System.out.println(compiler.blobs());
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
