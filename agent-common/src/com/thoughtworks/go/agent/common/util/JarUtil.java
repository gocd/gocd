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

package com.thoughtworks.go.agent.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JarUtil {
    private static final Log LOG = LogFactory.getLog(JarUtil.class);
    public static final String GO_VERSION = "Go-Version";
    private static final String EXPLODED_DEPENDENCIES_DIR_NAME = "exploded_agent_launcher_dependencies";

    public static String getGoVersion(String jar) {
        String version = getManifestKey(jar, GO_VERSION);
        return version == null ? "Unknown" : version;
    }

    public static String getManifestKey(String jar, String key) {
        String version = null;
        try {
            JarFile jarFile = new JarFile(jar);
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                version = attributes.getValue(key);
            }
        } catch (IOException e) {
            LOG.error("Exception while trying to read Go-Version from " + jar + ":" + e.toString());
        }
        return version;
    }

    public static Object objectFromJar(final String jarFileName, final String manifestClassKey) {
        return objectFromJar(jarFileName, manifestClassKey, null);
    }

    public static Object objectFromJar(String jarFileName, String manifestClassKey, String manifestLibDirKey, final Class... allowedForLoadingFromParent) {
        LOG.info(String.format("Attempting to load %s from %s File: ", manifestClassKey, jarFileName));
        try {
            File agentJar = new File(jarFileName);
            String absolutePath = agentJar.getAbsolutePath();
            List<URL> urls = new ArrayList<>();
            urls.add(agentJar.toURI().toURL());

            if (manifestLibDirKey != null) {
                String libDirPrefix = getManifestKey(absolutePath, manifestLibDirKey);
                LOG.info(String.format("manifestLibDirKey: %s: %s", manifestLibDirKey, libDirPrefix));
                prepareRefferedJars(jarFileName, absolutePath, urls, libDirPrefix);
            }
            ParentClassAccessFilteringClassloader filteringLoader = new ParentClassAccessFilteringClassloader(JarUtil.class.getClassLoader(), allowedForLoadingFromParent);
            URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), filteringLoader);
            String bootClassName = getManifestKey(absolutePath, manifestClassKey);
            LOG.info(String.format("manifestClassKey: %s: %s", manifestClassKey, bootClassName));
            return classLoader.loadClass(bootClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void prepareRefferedJars(String jarFileName, String absolutePath, List<URL> urlsExtracted, String libDirPrefix) throws IOException {
        JarFile jarFile = new JarFile(absolutePath);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String entryName = jarEntry.getName();
            if (entryName.startsWith(libDirPrefix) && entryName.endsWith(".jar")) {
                File depsDir = new File(EXPLODED_DEPENDENCIES_DIR_NAME, jarFileName);
                depsDir.mkdirs();
                String entryBaseName = entryName.replaceAll(".*/", "");
                File extractedJar = new File(depsDir, entryBaseName);
                String escapedJarURL = new File(absolutePath).toURI().toURL().toExternalForm();
                InputStream jarStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    URL jarUrl = new URL("jar:" + escapedJarURL + "!/" + entryName);
                    URLConnection conn = jarUrl.openConnection();
                    conn.setUseCaches(false);
                    jarStream = conn.getInputStream();
                    fileOutputStream = new FileOutputStream(extractedJar);
                    org.apache.commons.io.IOUtils.copyLarge(jarStream, fileOutputStream);
                } finally {
                    try {
                        if (jarStream != null) {
                            jarStream.close();
                        }
                    } finally {
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                    }
                }
                urlsExtracted.add(extractedJar.toURI().toURL());
            }
        }
    }

    public static boolean cleanup(String inUseLauncher) {
        File depsDir = new File(EXPLODED_DEPENDENCIES_DIR_NAME, inUseLauncher);
        return FileUtils.deleteQuietly(depsDir);
    }

    public static boolean tempFileExist(String inUseLauncher) {
        File depsDir = new File(EXPLODED_DEPENDENCIES_DIR_NAME, inUseLauncher);
        return depsDir.exists();
    }
}
