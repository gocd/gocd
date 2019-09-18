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

import com.thoughtworks.go.plugin.FileHelper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class GoPluginBundleDescriptorBuilderTest {
    private static final String TESTPLUGIN_ID = "testplugin.descriptorValidator";
    private GoPluginBundleDescriptorBuilder goPluginBundleDescriptorBuilder;
    private File pluginDirectory;
    private File bundleDirectory;

    @BeforeEach
    void setUp(@TempDir File rootDir) {
        final FileHelper temporaryFolder = new FileHelper(rootDir);
        pluginDirectory = temporaryFolder.newFolder("pluginDir");
        bundleDirectory = temporaryFolder.newFolder("bundleDir");

        goPluginBundleDescriptorBuilder = spy(new GoPluginBundleDescriptorBuilder());
        doReturn(bundleDirectory).when(goPluginBundleDescriptorBuilder).bundlePath();

    }

    @Test
    void shouldCreateThePluginDescriptorFromGivenPluginJarWithPluginXML() throws Exception {
        String pluginJarName = "descriptor-aware-test-plugin.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(pluginJarFile, true);
        final List<GoPluginDescriptor> descriptors = bundleDescriptor.descriptors();

        GoPluginDescriptor expectedDescriptor = buildExpectedDescriptor(pluginJarName, pluginJarFile.getAbsolutePath());

        assertThat(descriptors.size()).isEqualTo(1);
        assertThat(descriptors.get(0)).isEqualTo(expectedDescriptor);
        assertThat(descriptors.get(0).isInvalid()).isFalse();
        assertThat(descriptors.get(0).isBundledPlugin()).isTrue();
    }

    @Test
    void shouldCreateInvalidPluginDescriptorBecausePluginXMLDoesNotConformToXSD() throws Exception {
        String pluginJarName = "invalid-descriptor-plugin.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(pluginJarFile, true);
        List<GoPluginDescriptor> descriptors = bundleDescriptor.descriptors();

        GoPluginDescriptor expectedDescriptor = buildXMLSchemaErrorDescriptor(pluginJarName);
        assertThat(descriptors.size()).isEqualTo(1);
        assertThat(descriptors.get(0)).isEqualTo(expectedDescriptor);
        assertThat(descriptors.get(0).isInvalid()).isTrue();
        assertThat(descriptors.get(0).isBundledPlugin()).isTrue();
        assertThat(descriptors.get(0).getStatus().getMessages()).isEqualTo(expectedDescriptor.getStatus().getMessages());
    }

    @Test
    void shouldCreatePluginDescriptorEvenIfPluginXMLIsNotFound() throws Exception {
        String pluginJarName = "descriptor-aware-test-plugin-with-no-plugin-xml.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(pluginJarFile, false);

        final List<GoPluginDescriptor> descriptors = bundleDescriptor.descriptors();
        assertThat(descriptors.size()).isEqualTo(1);
        assertThat(descriptors.get(0).isInvalid()).isFalse();
        assertThat(descriptors.get(0).id()).isEqualTo(pluginJarName);
    }

    @Test
    void shouldCheckForBundleXMLFirst() throws Exception {
        String pluginJarName = "test-plugin-with-both-bundle-and-plugin-xmls.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(pluginJarFile, true);

        GoPluginBundleDescriptor expectedDescriptor = buildExpectedMultiPluginBundleDescriptor(pluginJarName, pluginJarFile.getAbsolutePath());
        assertThat(bundleDescriptor).isEqualTo(expectedDescriptor);
        assertThat(bundleDescriptor.isInvalid()).isFalse();
        assertThat(bundleDescriptor.isBundledPlugin()).isTrue();
    }

    @Test
    void shouldThrowExceptionForInvalidPluginIfThePluginJarDoesNotExist() {
        assertThatCode(() -> goPluginBundleDescriptorBuilder.build(new File(pluginDirectory, "invalid"), true))
                .isInstanceOf(RuntimeException.class);
    }

    private void copyPluginToThePluginDirectory(File pluginDir, String destinationFilenameOfPlugin) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("defaultFiles/" + destinationFilenameOfPlugin);
        FileUtils.copyURLToFile(resource, new File(pluginDir, destinationFilenameOfPlugin));
    }

    private GoPluginDescriptor buildExpectedDescriptor(String name, String pluginJarFileLocation) {
        /*
            <?xml version="1.0" encoding="utf-8" ?>
            <go-plugin id="testplugin.descriptorValidator" version="1">
               <about>
                 <name>Plugin Descriptor Validator</name>
                 <version>1.0.1</version>
                 <target-go-version>17.12</target-go-version>
                 <description>Validates its own plugin descriptor</description>
                 <vendor>
                   <name>ThoughtWorks GoCD Team</name>
                   <url>www.thoughtworks.com</url>
                 </vendor>
                 <target-os>
                   <value>Linux</value>
                   <value>Windows</value>
                 </target-os>
               </about>
            </go-plugin>
        */
        return new GoPluginDescriptor(TESTPLUGIN_ID, "1",
                new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "17.12", "Validates its own plugin descriptor",
                        new GoPluginDescriptor.Vendor("ThoughtWorks GoCD Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X")), pluginJarFileLocation,
                new File(bundleDirectory, name),
                true);
    }

    private GoPluginBundleDescriptor buildExpectedMultiPluginBundleDescriptor(String name, String pluginJarFileLocation) {
        final GoPluginDescriptor descriptor1 = new GoPluginDescriptor("testplugin.multipluginbundle.plugin1", "1",
                new GoPluginDescriptor.About("Plugin 1", "1.0.0", "19.5", "Example plugin 1",
                        new GoPluginDescriptor.Vendor("ThoughtWorks GoCD Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows")), pluginJarFileLocation,
                new File(bundleDirectory, name), true);
        descriptor1.addExtensionClasses(asList("cd.go.contrib.package1.TaskExtension", "cd.go.contrib.package1.ElasticAgentExtension"));

        final GoPluginDescriptor descriptor2 = new GoPluginDescriptor("testplugin.multipluginbundle.plugin2", "1",
                new GoPluginDescriptor.About("Plugin 2", "2.0.0", "19.5", "Example plugin 2",
                        new GoPluginDescriptor.Vendor("Some other org", "www.example.com"), singletonList("Linux")), pluginJarFileLocation,
                new File(bundleDirectory, name), true);
        descriptor2.addExtensionClasses(asList("cd.go.contrib.package2.TaskExtension", "cd.go.contrib.package2.AnalyticsExtension"));

        return new GoPluginBundleDescriptor(descriptor1, descriptor2);
    }

    private GoPluginDescriptor buildXMLSchemaErrorDescriptor(String name) {
        /*
            <?xml version="1.0" encoding="utf-8" ?>
            <go-plugin id="testplugin.descriptorValidator" version="1">
               <about>
                 <name>Plugin Descriptor Validator</name>
                 <version>1.0.1</version>
                 <target-go-version>17.12</target-go-version>
                 <description>Validates its own plugin descriptor</description>
                 <vendor>
                   <name>ThoughtWorks GoCD Team</name>
                   <url>www.thoughtworks.com</url>
                 </vendor>
                 <target-os>
                   <value>Linux</value>
                   <value>Windows</value>
                 </target-os>
                 <target-os> // this tag is repeated - this is invalid
                   <value>Linux</value>
                   <value>Windows</value>
                 </target-os>
               </about>
            </go-plugin>
        */
        File pluginJarFile = new File(pluginDirectory, name);

        final String message = String.format("Plugin with ID (%s) is not valid: %s.", pluginJarFile.getName(),
                "XML Schema validation of Plugin Descriptor(plugin.xml) failed. Cause: cvc-complex-type.2.4.d: Invalid content was found starting with element 'target-os'. No child element is expected at this point");

        final GoPluginDescriptor goPluginDescriptor = GoPluginDescriptor.usingId(pluginJarFile.getName(), pluginJarFile.getAbsolutePath(), new File(bundleDirectory, name), true);
        return new GoPluginBundleDescriptor(goPluginDescriptor).markAsInvalid(singletonList(message), null).descriptors().get(0);
    }
}
