/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Loads classes from the given jar (including jars nested within it) in preference to the parent classloader,
 * while classes and resources the jar does not contain resolve against the parent as usual. Resources are
 * never served from the parent, isolating the jar's contents.
 */
class NestedJarClassLoader extends ClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestedJarClassLoader.class);
    private final ClassLoader jarClassLoader;
    private final File jarDir;
    private static final File TEMP_DIR = new File("data/njcl");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtil.deleteQuietly(TEMP_DIR)));
    }

    NestedJarClassLoader(URL jarURL) {
        this(jarURL, NestedJarClassLoader.class.getClassLoader());
    }

    NestedJarClassLoader(URL jarURL, ClassLoader parentClassLoader) {
        super(parentClassLoader);
        this.jarDir = new File(TEMP_DIR, UUID.randomUUID().toString());
        this.jarClassLoader = createLoaderForJar(jarURL);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtil.deleteQuietly(jarDir)));
    }

    private ClassLoader createLoaderForJar(URL jarURL) {
        LOGGER.debug("Creating Loader For jar: {}", jarURL);
        return new URLClassLoader(enumerateJar(jarURL), this);
    }

    private URL[] enumerateJar(URL urlOfJar) {
        LOGGER.debug("Enumerating jar: {}", urlOfJar);
        List<URL> urls = new ArrayList<>();
        urls.add(urlOfJar);
        try (JarInputStream jarStream = new JarInputStream(urlOfJar.openStream())) {
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
            jarStream.transferTo(out);
        }
        LOGGER.info("Exploded Entry {} from to {}", entry.getName(), nestedJarFile);
        return nestedJarFile.toURI().toURL();
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            return jarClassLoader.loadClass(name);
        }
        return super.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (existsInTfsJar(name)) {
            throw new ClassNotFoundException(name);
        }
        return super.loadClass(name, resolve);
    }

    private boolean existsInTfsJar(String name) {
        if (jarClassLoader == null) {
            return false;
        }
        String classAsResourceName = name.replace('.', '/') + ".class";
        URL url = jarClassLoader.getResource(classAsResourceName);
        LOGGER.debug("Loading {} from jar returned {} for url: {}  ", name, url != null, url);
        return url != null;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

}
