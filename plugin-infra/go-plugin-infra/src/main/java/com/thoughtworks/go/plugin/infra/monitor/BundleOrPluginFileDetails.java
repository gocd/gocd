/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

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

    public boolean isBundleJar() {
        return isNotEmpty(getEntryAsString(BUNDLE_XML));
    }

    public ByteArrayInputStream getBundleXml() {
        return new ByteArrayInputStream(getEntryAsString(BUNDLE_XML));
    }

    public boolean isPluginJar() {
        return isNotEmpty(getEntryAsString(PLUGIN_XML));
    }

    public ByteArrayInputStream getPluginXml() {
        return new ByteArrayInputStream(getEntryAsString(PLUGIN_XML));
    }

    private byte[] getEntryAsString(String jarFileEntry) {
        try (JarFile jarFile = new JarFile(file)) {
            ZipEntry entry = jarFile.getEntry(jarFileEntry);
            if (entry != null) {
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    return IOUtils.toByteArray(inputStream);
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
