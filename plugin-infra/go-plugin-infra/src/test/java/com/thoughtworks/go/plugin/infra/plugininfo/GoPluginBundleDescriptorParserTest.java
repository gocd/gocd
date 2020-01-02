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

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptorParser.parseXML;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

class GoPluginBundleDescriptorParserTest {
    @Test
    void shouldParseValidVersionOfPluginBundleXML() throws IOException, SAXException {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/valid-gocd-bundle.xml");
        final GoPluginBundleDescriptor bundleDescriptor = parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true);

        assertThat(bundleDescriptor.descriptors().size()).isEqualTo(2);

        final GoPluginDescriptor pluginDescriptor1 = bundleDescriptor.descriptors().get(0);
        assertPluginDescriptor(pluginDescriptor1, "testplugin.multipluginbundle.plugin1",
                "Plugin 1", "1.0.0", "19.5",
                "Example plugin 1", "ThoughtWorks GoCD Team", "www.thoughtworks.com", asList("Linux", "Windows"),
                asList("cd.go.contrib.package1.TaskExtension", "cd.go.contrib.package1.ElasticAgentExtension"));


        final GoPluginDescriptor pluginDescriptor2 = bundleDescriptor.descriptors().get(1);
        assertPluginDescriptor(pluginDescriptor2, "testplugin.multipluginbundle.plugin2",
                "Plugin 2", "2.0.0", "19.5",
                "Example plugin 2", "Some other org", "www.example.com", singletonList("Linux"),
                asList("cd.go.contrib.package2.TaskExtension", "cd.go.contrib.package2.AnalyticsExtension"));
    }

    @Test
    void shouldNotAllowPluginWithEmptyListOfExtensionsInABundle() throws IOException {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/gocd-bundle-with-no-extension-classes.xml");

        try {
            GoPluginBundleDescriptorParser.parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true);
            fail("Expected this to throw an exception");
        } catch (SAXException e) {
            assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.2.4.b: The content of element 'extensions' is not complete. One of '{extension}' is expected.");
        }
    }

    private void assertPluginDescriptor(GoPluginDescriptor pluginDescriptor,
                                        String pluginID,
                                        String pluginName,
                                        String pluginVersion,
                                        String targetGoVersion,
                                        String description,
                                        String vendorName,
                                        String vendorURL,
                                        List<String> targetOSes,
                                        List<String> extensionClasses) {
        assertThat(pluginDescriptor.id()).isEqualTo(pluginID);
        assertThat(pluginDescriptor.pluginFileLocation()).isEqualTo("/tmp/a.jar");
        assertThat(pluginDescriptor.fileName()).isEqualTo("tmp");
        assertThat(pluginDescriptor.about().name()).isEqualTo(pluginName);
        assertThat(pluginDescriptor.about().version()).isEqualTo(pluginVersion);
        assertThat(pluginDescriptor.about().targetGoVersion()).isEqualTo(targetGoVersion);
        assertThat(pluginDescriptor.about().description()).isEqualTo(description);
        assertThat(pluginDescriptor.about().vendor().name()).isEqualTo(vendorName);
        assertThat(pluginDescriptor.about().vendor().url()).isEqualTo(vendorURL);
        assertThat(pluginDescriptor.about().targetOperatingSystems()).isEqualTo(targetOSes);
        assertThat(pluginDescriptor.extensionClasses()).isEqualTo(extensionClasses);
    }

}
