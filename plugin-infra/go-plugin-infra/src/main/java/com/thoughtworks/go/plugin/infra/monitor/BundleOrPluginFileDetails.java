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
package com.thoughtworks.go.plugin.infra.monitor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Slf4j
@EqualsAndHashCode
@ToString
public class BundleOrPluginFileDetails {
    private static final String PLUGIN_XML = "plugin.xml";
    private static final String BUNDLE_XML = "gocd-bundle.xml";

    private final File file;
    private final File pluginWorkDir;
    private final boolean bundledPlugin;
    @EqualsAndHashCode.Exclude
    private final long lastModified;

    public BundleOrPluginFileDetails(File file, boolean bundledPlugin, File pluginWorkDir) {
        this.file = file;
        this.pluginWorkDir = pluginWorkDir;
        this.bundledPlugin = bundledPlugin;
        this.lastModified = file.lastModified();
    }

    public File file() {
        return file.getAbsoluteFile();
    }

    public boolean doesTimeStampDiffer(BundleOrPluginFileDetails other) {
        return lastModified != other.lastModified;
    }

    public boolean isBundledPlugin() {
        return bundledPlugin;
    }

    public boolean exists() {
        return file.exists();
    }

    public InputStream getBundleXml() {
        byte[] entry = getEntry(BUNDLE_XML);
        return new ByteArrayInputStream(entry == null ? new byte[0] : entry);
    }

    public InputStream getPluginXml() {
        byte[] entry = getEntry(PLUGIN_XML);
        return new ByteArrayInputStream(entry == null ? new byte[0] : entry);
    }

    private byte[] getEntry(String jarFileEntry) {
        try (JarFile jarFile = new JarFile(file)) {
            ZipEntry entry = jarFile.getEntry(jarFileEntry);
            if (entry != null) {
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    return inputStream.readAllBytes();
                }
            }
        } catch (IOException e) {
            log.warn("Could not load jar {}", file, e);
        }
        return null;
    }

    public File extractionLocation() {
        return new File(pluginWorkDir, file.getName());
    }

}
