/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra.plugininfo;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_WORK_DIR;
import static java.util.Collections.singletonList;

@Component
public class GoPluginBundleDescriptorBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoPluginBundleDescriptorBuilder.class);
    private static final String PLUGIN_XML = "plugin.xml";
    private static final String BUNDLE_XML = "gocd-bundle.xml";

    private SystemEnvironment systemEnvironment;
    private File pluginWorkDir;

    protected GoPluginBundleDescriptorBuilder() {
        this.systemEnvironment = new SystemEnvironment();
        pluginWorkDir = pluginWorkDir();
    }

    @Autowired
    public GoPluginBundleDescriptorBuilder(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        pluginWorkDir = pluginWorkDir();
    }

    public GoPluginBundleDescriptor build(File pluginJarFile, boolean isBundledPlugin) {
        if (!pluginJarFile.exists()) {
            throw new RuntimeException(String.format("Plugin jar does not exist: %s", pluginJarFile.getAbsoluteFile()));
        }

        try (JarFile jarFile = new JarFile(pluginJarFile)) {
            ZipEntry bundleXMLEntry = jarFile.getEntry(BUNDLE_XML);
            if (bundleXMLEntry != null) {
                try (InputStream bundleXMLStream = jarFile.getInputStream(bundleXMLEntry)) {
                    return GoPluginBundleDescriptorParser.parseXML(bundleXMLStream, pluginJarFile.getAbsolutePath(), getBundleLocation(pluginWorkDir, pluginJarFile.getName()), isBundledPlugin);
                }
            }

            ZipEntry pluginXMLEntry = jarFile.getEntry(PLUGIN_XML);
            if (pluginXMLEntry == null) {
                String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
                return new GoPluginBundleDescriptor(GoPluginDescriptor.builder()
                        .id(pluginJarFile.getName())
                        .bundleLocation(getBundleLocation(pluginWorkDir, pluginJarFile.getName()))
                        .pluginJarFileLocation(pluginJarFileLocation)
                        .pluginJarFileLocation(pluginJarFileLocation)
                        .isBundledPlugin(isBundledPlugin)
                        .build());
            }

            try (InputStream pluginXMLStream = jarFile.getInputStream(pluginXMLEntry)) {
                return GoPluginDescriptorParser.parseXML(pluginXMLStream, pluginJarFile.getAbsolutePath(), getBundleLocation(pluginWorkDir, pluginJarFile.getName()), isBundledPlugin);
            }

        } catch (Exception e) {
            LOGGER.warn("Could not load plugin with jar filename:{}", pluginJarFile.getName(), e);
            String cause = e.getCause() != null ? String.format("%s. Cause: %s", e.getMessage(), e.getCause().getMessage()) : e.getMessage();
            String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
            return new GoPluginBundleDescriptor(GoPluginDescriptor.builder()
                    .id(pluginJarFile.getName())
                    .bundleLocation(getBundleLocation(pluginWorkDir, pluginJarFile.getName()))
                    .pluginJarFileLocation(pluginJarFileLocation)
                    .pluginJarFileLocation(pluginJarFileLocation)
                    .isBundledPlugin(isBundledPlugin)
                    .build())
                    .markAsInvalid(singletonList(String.format("Plugin with ID (%s) is not valid: %s", pluginJarFile.getName(), cause)), e);
        }
    }

    private File getBundleLocation(File bundleDirectory, String name) {
        return new File(bundleDirectory, name);
    }

    File pluginWorkDir() {
        File workDir = new File(systemEnvironment.get(PLUGIN_WORK_DIR));
        FileUtil.validateAndCreateDirectory(workDir);
        return workDir;
    }
}
