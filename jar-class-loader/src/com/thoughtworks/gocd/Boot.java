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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
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
        new Boot(args).run();
    }

    private void run() {
        Handler.init();
        try (JarFile jarFile = new JarFile(currentJarFile())) {
            String mainClassName = mainClassName(jarFile);

            List<URL> urls = jarFile.stream()
                    .filter(FIND_JAR_FILES_UNDER_LIB_FOLDER)
                    .map(jarEntry -> Handler.toOneJarUrl(jarEntry.getName()))
                    .collect(Collectors.toList());

            ClassLoader jcl = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(jcl);
            Class<?> mainClass = jcl.loadClass(mainClassName);
            mainClass.getMethod("main", String[].class).invoke(null, new Object[]{args});
        } catch (IOException | ReflectiveOperationException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static File currentJarFile() {
        return new File(Boot.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath());
    }

    static String mainClassName(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue("GoCD-Main-Class");
    }
}
