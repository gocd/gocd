/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Loads the classes from the given jars.
 */
public class NestedJarClassLoader extends ClassLoader {

    private static final Logger LOGGER = Logger.getLogger(NestedJarClassLoader.class);
    private final ClassLoader loaderForJar;
    private final ClassLoader myLoader;
    private final String[] excludes;

    public NestedJarClassLoader(URL jarURL, String... excludes) {
        this(jarURL, NestedJarClassLoader.class.getClassLoader(), excludes);
    }

    NestedJarClassLoader(URL jarURL, ClassLoader parent, String... excludes) {
        super(null);
        myLoader = parent;
        loaderForJar = createLoaderForJar(jarURL);
        this.excludes = excludes;
    }

    private ClassLoader createLoaderForJar(URL jarURL) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating Loader For jar: " + jarURL);
        }
        ClassLoader jarLoader = new URLClassLoader(enumerateJar(jarURL), this);
        if (jarLoader == null) {
            LOGGER.warn("No jar found with url: " + jarURL);
        }
        return jarLoader;
    }

    private URL[] enumerateJar(URL urlOfJar) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Enumerating jar: " + urlOfJar);
        }
        List<URL> urls = new ArrayList<>();
        urls.add(urlOfJar);
        try {
            JarInputStream jarStream = new JarInputStream(urlOfJar.openStream());
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
                    urls.add(expandJarAndReturnURL(urlOfJar, entry));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to enumerate jar " + urlOfJar, e);
        }
        return urls.toArray(new URL[0]);
    }

    private URL expandJarAndReturnURL(URL urlOfJar, JarEntry entry) throws IOException {
        File nestedJarFile = File.createTempFile(new File(entry.getName()).getName(), ".jar");
        nestedJarFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(nestedJarFile);
        InputStream in = new URL("jar:file:" + urlOfJar.getFile() + "!/" + entry).openStream();
        IOUtils.copy(in, out);
        out.close();
        in.close();
        LOGGER.info(String.format("Exploded Entry %s from to %s", entry.getName(), nestedJarFile.toURI().toURL()));
        return nestedJarFile.toURI().toURL();
    }


    @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            return loaderForJar.loadClass(name);
        }
        return myLoader.loadClass(name);
    }

    @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            throw new ClassNotFoundException(name);
        }
        return invokeParentClassloader(name, resolve);
    }

    private Class<?> invokeParentClassloader(String name, Boolean resolve) throws ClassNotFoundException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Invoking parent classloader for %s with resolve %s", name, resolve));
        }
        try {
            Method loadClass = findNonPublicMethod("loadClass", myLoader.getClass(), String.class, boolean.class);
            return (Class<?>) loadClass.invoke(myLoader, name, (Boolean) resolve);
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
        if (loaderForJar == null) {
            return false;
        }
        String classAsResourceName = name.replace('.', '/') + ".class";
        if (isExcluded(classAsResourceName)) {
            return false;
        }
        URL url = loaderForJar.getResource(classAsResourceName);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Loading %s from jar returned %s for url: %s  ", name, url != null, url));
        }
        return url != null;
    }

    @Override public URL getResource(String name) {
        if (isExcluded(name)) {
            return myLoader.getResource(name);
        }
        return null;
    }

    private boolean isExcluded(String name) {
        for (String excluded : excludes) {
            if (name.contains(excluded)) {
                return true;
            }
        }
        return false;
    }

}
