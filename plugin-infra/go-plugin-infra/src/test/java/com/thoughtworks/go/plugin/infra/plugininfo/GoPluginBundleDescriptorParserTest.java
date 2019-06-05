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

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GoPluginBundleDescriptorParserTest {
    @Test
    public void shouldParseValidVersionOfPluginBundleXML() throws IOException, SAXException {
        InputStream pluginXml = getClass().getClassLoader().getResourceAsStream("defaultFiles/valid-gocd-bundle.xml");
        final GoPluginBundleDescriptor bundleDescriptor = GoPluginBundleDescriptorParser.parseXML(pluginXml, "/tmp/a.jar", new File("/tmp/"), true);

        assertThat(bundleDescriptor.descriptors().size(), is(2));


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

    private void assertPluginDescriptor(GoPluginDescriptor pluginDescriptor, String pluginID, String pluginName,
                                        String pluginVersion, String targetGoVersion, String description, String vendorName,
                                        String vendorURL, List<String> targetOSes, List<String> extensionClasses) {
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
        assertThat(pluginDescriptor.extensionClasses(), is(extensionClasses));
    }

}