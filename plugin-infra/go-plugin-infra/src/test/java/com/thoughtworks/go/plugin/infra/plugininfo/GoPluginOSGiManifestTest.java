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

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.osgi.framework.Constants.*;

public class GoPluginOSGiManifestTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static OSChecker WINDOWS = new OSChecker(OSChecker.WINDOWS);
    private File tmpDir;
    private File manifestFile;
    private File bundleLocation;
    private File bundleDependencyDir;
    private GoPluginOSGiManifestGenerator goPluginOSGiManifestGenerator;

    @Before
    public void setUp() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        tmpDir = temporaryFolder.newFolder();
        bundleLocation = createPluginBundle("test-plugin-bundle");
        manifestFile = new File(bundleLocation, "META-INF/MANIFEST.MF");

        bundleDependencyDir = new File(bundleLocation, "lib");
        goPluginOSGiManifestGenerator = new DefaultGoPluginOSGiManifestCreator();
    }

    @Test
    public void shouldCreateABundleManifestFromTheGivenPluginDescriptor() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is(nullValue()));
        assertThat(valueFor(BUNDLE_CLASSPATH), is(nullValue()));

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        GoPluginOSGiManifest manifest = new GoPluginOSGiManifest(descriptor);
        manifest.update();

        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is("pluginId"));
        assertThat(valueFor(BUNDLE_ACTIVATOR), is(DefaultGoPluginActivator.class.getCanonicalName()));
        assertThat(valueFor(BUNDLE_CLASSPATH), is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));

        assertThat(descriptor.bundleSymbolicName(), is("pluginId"));
        assertThat(descriptor.bundleClassPath(), is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));
        assertThat(descriptor.bundleActivator(), is(DefaultGoPluginActivator.class.getCanonicalName()));
    }

    @Test
    public void shouldAddGoPluginActivatorJarToDependenciesOnlyOnceAtTheBeginning() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/go-plugin-activator.jar"), "Some data", UTF_8);

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        String classpathEntry = valueFor(BUNDLE_CLASSPATH);
        assertThat(classpathEntry, is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));
    }

    @Test
    public void shouldCreateManifestWithProperClassPathForAllDependencyJarsInPluginDependenciesDirectory() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/dependency-1.jar"), "Some data", UTF_8);
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/dependency-2.jar"), "Some data", UTF_8);
        FileUtils.writeStringToFile(new File(bundleLocation, "lib/dependency-3.jar"), "Some data", UTF_8);

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is("pluginId"));

        String classpathEntry = valueFor(BUNDLE_CLASSPATH);
        assertThat(classpathEntry, startsWith("lib/go-plugin-activator.jar,.,"));
        assertThat(classpathEntry, containsString(",lib/dependency.jar"));
        assertThat(classpathEntry, containsString(",lib/dependency-1.jar"));
        assertThat(classpathEntry, containsString(",lib/dependency-2.jar"));
        assertThat(classpathEntry, containsString(",lib/dependency-3.jar"));
    }

    @Test
    public void shouldCreateManifestWithProperClassPathWhenDependencyDirDoesNotExist() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.deleteDirectory(bundleDependencyDir);

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_CLASSPATH), is("lib/go-plugin-activator.jar,."));
    }

    @Test
    public void shouldMarkThePluginInvalidIfItsManifestAlreadyContainsSymbolicName() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        addHeaderToManifest(BUNDLE_SYMBOLICNAME, "Dummy Value");

        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is(not(nullValue())));
        assertThat(valueFor(BUNDLE_CLASSPATH), is(nullValue()));

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is("Dummy Value"));
        assertThat(descriptor.isInvalid(), is(true));
    }

    @Test
    public void shouldOverrideTheBundleClassPathInTheManifestIfItAlreadyHasIt() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        addHeaderToManifest(BUNDLE_CLASSPATH, "Dummy Value");

        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is(nullValue()));
        assertThat(valueFor(BUNDLE_CLASSPATH), is(not(nullValue())));

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_CLASSPATH), is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));
        assertThat(descriptor.isInvalid(), is(false));
    }

    @Test
    public void shouldOverrideTheBundleActivatorInTheManifestIfItAlreadyHasIt() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        addHeaderToManifest(BUNDLE_ACTIVATOR, "Dummy Value");
        assertThat(valueFor(BUNDLE_ACTIVATOR), is(not(nullValue())));

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_ACTIVATOR), is(DefaultGoPluginActivator.class.getCanonicalName()));
        assertThat(descriptor.isInvalid(), is(false));
    }

    @Test
    public void shouldCreateManifestWithProperClassPathWhenDependencyDirIsEmpty() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        FileUtils.deleteQuietly(new File(bundleLocation, "lib/dependency.jar"));
        assertThat(bundleDependencyDir.listFiles().length, is(0));

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_CLASSPATH), is("lib/go-plugin-activator.jar,."));
    }

    @Test
    public void manifestCreatorShouldUpdateTheGoPluginManifest() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is(nullValue()));
        assertThat(valueFor(BUNDLE_CLASSPATH), is(nullValue()));

        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("pluginId", "some-plugin.jar", bundleLocation, true);
        goPluginOSGiManifestGenerator.updateManifestOf(descriptor);

        assertThat(valueFor(BUNDLE_SYMBOLICNAME), is("pluginId"));
        assertThat(valueFor(BUNDLE_CLASSPATH), is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));
    }

    @After
    public void tearDown() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
    }

    private String valueFor(final String prefix) throws IOException {
        if (!manifestFile.exists()) {
            return null;
        }

        FileInputStream manifestInputStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestInputStream);
        Attributes entries = manifest.getMainAttributes();

        return entries.getValue(prefix);
    }

    private void addHeaderToManifest(String header, String value) throws IOException {
        FileInputStream manifestInputStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(manifestInputStream);
        Attributes entries = manifest.getMainAttributes();
        entries.put(new Attributes.Name(header), value);

        FileOutputStream manifestOutputStream = new FileOutputStream(manifestFile, false);
        manifest.write(manifestOutputStream);
        manifestOutputStream.close();
        manifestInputStream.close();
    }

    private File createPluginBundle(String bundleName) throws IOException, URISyntaxException {
        File destinationPluginBundleLocation = new File(tmpDir, bundleName);
        destinationPluginBundleLocation.mkdirs();

        URL resource = getClass().getClassLoader().getResource("defaultFiles/descriptor-aware-test-plugin.jar");
        new ZipUtil().unzip(new File(resource.toURI()), destinationPluginBundleLocation);
        return destinationPluginBundleLocation;
    }
}
