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

package com.thoughtworks.go.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Loads the classes from the given jars.
 */
public class NestedJarClassLoader extends ClassLoader {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NestedJarClassLoader.class);
    private final ClassLoader jarClassLoader;
    private final ClassLoader parentClassLoader;
    private final String[] excludes;
    private final File jarDir;
    private static final File TEMP_DIR = new File("data/njcl");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(TEMP_DIR);
            }
        });
    }

    public NestedJarClassLoader(URL jarURL, String... excludes) {
        this(jarURL, NestedJarClassLoader.class.getClassLoader(), excludes);
    }

    NestedJarClassLoader(URL jarURL, ClassLoader parentClassLoader, String... excludes) {
        super(null);
        this.jarDir = new File(TEMP_DIR, UUID.randomUUID().toString());
        this.parentClassLoader = parentClassLoader;
        this.jarClassLoader = createLoaderForJar(jarURL);
        this.excludes = excludes;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(jarDir);
            }
        });
    }

    private ClassLoader createLoaderForJar(URL jarURL) {
        LOGGER.debug("Creating Loader For jar: {}", jarURL);
        ClassLoader jarLoader = new URLClassLoader(enumerateJar(jarURL), this);
        if (jarLoader == null) {
            LOGGER.warn("No jar found with url: {}", jarURL);
        }
        return jarLoader;
    }

    private URL[] enumerateJar(URL urlOfJar) {
        LOGGER.debug("Enumerating jar: {}", urlOfJar);
        List<URL> urls = new ArrayList<>();
        urls.add(urlOfJar);
        try {
            JarInputStream jarStream = new JarInputStream(urlOfJar.openStream());
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                    urls.add(expandJarAndReturnURL(jarStream, entry));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to enumerate jar {}", urlOfJar, e);
        }
        return urls.toArray(new URL[0]);
    }

    private URL expandJarAndReturnURL(JarInputStream jarStream, JarEntry entry) throws IOException {
        File nestedJarFile = new File(jarDir, entry.getName());
        nestedJarFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(nestedJarFile)) {
            IOUtils.copy(jarStream, out);
        }
        LOGGER.info("Exploded Entry {} from to {}", entry.getName(), nestedJarFile);
        return nestedJarFile.toURI().toURL();
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            return jarClassLoader.loadClass(name);
        }
        return parentClassLoader.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            throw new ClassNotFoundException(name);
        }
        return invokeParentClassloader(name, resolve);
    }

    private Class<?> invokeParentClassloader(String name, boolean resolve) throws ClassNotFoundException {
        LOGGER.debug("Invoking parent classloader for {} with resolve {}", name, resolve);
        try {
            Method loadClass = findNonPublicMethod("loadClass", parentClassLoader.getClass(), String.class, boolean.class);
            return (Class<?>) loadClass.invoke(parentClassLoader, name, resolve);
        } catch (InvocationTargetException e) {
            handleClassNotFound(e);
            throw new RuntimeException("Failed to invoke parent classloader", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to invoke parent classloader", e);
        }
    }

    private Method findNonPublicMethod(String name, Class klass, Class... args) {
        try {
            Method method = klass.getDeclaredMethod(name, args);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return findNonPublicMethod(name, klass.getSuperclass(), args);
        }
    }

    private void handleClassNotFound(InvocationTargetException e) throws ClassNotFoundException {
        if (e.getCause() instanceof ClassNotFoundException) {
            throw (ClassNotFoundException) e.getCause();
        }
    }

    private boolean existsInTfsJar(String name) {
        if (jarClassLoader == null) {
            return false;
        }
        String classAsResourceName = name.replace('.', '/') + ".class";
        if (isExcluded(classAsResourceName)) {
            return false;
        }
        URL url = jarClassLoader.getResource(classAsResourceName);
        LOGGER.debug("Loading {} from jar returned {} for url: {}  ", name, url != null, url);
        return url != null;
    }

    @Override
    public URL getResource(String name) {
        if (isExcluded(name)) {
            return parentClassLoader.getResource(name);
        }
        return null;
    }

    private boolean isExcluded(String name) {
        for (String excluded : excludes) {
            if (name.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

}
