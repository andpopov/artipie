package com.artipie.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;

/**
 * Dynamically compiles java-sources.
 */
public class CompilerTool {
    /**
     * Classpath for compilation.
     */
    private final List<URL> classpath = new ArrayList<>();

    /**
     * Sources for compilation.
     */
    private final List<URL> sources = new ArrayList<>();

    /**
     * Diagnostic listener.
     */
    private DiagnosticListener<JavaFileObject> diagnostic = new DiagnosticCollector<JavaFileObject>();

    /**
     * Code blobs of compiled classes.
     */
    private final List<CodeBlob> blobs = new ArrayList<>();

    /**
     * Ctor.
     */
    public CompilerTool() {
    }

    /**
     * Add list of URLs to compilation classpath.
     * @param urls List of URLs.
     */
    public void addClasspaths(final List<URL> urls) {
        this.classpath.addAll(urls);
    }

    /**
     * Add list of java source URLs for compilation.
     * @param urls List of URLs.
     */
    public void addSources(final List<URL> urls) {
        this.sources.addAll(urls);
    }

    /**
     * Diagnostic listener.
     * @return Diagnostic listener.
     */
    public DiagnosticListener<JavaFileObject> diagnostic() {
        return this.diagnostic;
    }

    /**
     * Compiled code blobs.
     * @return Code blobs.
     */
    public List<CodeBlob> blobs() {
        return this.blobs;
    }

    /**
     * Compiles java sources and stores compiled classes to list of code blobs.
     * @throws IOException IOException
     */
    public void compile() throws IOException {
        final Path output = Files.createTempDirectory("compiled");
        Iterable<String> options = Arrays.asList("-d", output.toString());
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fm = compiler.getStandardFileManager(
                this.diagnostic, Locale.ENGLISH, Charset.defaultCharset()
            );
            fm.setLocation(StandardLocation.CLASS_PATH, urlsToFiles(this.classpath));
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(urlsToFiles(this.sources));
            if (!compiler.getTask(null, fm, this.diagnostic, options, null, units).call()) {
                fm.close();
                throw new AssertionError("compilation failed");
            }
            fm.close();
            this.blobs.addAll(blobs(output));
        } finally {
            FileUtils.deleteDirectory(output.toFile());
        }
    }

    /**
     * Loads result of compilation from directory to set of code blobs.
     * @param dir Directory stores result of compilation.
     * @return Set of code blobs.
     * @throws IOException
     */
    private static Set<CodeBlob> blobs(final Path dir) throws IOException {
        Set<CodeBlob> blobs = new HashSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(path)) {
                    if (path.toString().endsWith(".class")) {
                        final String classname = dir.relativize(path).toString()
                            .replace(File.separatorChar, '.').replaceAll("\\.class$", "");
                        final byte[] bytes = Files.readAllBytes(path);
                        blobs.add(new CodeBlob(classname, bytes));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return blobs;
    }

    /**
     * Converts list of URL to List of File.
     * @param urls List of URL.
     * @return List of File.
     */
    private static List<File> urlsToFiles(final List<URL> urls) {
        return urls.stream().map(FileUtils::toFile).toList();
    }
}
