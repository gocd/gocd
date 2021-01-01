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

import com.thoughtworks.go.plugin.FileHelper;
import com.thoughtworks.go.plugin.infra.monitor.BundleOrPluginFileDetails;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    }

    @Test
    void shouldCreateThePluginDescriptorFromGivenPluginJarWithPluginXML() throws Exception {
        String pluginJarName = "descriptor-aware-test-plugin.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);
        BundleOrPluginFileDetails bundleOrPluginFileDetails = new BundleOrPluginFileDetails(pluginJarFile, true, pluginDirectory);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(bundleOrPluginFileDetails);
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
        BundleOrPluginFileDetails bundleOrPluginFileDetails = new BundleOrPluginFileDetails(pluginJarFile, true, pluginDirectory);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(bundleOrPluginFileDetails);
        List<GoPluginDescriptor> descriptors = bundleDescriptor.descriptors();

        GoPluginDescriptor expectedDescriptor = buildXMLSchemaErrorDescriptor(pluginJarName);
        assertThat(descriptors.size()).isEqualTo(1);
        assertThat(descriptors.get(0)).isEqualTo(expectedDescriptor);
        assertThat(descriptors.get(0).isInvalid()).isTrue();
        assertThat(descriptors.get(0).isBundledPlugin()).isTrue();
        assertThat(descriptors.get(0).getStatus().getMessages()).isEqualTo(expectedDescriptor.getStatus().getMessages());
    }

    @Test
    void shouldCreateInvalidPluginDescriptorEvenIfPluginXMLIsNotFound() throws Exception {
        String pluginJarName = "descriptor-aware-test-plugin-with-no-plugin-xml.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);
        BundleOrPluginFileDetails bundleOrPluginFileDetails = new BundleOrPluginFileDetails(pluginJarFile, true, pluginDirectory);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(bundleOrPluginFileDetails);

        final List<GoPluginDescriptor> descriptors = bundleDescriptor.descriptors();
        assertThat(descriptors.size()).isEqualTo(1);
        assertThat(descriptors.get(0).isInvalid()).isTrue();
        assertThat(descriptors.get(0).id()).isEqualTo(pluginJarName);
    }

    @Test
    void shouldCheckForBundleXMLFirst() throws Exception {
        String pluginJarName = "test-plugin-with-both-bundle-and-plugin-xmls.jar";
        copyPluginToThePluginDirectory(pluginDirectory, pluginJarName);
        File pluginJarFile = new File(pluginDirectory, pluginJarName);
        BundleOrPluginFileDetails bundleOrPluginFileDetails = new BundleOrPluginFileDetails(pluginJarFile, true, pluginDirectory);

        final GoPluginBundleDescriptor bundleDescriptor = goPluginBundleDescriptorBuilder.build(bundleOrPluginFileDetails);

        GoPluginBundleDescriptor expectedDescriptor = buildExpectedMultiPluginBundleDescriptor(pluginJarName, pluginJarFile.getAbsolutePath());
        assertThat(bundleDescriptor).isEqualTo(expectedDescriptor);
        assertThat(bundleDescriptor.isInvalid()).isFalse();
        assertThat(bundleDescriptor.isBundledPlugin()).isTrue();
    }

    @Test
    void shouldThrowExceptionForInvalidPluginIfThePluginJarDoesNotExist() {
        BundleOrPluginFileDetails bundleOrPluginFileDetails = new BundleOrPluginFileDetails(new File("not-existing.jar"), true, pluginDirectory);
        assertThatCode(() -> goPluginBundleDescriptorBuilder.build(bundleOrPluginFileDetails))
                .isInstanceOf(RuntimeException.class);
    }

    private void copyPluginToThePluginDirectory(File pluginDir,
                                                String destinationFilenameOfPlugin) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("defaultFiles/" + destinationFilenameOfPlugin);
        FileUtils.copyURLToFile(resource, new File(pluginDir, destinationFilenameOfPlugin));
    }

    private GoPluginDescriptor buildExpectedDescriptor(String fileName, String pluginJarFileLocation) {
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

        return GoPluginDescriptor.builder()
                .id(TESTPLUGIN_ID)
                .version("1")
                .pluginJarFileLocation(pluginJarFileLocation)
                .bundleLocation(new File(bundleDirectory, fileName))
                .isBundledPlugin(true)
                .about(GoPluginDescriptor.About.builder()
                        .name("Plugin Descriptor Validator")
                        .version("1.0.1")
                        .targetGoVersion("17.12")
                        .vendor(new GoPluginDescriptor.Vendor("ThoughtWorks GoCD Team", "www.thoughtworks.com"))
                        .targetOperatingSystems(List.of(new String[]{"Linux", "Windows", "Mac OS X"}))
                        .description("Validates its own plugin descriptor").build()
                ).build();

    }

    private GoPluginBundleDescriptor buildExpectedMultiPluginBundleDescriptor(String fileName,
                                                                              String pluginJarFileLocation) {

        final GoPluginDescriptor descriptor1 = GoPluginDescriptor.builder()
                .version("1")
                .id("testplugin.multipluginbundle.plugin1")
                .version(null)
                .pluginJarFileLocation(pluginJarFileLocation)
                .bundleLocation(new File(bundleDirectory, fileName))
                .isBundledPlugin(true)
                .about(GoPluginDescriptor.About.builder()
                        .name("Plugin 1")
                        .version("1.0.0")
                        .targetGoVersion("19.5")
                        .vendor(new GoPluginDescriptor.Vendor("ThoughtWorks GoCD Team", "www.thoughtworks.com"))
                        .targetOperatingSystems(List.of(new String[]{"Linux", "Windows"}))
                        .description("Example plugin 1").build()
                )
                .extensionClasses(List.of("cd.go.contrib.package1.TaskExtension", "cd.go.contrib.package1.ElasticAgentExtension"))
                .build();

        final GoPluginDescriptor descriptor2 = GoPluginDescriptor.builder()
                .id("testplugin.multipluginbundle.plugin2")
                .version(null)
                .pluginJarFileLocation(pluginJarFileLocation)
                .bundleLocation(new File(bundleDirectory, fileName))
                .isBundledPlugin(true)
                .about(GoPluginDescriptor.About.builder()
                        .name("Plugin 2")
                        .version("2.0.0")
                        .targetGoVersion("19.5")
                        .vendor(new GoPluginDescriptor.Vendor("Some other org", "www.example.com"))
                        .targetOperatingSystems(List.of(new String[]{"Linux"}))
                        .description("Example plugin 2").build()
                )
                .extensionClasses(List.of("cd.go.contrib.package2.TaskExtension", "cd.go.contrib.package2.AnalyticsExtension"))
                .build();

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

        String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
        final GoPluginDescriptor goPluginDescriptor = GoPluginDescriptor.builder()
                .id(pluginJarFile.getName())
                .bundleLocation(new File(bundleDirectory, name))
                .pluginJarFileLocation(pluginJarFileLocation)
                .isBundledPlugin(true)
                .build();
        return new GoPluginBundleDescriptor(goPluginDescriptor).markAsInvalid(singletonList(message), null).descriptors().get(0);
    }
}
