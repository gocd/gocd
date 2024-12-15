/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.plugin.infra.monitor.BundleOrPluginFileDetails;
import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;

/* Parses an XML of this kind (see below). Also see @GoPluginDescriptorParser and gocd-bundle-descriptor.xsd.

<gocd-bundle version="1">
  <plugins>
    <plugin id="testplugin.multipluginbundle.plugin1">
      <about>
        <name>Plugin 1</name>
        <version>1.0.0</version>
        <target-go-version>19.5</target-go-version>
        <description>Example plugin 1</description>
        <vendor>
          <name>GoCD Team</name>
          <url>https://gocd.org</url>
        </vendor>
        <target-os>
          <value>Linux</value>
          <value>Windows</value>
        </target-os>
      </about>

      <extensions>
        <extension class="cd.go.contrib.package1.TaskExtension" />
        <extension class="cd.go.contrib.package1.ElasticAgentExtension" />
      </extensions>
    </plugin>

    <plugin id="testplugin.multipluginbundle.part2">
      <about>
        <name>Plugin 2</name>
        <version>2.0.0</version>
        <target-go-version>19.5</target-go-version>
        <description>Example plugin 2</description>
        <vendor>
          <name>Some other org</name>
          <url>www.example.com</url>
        </vendor>
        <target-os>
          <value>Linux</value>
        </target-os>
      </about>

      <extensions>
        <extension class="cd.go.contrib.package2.TaskExtension" />
        <extension class="cd.go.contrib.package2.AnalyticsExtension" />
      </extensions>
    </plugin>
  </plugins>
</gocd-bundle>
*/

public final class GoPluginBundleDescriptorParser {
    private GoPluginBundleDescriptorParser() {
    }

    public static GoPluginBundleDescriptor parseXML(InputStream pluginXml,
                                                    BundleOrPluginFileDetails bundleOrPluginJarFile) throws JAXBException, XMLStreamException, SAXException {
        return parseXML(pluginXml, bundleOrPluginJarFile.file().getAbsolutePath(), bundleOrPluginJarFile.extractionLocation(), bundleOrPluginJarFile.isBundledPlugin());
    }

    static GoPluginBundleDescriptor parseXML(InputStream pluginXML,
                                             String pluginJarFileLocation,
                                             File pluginBundleLocation,
                                             boolean isBundledPlugin) throws JAXBException, XMLStreamException, SAXException {

        GoPluginBundleDescriptor bundle = GoPluginDescriptorParser.deserializeXML(pluginXML, GoPluginBundleDescriptor.class, "/gocd-bundle-descriptor.xsd", "bundle.xml");
        bundle.pluginDescriptors().forEach(d -> {
            d.setBundleDescriptor(bundle);
            d.version(bundle.version());
            d.pluginJarFileLocation(pluginJarFileLocation);
            d.bundleLocation(pluginBundleLocation);
            d.isBundledPlugin(isBundledPlugin);
        });
        return bundle;
    }

}
