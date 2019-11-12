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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static com.thoughtworks.go.util.TestUtils.contains;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GoPluginDescriptorParserTest {
    @Test
    public void shouldPerformPluginXsdValidationAndFailWhenIDIsNotPresent() throws Exception {
        InputStream pluginXml = IOUtils.toInputStream("<go-plugin version=\"1\"></go-plugin>", StandardCharsets.UTF_8);
        try {
            GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/", new File("/tmp/"), true);
            fail("xsd validation should have failed");
        } catch (SAXException e) {
           assertThat(e.getMessage(), is("XML Schema validation of Plugin Descriptor(plugin.xml) failed"));
        }
    }

    @Test
    public void shouldPerformPluginXsdValidationAndFailWhenVersionIsNotPresent() throws Exception {
        InputStream pluginXml = IOUtils.toInputStream("<go-plugin id=\"some\"></go-plugin>", StandardCharsets.UTF_8);
        try {
            GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/", new File("/tmp/"), true);
            fail("xsd validation should have failed");
        } catch (SAXException e) {
           assertThat(e.getMessage(), is("XML Schema validation of Plugin Descriptor(plugin.xml) failed"));
        }
    }

    @Test
    public void shouldValidatePluginVersion() throws Exception {
        InputStream pluginXml = IOUtils.toInputStream("<go-plugin version=\"10\"></go-plugin>", StandardCharsets.UTF_8);
        try {
            GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/", new File("/tmp/"), true);
            fail("xsd validation should have failed");
        } catch (SAXException e) {
            assertThat(e.getMessage(), is("XML Schema validation of Plugin Descriptor(plugin.xml) failed"));
            assertThat(e.getCause().getMessage(), contains("Value '10' of attribute 'version' of element 'go-plugin' is not valid"));
        }
    }

    @Test
    public void shouldParseValidVersionOfPluginXML() throws IOException, SAXException {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/valid-plugin.xml");
        final GoPluginBundleDescriptor bundleDescriptor = GoPluginDescriptorParser.parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true);

        assertThat(bundleDescriptor.descriptors().size(), is(1));

        final GoPluginDescriptor pluginDescriptor = bundleDescriptor.descriptors().get(0);
        assertPluginDescriptor(pluginDescriptor, "testplugin.descriptorValidator", "Plugin Descriptor Validator",
                "1.0.1", "17.12", "Validates its own plugin descriptor",
                "ThoughtWorks GoCD Team", "www.thoughtworks.com", asList("Linux", "Windows"));

        assertThat(pluginDescriptor.extensionClasses(), is(empty()));
    }

    private void assertPluginDescriptor(GoPluginDescriptor pluginDescriptor, String pluginID, String pluginName,
                                        String pluginVersion, String targetGoVersion, String description, String vendorName,
                                        String vendorURL, List<String> targetOSes) {
        assertThat(pluginDescriptor.id(), is(pluginID));
        assertThat(pluginDescriptor.pluginFileLocation(), is("/tmp/a.jar"));
        assertThat(pluginDescriptor.fileName(), is("tmp"));
        assertThat(pluginDescriptor.about().name(), is(pluginName));
        assertThat(pluginDescriptor.about().version(), is(pluginVersion));
        assertThat(pluginDescriptor.about().targetGoVersion(), is(targetGoVersion));
        assertThat(pluginDescriptor.about().description(), is(description));
        assertThat(pluginDescriptor.about().vendor().name(), is(vendorName));
        assertThat(pluginDescriptor.about().vendor().url(), is(vendorURL));
        assertThat(pluginDescriptor.about().targetOperatingSystems(), is(targetOSes));
    }
}
