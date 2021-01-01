/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.osgi.framework.Constants.*;

public class GoPluginOSGiManifest {
    private static final String BUNDLE_ROOT_DIR = ".";
    public static final String PLUGIN_DEPENDENCY_DIR = "lib";
    private static final String PLUGIN_DEPS_DIR_PREFIX = "," + PLUGIN_DEPENDENCY_DIR + "/";
    public static final String ACTIVATOR_JAR_NAME = "go-plugin-activator.jar";
    private static final String CLASSPATH_PREFIX = String.format("%s/%s,%s", PLUGIN_DEPENDENCY_DIR, ACTIVATOR_JAR_NAME, BUNDLE_ROOT_DIR);

    private GoPluginBundleDescriptor descriptor;
    private File manifestLocation;
    private File dependenciesDir;

    public GoPluginOSGiManifest(GoPluginBundleDescriptor descriptor) {
        this.descriptor = descriptor;
        manifestLocation = new File(descriptor.bundleLocation(), "META-INF/MANIFEST.MF");
        dependenciesDir = new File(descriptor.bundleLocation(), PLUGIN_DEPENDENCY_DIR);
    }

    public void update() throws IOException {
        String symbolicName = descriptor.bundleSymbolicName();
        String classPath = buildClassPath();
        String bundleActivator = DefaultGoPluginActivator.class.getCanonicalName();

        if (!manifestLocation.exists()) {
            manifestLocation.createNewFile();
        }

        updateManifest(symbolicName, classPath, bundleActivator);
    }

    private void updateManifest(String symbolicName, String classPath, String bundleActivator) throws IOException {
        try (FileInputStream manifestInputStream = new FileInputStream(manifestLocation)) {
            Manifest manifest = new Manifest(manifestInputStream);
            Attributes mainAttributes = manifest.getMainAttributes();

            if (mainAttributes.containsKey(new Attributes.Name(BUNDLE_SYMBOLICNAME))) {
                descriptor.markAsInvalid(Arrays.asList("Plugin JAR is invalid. MANIFEST.MF already contains header: " + BUNDLE_SYMBOLICNAME), null);
                return;
            }
            mainAttributes.put(new Attributes.Name(BUNDLE_SYMBOLICNAME), symbolicName);
            mainAttributes.put(new Attributes.Name(BUNDLE_CLASSPATH), classPath);
            mainAttributes.put(new Attributes.Name(BUNDLE_ACTIVATOR), bundleActivator);

            try (FileOutputStream manifestOutputStream = new FileOutputStream(manifestLocation)) {
                manifest.write(manifestOutputStream);
            }
        }
    }

    private String buildClassPath() {
        StringBuilder header = new StringBuilder(CLASSPATH_PREFIX);

        if (!dependenciesDir.exists() || !dependenciesDir.isDirectory()) {
            return header.toString();
        }

        Collection<File> dependencyJars = FileUtils.listFiles(dependenciesDir, new String[]{"jar"}, false);
        for (File dependencyJarFileName : dependencyJars) {
            if (!ACTIVATOR_JAR_NAME.equals(dependencyJarFileName.getName())) {
                header.append(PLUGIN_DEPS_DIR_PREFIX).append(dependencyJarFileName.getName());
            }
        }

        return header.toString();
    }

    @Component
    public static class DefaultGoPluginOSGiManifestCreator implements GoPluginOSGiManifestGenerator {
        public void updateManifestOf(GoPluginBundleDescriptor bundleDescriptor) {
            GoPluginOSGiManifest manifest = new GoPluginOSGiManifest(bundleDescriptor);
            try {
                manifest.update();
            } catch (IOException e) {
                throw new RuntimeException("Failed to update MANIFEST.MF", e);
            }
        }
    }
}
