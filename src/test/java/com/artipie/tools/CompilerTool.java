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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class CompilerTool {
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
