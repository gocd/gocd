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

package com.thoughtworks.go.agent.common.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class JarUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JarUtil.class);

    public static String getManifestKey(File aJarFile, String key) {
        try (JarFile jarFile = new JarFile(aJarFile)) {
            return getManifestKey(jarFile, key);
        } catch (IOException e) {
            LOG.error("Exception while trying to read key {} from manifest of {}", key, aJarFile, e);
        }
        return null;
    }

    private static String getManifestKey(JarFile jarFile, String key) {
        try {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                return attributes.getValue(key);
            }
        } catch (IOException e) {
            LOG.error("Exception while trying to read key {} from manifest of {}", key, jarFile.getName(), e);
        }
        return null;
    }

    private static File extractJarEntry(JarFile jarFile, JarEntry jarEntry, File targetFile) {
        LOG.debug("Extracting {}!/{} -> {}", jarFile, jarEntry, targetFile);
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            FileUtils.forceMkdirParent(targetFile);
            Files.copy(inputStream, targetFile.toPath());
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> extractFilesInLibDirAndReturnFiles(File aJarFile, Predicate<JarEntry> extractFilter, File outputTmpDir) {
        List<File> newClassPath = new ArrayList<>();
        try (JarFile jarFile = new JarFile(aJarFile)) {

            List<File> extractedJars = jarFile.stream()
                    .filter(extractFilter)
                    .map(jarEntry -> {
                        String jarFileBaseName = FilenameUtils.getName(jarEntry.getName());
                        File targetFile = new File(outputTmpDir, jarFileBaseName);
                        return extractJarEntry(jarFile, jarEntry, targetFile);
                    })
                    .collect(Collectors.toList());

            // add deps in dir specified by `libDirManifestKey`
            newClassPath.addAll(extractedJars);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newClassPath;
    }

    public static URL[] toURLs(List<File> files) {
        return files.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()).toArray(new URL[0]);
    }

    public static URLClassLoader getClassLoaderFromJar(File aJarFile, Predicate<JarEntry> extractFilter, File outputTmpDir, ClassLoader parentClassLoader, Class... allowedClasses) {
        List<File> urls = new ArrayList<>();
        urls.add(aJarFile);
        urls.addAll(extractFilesInLibDirAndReturnFiles(aJarFile, extractFilter, outputTmpDir));
        ParentClassAccessFilteringClassloader filteringClassloader = new ParentClassAccessFilteringClassloader(parentClassLoader, allowedClasses);
        return new URLClassLoader(toURLs(urls), filteringClassloader);
    }
}
