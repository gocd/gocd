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

import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptorParser.parseXML;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class GoPluginBundleDescriptorParserTest {
    @Test
    void shouldParseValidVersionOfPluginBundleXML() throws Exception {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/valid-gocd-bundle.xml");
        final GoPluginBundleDescriptor bundle = parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true);
        assertEquals(2, bundle.descriptors().size());

        final GoPluginDescriptor pluginDescriptor1 = bundle.descriptors().get(0);
        assertPluginDescriptor(pluginDescriptor1, "testplugin.multipluginbundle.plugin1",
                "Plugin 1", "1.0.0", "19.5",
                "Example plugin 1", "ThoughtWorks GoCD Team", "www.thoughtworks.com", asList("Linux", "Windows"),
                asList("cd.go.contrib.package1.TaskExtension", "cd.go.contrib.package1.ElasticAgentExtension"));

        final GoPluginDescriptor pluginDescriptor2 = bundle.descriptors().get(1);
        assertPluginDescriptor(pluginDescriptor2, "testplugin.multipluginbundle.plugin2",
                "Plugin 2", "2.0.0", "19.5",
                "Example plugin 2", "Some other org", "www.example.com", singletonList("Linux"),
                asList("cd.go.contrib.package2.TaskExtension", "cd.go.contrib.package2.AnalyticsExtension"));
    }

    @Test
    void shouldNotAllowPluginWithEmptyListOfExtensionsInABundle() {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/gocd-bundle-with-no-extension-classes.xml");

        final JAXBException e = assertThrows(JAXBException.class, () ->
                parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true));
        assertTrue(e.getCause().getMessage().contains("The content of element 'extensions' is not complete. One of '{extension}' is expected"), format("Message not correct: [%s]", e.getCause().getMessage()));
    }

    @SuppressWarnings("SameParameterValue")
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
        assertEquals(pluginID, pluginDescriptor.id());
        assertEquals("/tmp/a.jar", pluginDescriptor.pluginJarFileLocation());
        assertEquals("tmp", pluginDescriptor.fileName());
        assertEquals(pluginName, pluginDescriptor.about().name());
        assertEquals(pluginVersion, pluginDescriptor.about().version());
        assertEquals(targetGoVersion, pluginDescriptor.about().targetGoVersion());
        assertEquals(description, pluginDescriptor.about().description());
        assertEquals(vendorName, pluginDescriptor.about().vendor().name());
        assertEquals(vendorURL, pluginDescriptor.about().vendor().url());
        assertEquals(targetOSes, pluginDescriptor.about().targetOperatingSystems());
        assertEquals(extensionClasses, pluginDescriptor.extensionClasses());
    }

}
