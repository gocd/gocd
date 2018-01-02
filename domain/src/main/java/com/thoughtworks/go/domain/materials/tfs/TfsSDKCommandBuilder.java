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

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.NestedJarClassLoader;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandArgument;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Builds the TFSSDK Commmand
 */
class TfsSDKCommandBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TfsSDKCommandBuilder.class);
    private final File tempFolder = new File("data/tfs-sdk");
    private final ClassLoader sdkLoader;
    private static TfsSDKCommandBuilder ME;

    private TfsSDKCommandBuilder() throws IOException, URISyntaxException {
        this.sdkLoader = initSdkLoader();
    }

    /*
     * Used in tests
     */
    @Deprecated
    TfsSDKCommandBuilder(ClassLoader sdkLoader) {
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

    private TfsCommand instantitateAdapter(String materialFingerPrint, UrlArgument url, String domain, String userName, String password, String computedWorkspaceName, String projectPath) throws ReflectiveOperationException, IOException {
        Class<?> adapterClass = Class.forName(tfsSdkCommandTCLAdapterClassName(), true, sdkLoader);
        Constructor<?> constructor = adapterClass.getConstructor(String.class, CommandArgument.class, String.class, String.class, String.class, String.class, String.class);
        return (TfsCommand) constructor.newInstance(materialFingerPrint, url, domain, userName, password, computedWorkspaceName, projectPath);
    }

    private String tfsSdkCommandTCLAdapterClassName() {
        return "com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter";
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
        FileUtils.deleteQuietly(tempFolder);
        tempFolder.mkdirs();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(tempFolder);
            }
        });

        explodeNatives();
        setNativePath(tempFolder);
        return new NestedJarClassLoader(getJarURL(), "org/apache/log4j/", "org/apache/commons/logging/");
    }

    private void setNativePath(File tempFolder) {
        String sdkNativePath = Paths.get(tempFolder.getAbsolutePath(), "tfssdk", "native").toString();
        LOGGER.info("[TFS SDK] Setting native lib path, com.microsoft.tfs.jni.native.base-directory={}", sdkNativePath);
        System.setProperty("com.microsoft.tfs.jni.native.base-directory", sdkNativePath);
    }

    private void explodeNatives() throws IOException {
        URL urlOfJar = getJarURL();
        LOGGER.info("[TFS SDK] Exploding natives from {} to folder {}", urlOfJar.toString(), tempFolder.getAbsolutePath());
        JarInputStream jarStream = new JarInputStream(urlOfJar.openStream());
        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().startsWith("tfssdk/native/")) {
                File newFile = new File(tempFolder, entry.getName());
                newFile.getParentFile().mkdirs();
                LOGGER.info("[TFS SDK] Extract {} -> {}", entry.getName(), newFile);
                try (OutputStream fos = new FileOutputStream(newFile)) {
                    IOUtils.copy(jarStream, fos);
                }
            }
        }
    }

    private URL getJarURL() throws IOException {
        return TFSJarDetector.create(new SystemEnvironment()).getJarURL();
    }
}
