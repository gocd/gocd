/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.gocd;

import com.thoughtworks.gocd.onejar.Handler;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Boot {
    private static final Predicate<JarEntry> FIND_JAR_FILES_UNDER_LIB_FOLDER = jarEntry -> jarEntry.getName().startsWith("lib/") && jarEntry.getName().endsWith(".jar");

    private final String[] args;

    private Boot(String... args) {
        this.args = args;
    }

    public static void main(String... args) {
        if (shouldRedirectStdOutAndErr()) {
            redirectStdOutAndErr();
        }

        log("Starting process: ");
        log("  Working directory    : " + System.getProperty("user.dir"));
        log("  JVM arguments        : " + jvmArgs());
        log("  Application arguments: " + Arrays.asList(args));
        log("           GoCD Version: " + Boot.class.getPackage().getImplementationVersion());
        log("         JVM properties: " + System.getProperties());
        log("  Environment Variables: " + System.getenv());
        new Boot(args).run();
    }

    private static void redirectStdOutAndErr() {
        try {
            PrintStream out = new PrintStream(new FileOutputStream(getOutFile(), true), true);
            System.setErr(out);
            System.setOut(out);
        } catch (FileNotFoundException ignore) {
            // cannot redirect out and err to file, so we don't
            log("Unable to redirect stdout/stderr to file " + getOutFile() + ". Will continue without redirecting stdout/stderr.");
            ignore.printStackTrace();
        }
    }

    private static List<String> jvmArgs() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        return bean.getInputArguments();
    }

    private static void log(String message) {
        System.err.println("[" + new Date() + "] " + message);
    }

    private static String getOutFile() {
        return System.getProperty("gocd.redirect.stdout.to.file");
    }

    private static boolean shouldRedirectStdOutAndErr() {
        return getOutFile() != null;
    }

    private void run() {
        Handler.init();
        try (JarFile jarFile = new JarFile(currentJarFile())) {
            String mainClassName = mainClassName(jarFile);

            List<URL> jarsInJar = jarFile.stream()
                    .filter(FIND_JAR_FILES_UNDER_LIB_FOLDER)
                    .map(jarEntry -> Handler.toOneJarUrl(jarEntry.getName()))
                    .collect(Collectors.toList());

            List<URL> urls = new ArrayList<>();
            urls.addAll(additionalJarsInLibsDirectoryInCWD());
            urls.addAll(jarsInJar);

            ClassLoader jcl = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(jcl);
            Class<?> mainClass = jcl.loadClass(mainClassName);
            mainClass.getMethod("main", String[].class).invoke(null, new Object[]{args});
        } catch (IOException | ReflectiveOperationException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private List<URL> additionalJarsInLibsDirectoryInCWD() {
        File[] libs = new File("libs").listFiles(pathname -> pathname.getName().endsWith(".jar"));

        if (libs != null && libs.length > 0) {
            return Arrays.stream(libs).map(file -> {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    static File currentJarFile() {
        try {
            return new File(Boot.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static String mainClassName(JarFile jarFile) throws IOException {
        // allow overriding the main class name, used by OSX installers
        if (System.getProperty("jar-class-loader.main.class") == null) {
            return defaultMainClassName(jarFile);
        } else {
            return System.getProperty("jar-class-loader.main.class");
        }
    }

    private static String defaultMainClassName(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue("GoCD-Main-Class");
    }
}
