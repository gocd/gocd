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

package com.thoughtworks.go.domain.materials.tfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.NestedJarClassLoader;
import com.thoughtworks.go.util.command.CommandArgument;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Builds the TFSSDK Commmand
 */
class TfsSDKCommandBuilder {

    private static final Logger LOGGER = Logger.getLogger(TfsSDKCommandBuilder.class);
    private final ClassLoader sdkLoader;
    private static TfsSDKCommandBuilder ME;

    private TfsSDKCommandBuilder() throws IOException, URISyntaxException {
        this.sdkLoader = initSdkLoader();
    }

    /*
     * Used in tests
     */
    @Deprecated TfsSDKCommandBuilder(ClassLoader sdkLoader) {
        this.sdkLoader = sdkLoader;
    }

    TfsCommand buildTFSSDKCommand(String materialFingerPrint, UrlArgument url, String domain, String userName,
                                  String password, final String computedWorkspaceName, String projectPath) {
        try {
            return instantitateAdapter(materialFingerPrint, url, domain, userName, password, computedWorkspaceName, projectPath);
        } catch (Exception e) {
            String message = "[TFS SDK] Could not create TFS SDK Command ";
            LOGGER.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private TfsCommand instantitateAdapter(String materialFingerPrint, UrlArgument url, String domain, String userName, String password, String computedWorkspaceName, String projectPath)
            throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(sdkLoader);
            Class<?> adapterClass = Class.forName("com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter", true, sdkLoader);
            Constructor<?> constructor = adapterClass.getConstructor(String.class, CommandArgument.class, String.class, String.class, String.class, String.class, String.class);
            return (TfsCommand) constructor.newInstance(materialFingerPrint, url, domain, userName, password, computedWorkspaceName, projectPath);
        } finally {
            Thread.currentThread().setContextClassLoader(tcl);
        }

    }

    static TfsSDKCommandBuilder getBuilder() throws IOException, URISyntaxException {
        if (ME == null) {
            synchronized (TfsSDKCommandBuilder.class) {
                if (ME == null) {
                    ME = new TfsSDKCommandBuilder();
                }
            }
        }
        return ME;
    }

    private ClassLoader initSdkLoader() throws URISyntaxException, IOException {
        URL jarURL = urlForSDKJar("lib/tfs-impl.jar");
        URL expandedJarUrl = expandJarAndReturnURL(jarURL);
        File tempFolder = FileUtil.createTempFolder();
        tempFolder.deleteOnExit();
        explodeNatives(expandedJarUrl, tempFolder);
        setNativePath(tempFolder);
        String useTheParentLog4jConfiguration = "log4j";
        return new NestedJarClassLoader(expandedJarUrl, useTheParentLog4jConfiguration);
    }

    private void setNativePath(File tempFolder) {
        String sdkNativePath = tempFolder.getAbsolutePath() + File.separator + "tfssdk" + File.separator + "native";
        LOGGER.info("[TFS SDK] Setting native lib path, com.microsoft.tfs.jni.native.base-directory=" + sdkNativePath);
        System.setProperty("com.microsoft.tfs.jni.native.base-directory", sdkNativePath);
    }

    private void explodeNatives(URL urlOfJar, File tempFolder) throws IOException {
        LOGGER.info(String.format("[TFS SDK] Exploding natives from %s to folder %s", urlOfJar.toString(), tempFolder.getAbsolutePath()));
        JarInputStream jarStream = new JarInputStream(urlOfJar.openStream());
        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().contains("native")) {
                expandFile(urlOfJar, entry, tempFolder);
            }
        }
    }

    private void expandFile(URL urlOfJar, JarEntry entry, File tempFolder) throws IOException {
        LOGGER.info(String.format("[TFS SDK] Exploding File %s from %s to folder %s", entry.getName(), urlOfJar.toString(), tempFolder.getAbsolutePath()));
        File f = new File(tempFolder + File.separator + entry.getName());
        f.getParentFile().mkdirs();
        FileOutputStream out = null;
        InputStream in = null;
        try {
            out = new FileOutputStream(f);
            in = new URL("jar:file:" + urlOfJar.getFile() + "!/" + entry).openStream();
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }

    }

    private URL expandJarAndReturnURL(URL urlOfJar) throws IOException {
        File nestedJarFile = File.createTempFile(new File(urlOfJar.toExternalForm()).getName(), ".jar");
        nestedJarFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(nestedJarFile);
        InputStream in = urlOfJar.openStream();
        IOUtils.copy(in, out);
        out.close();
        in.close();
        LOGGER.info(String.format("[TFS SDK] Exploded Jar %s from to %s", urlOfJar.toString(), nestedJarFile.toURI().toURL()));
        return nestedJarFile.toURI().toURL();
    }


    private URL urlForSDKJar(String jarName) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[TFS SDK] Searching the classpath for jar: " + jarName);
        }
        return this.getClass().getResource("/" + jarName);
    }
}
