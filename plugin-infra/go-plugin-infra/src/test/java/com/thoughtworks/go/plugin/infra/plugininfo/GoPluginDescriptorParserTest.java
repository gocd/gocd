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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class GoPluginDescriptorParserTest {
    @Test
    void shouldPerformPluginXsdValidationAndFailWhenIDIsNotPresent() {
        InputStream pluginXml = IOUtils.toInputStream("<go-plugin version=\"1\"></go-plugin>", StandardCharsets.UTF_8);
        final JAXBException e = assertThrows(JAXBException.class, () ->
                GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/", new File("/tmp/"), true));
        assertTrue(e.getCause().getMessage().contains("Attribute 'id' must appear on element 'go-plugin'"), format("Message not correct: [%s]", e.getCause().getMessage()));
    }

    @Test
    void shouldPerformPluginXsdValidationAndFailWhenVersionIsNotPresent() {
        InputStream pluginXml = IOUtils.toInputStream("<go-plugin id=\"some\"></go-plugin>", StandardCharsets.UTF_8);
        final JAXBException e = assertThrows(JAXBException.class, () ->
                GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/", new File("/tmp/"), true));
        assertTrue(e.getCause().getMessage().contains("Attribute 'version' must appear on element 'go-plugin'"), format("Message not correct: [%s]", e.getCause().getMessage()));
    }

    @Test
    void shouldValidatePluginVersion() {
        InputStream pluginXml = IOUtils.toInputStream("<go-plugin version=\"10\"></go-plugin>", StandardCharsets.UTF_8);
        final JAXBException e = assertThrows(JAXBException.class, () ->
                GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/", new File("/tmp/"), true));
        assertTrue(e.getCause().getMessage().contains("Value '10' of attribute 'version' of element 'go-plugin' is not valid"), format("Message not correct: [%s]", e.getCause().getMessage()));
    }

    @Test
    void shouldParseValidVersionOfPluginXML() throws Exception {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/valid-plugin.xml");
        final GoPluginBundleDescriptor bundleDescriptor = GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true);

        assertEquals(1, bundleDescriptor.descriptors().size());

        final GoPluginDescriptor pluginDescriptor = bundleDescriptor.descriptors().get(0);
        assertPluginDescriptor(pluginDescriptor, "testplugin.descriptorValidator", "Plugin Descriptor Validator",
                "1.0.1", "17.12", "Validates its own plugin descriptor",
                "ThoughtWorks GoCD Team", "www.thoughtworks.com", asList("Linux", "Windows"));

        assertTrue(pluginDescriptor.extensionClasses().isEmpty());
    }

    @SuppressWarnings("SameParameterValue")
    private void assertPluginDescriptor(GoPluginDescriptor pluginDescriptor, String pluginID, String pluginName,
                                        String pluginVersion, String targetGoVersion, String description, String vendorName,
                                        String vendorURL, List<String> targetOSes) {
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
    }
}
