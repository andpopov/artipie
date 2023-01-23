package com.artipie.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;

public class CompilerTool {
    private final List<File> classpath = new ArrayList<>();

    private final List<File> sources = new ArrayList<>();

    private DiagnosticListener<JavaFileObject> diagnostic = new DiagnosticCollector<JavaFileObject>();

    private final List<Blob> blobs = new ArrayList<>();

    public CompilerTool() {
    }

    public void addClasspaths(final Path... paths) {
        addClasspaths(Arrays.stream(paths).map(Path::toFile).toList());
    }

    public void addClasspaths(final File... files) {
        addClasspaths(List.of(files));
    }

    public void addClasspaths(final List<File> files) {
        classpath.addAll(files);
    }

    public void addSources(final Path... paths) {
        addSources(Arrays.stream(paths).map(Path::toFile).toList());
    }

    public void addSources(final File... files) {
        addSources(List.of(files));
    }

    public void addSources(final List<File> files) {
        sources.addAll(files);
    }

    public DiagnosticListener<JavaFileObject> diagnostic() {
        return diagnostic;
    }

    public List<Blob> blobs() {
        return blobs;
    }

    public void compile() throws IOException {
        final Path output = Files.createTempDirectory("compiled");
        Iterable<String> options = Arrays.asList("-d", output.toString());
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fm = compiler.getStandardFileManager(
                diagnostic, Locale.ENGLISH, Charset.defaultCharset()
            );
            fm.setLocation(StandardLocation.CLASS_PATH, classpath);
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(sources);
            if (!compiler.getTask(null, fm, diagnostic, options, null, units).call()) {
                fm.close();
                throw new AssertionError("compilation failed");
            }
            fm.close();
            blobs.addAll(blobs(output));
        } finally {
            FileUtils.deleteDirectory(output.toFile());
        }
    }

    private static Set<Blob> blobs(final Path dir) throws IOException {
        Set<Blob> blobs = new HashSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(path)) {
                    if (path.toString().endsWith(".class")) {
                        final String classname = dir.relativize(path).toString().replace(File.separatorChar, '.').replaceAll("\\.class$", "");
                        final byte[] bytes = Files.readAllBytes(path);
                        blobs.add(new Blob(classname, bytes));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return blobs;
    }


    final static String TMP = "C:\\compiled";

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Iterable<String> options = Arrays.asList(
            "-d", TMP,
            "-classpath",
            "C:\\Users\\aWX1192382\\.m2\\repository\\org\\jboss\\spec\\javax\\ws\\rs\\jboss-jaxrs-api_2.1_spec\\2.0.1.Final\\jboss-jaxrs-api_2.1_spec-2.0.1.Final.jar"
                + System.getProperty("path.separator") +
                "C:\\Users\\aWX1192382\\.m2\\repository\\org\\keycloak\\keycloak-admin-client\\20.0.1\\keycloak-admin-client-20.0.1.jar"
                + System.getProperty("path.separator") +
                "C:\\Users\\aWX1192382\\.m2\\repository\\org\\keycloak\\keycloak-core\\20.0.1\\keycloak-core-20.0.1.jar"

        );

        DiagnosticListener<? super JavaFileObject> diagnosticListener = new DiagnosticListener<JavaFileObject>() {
            @Override
            public void report(final Diagnostic<? extends JavaFileObject> diagnostic) {
                System.out.println(diagnostic.getKind() + " " + diagnostic);
            }
        };
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticListener, Locale.ENGLISH, Charset.defaultCharset());

        Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles(
            List.of(
                Paths.get(URI.create("file:///projects/artipie/keycloak-docker-initializer/src/main/java/keycloak/KeycloakDockerInitializer.java")).toFile()
            )
        );
        compiler.getTask(null, fileManager, diagnosticListener, options, null, compilationUnits1).call();

        fileManager.close();

        // Load and instantiate compiled class.
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(TMP).toURI().toURL()});
        Class<?> cls;
        cls = Class.forName("keycloak.KeycloakDockerInitializer", true, classLoader);
        cls.getMethod("main2").invoke(null);
    }
}
