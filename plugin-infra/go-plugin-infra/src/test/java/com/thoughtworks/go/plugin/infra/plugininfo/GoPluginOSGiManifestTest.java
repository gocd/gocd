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
package com.thoughtworks.go.plugin.infra.plugininfo;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.plugin.FileHelper;
import com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.framework.Constants.*;

class GoPluginOSGiManifestTest {
    private static final OSChecker WINDOWS = new OSChecker(OSChecker.WINDOWS);
    private File tmpDir;
    private File manifestFile;
    private File bundleLocation;
    private File bundleDependencyDir;
    private GoPluginOSGiManifestGenerator goPluginOSGiManifestGenerator;

    @BeforeEach
    void setUp(@TempDir File rootDir) throws Exception {
        final FileHelper temporaryFolder = new FileHelper(rootDir);
        if (WINDOWS.satisfy()) {
            return;
        }
        tmpDir = temporaryFolder.newFolder();
        bundleLocation = createPluginBundle("test-plugin-bundle");
        manifestFile = new File(bundleLocation, "META-INF/MANIFEST.MF");

        bundleDependencyDir = new File(bundleLocation, "lib");
        goPluginOSGiManifestGenerator = new GoPluginOSGiManifest.DefaultGoPluginOSGiManifestCreator();
    }

    @Test
    void shouldCreateABundleManifestFromTheGivenPluginDescriptor() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isNull();
        assertThat(valueFor(BUNDLE_CLASSPATH)).isNull();

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        GoPluginOSGiManifest manifest = new GoPluginOSGiManifest(descriptor);
        manifest.update();

        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isEqualTo("pluginId");
        assertThat(valueFor(BUNDLE_ACTIVATOR)).isEqualTo(DefaultGoPluginActivator.class.getCanonicalName());
        assertThat(valueFor(BUNDLE_CLASSPATH)).isEqualTo("lib/go-plugin-activator.jar,.,lib/dependency.jar");

        assertThat(descriptor.bundleSymbolicName()).isEqualTo("pluginId");
    }

    @Test
    void shouldAddGoPluginActivatorJarToDependenciesOnlyOnceAtTheBeginning() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/go-plugin-activator.jar"), "Some data", UTF_8);

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        String classpathEntry = valueFor(BUNDLE_CLASSPATH);
        assertThat(classpathEntry).isEqualTo("lib/go-plugin-activator.jar,.,lib/dependency.jar");
    }

    @Test
    void shouldCreateManifestWithProperClassPathForAllDependencyJarsInPluginDependenciesDirectory() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/dependency-1.jar"), "Some data", UTF_8);
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/dependency-2.jar"), "Some data", UTF_8);
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/dependency-3.jar"), "Some data", UTF_8);

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isEqualTo("pluginId");

        String classpathEntry = valueFor(BUNDLE_CLASSPATH);
        assertThat(classpathEntry).startsWith("lib/go-plugin-activator.jar,.,");
        assertThat(classpathEntry).contains(",lib/dependency.jar");
        assertThat(classpathEntry).contains(",lib/dependency-1.jar");
        assertThat(classpathEntry).contains(",lib/dependency-2.jar");
        assertThat(classpathEntry).contains(",lib/dependency-3.jar");
    }

    @Test
    void shouldCreateManifestWithProperClassPathWhenDependencyDirDoesNotExist() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.deleteDirectory(bundleDependencyDir);

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_CLASSPATH)).isEqualTo("lib/go-plugin-activator.jar,.");
    }

    @Test
    void shouldMarkThePluginInvalidIfItsManifestAlreadyContainsSymbolicName() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        addHeaderToManifest(BUNDLE_SYMBOLICNAME, "Dummy Value");

        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isNotNull();
        assertThat(valueFor(BUNDLE_CLASSPATH)).isNull();

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isEqualTo("Dummy Value");
        assertThat(descriptor.isInvalid()).isTrue();
    }

    @Test
    void shouldOverrideTheBundleClassPathInTheManifestIfItAlreadyHasIt() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        addHeaderToManifest(BUNDLE_CLASSPATH, "Dummy Value");

        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isNull();
        assertThat(valueFor(BUNDLE_CLASSPATH)).isNotNull();

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_CLASSPATH)).isEqualTo("lib/go-plugin-activator.jar,.,lib/dependency.jar");
        assertThat(descriptor.isInvalid()).isFalse();
    }

    @Test
    void shouldOverrideTheBundleActivatorInTheManifestIfItAlreadyHasIt() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        addHeaderToManifest(BUNDLE_ACTIVATOR, "Dummy Value");
        assertThat(valueFor(BUNDLE_ACTIVATOR)).isNotNull();

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_ACTIVATOR)).isEqualTo(DefaultGoPluginActivator.class.getCanonicalName());
        assertThat(descriptor.isInvalid()).isFalse();
    }

    @Test
    void shouldCreateManifestWithProperClassPathWhenDependencyDirIsEmpty() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.deleteQuietly(new File(bundleLocation, "lib/dependency.jar"));
        assertThat(bundleDependencyDir.listFiles().length).isEqualTo(0);

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_CLASSPATH)).isEqualTo("lib/go-plugin-activator.jar,.");
    }

    @Test
    void manifestCreatorShouldUpdateTheGoPluginManifest() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isNull();
        assertThat(valueFor(BUNDLE_CLASSPATH)).isNull();

        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("pluginId").pluginJarFileLocation("some-plugin.jar").bundleLocation(bundleLocation).isBundledPlugin(true).build());
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_SYMBOLICNAME)).isEqualTo("pluginId");
        assertThat(valueFor(BUNDLE_CLASSPATH)).isEqualTo("lib/go-plugin-activator.jar,.,lib/dependency.jar");
    }

    private String valueFor(final String prefix) throws IOException {
        if (!manifestFile.exists()) {
            return null;
        }

        try (FileInputStream manifestInputStream = new FileInputStream(manifestFile)) {
            Manifest manifest = new Manifest(manifestInputStream);
            Attributes entries = manifest.getMainAttributes();

            return entries.getValue(prefix);
        }
    }

    private void addHeaderToManifest(String header, String value) throws IOException {
        try (FileInputStream manifestInputStream = new FileInputStream(manifestFile)) {
            Manifest manifest = new Manifest(manifestInputStream);
            Attributes entries = manifest.getMainAttributes();
            entries.put(new Attributes.Name(header), value);

            try (FileOutputStream manifestOutputStream = new FileOutputStream(manifestFile, false)) {
                manifest.write(manifestOutputStream);
            }
        }
    }

    private File createPluginBundle(String bundleName) throws IOException, URISyntaxException {
        File destinationPluginBundleLocation = new File(tmpDir, bundleName);
        destinationPluginBundleLocation.mkdirs();

        URL resource = getClass().getClassLoader().getResource("defaultFiles/descriptor-aware-test-plugin.jar");
        new ZipUtil().unzip(new File(resource.toURI()), destinationPluginBundleLocation);
        return destinationPluginBundleLocation;
    }
}
