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

import com.thoughtworks.go.plugin.infra.monitor.BundleOrPluginFileDetails;
import org.apache.commons.digester3.Digester;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
          <name>ThoughtWorks GoCD Team</name>
          <url>www.thoughtworks.com</url>
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
    private String pluginJarFileLocation;
    private File pluginBundleLocation;
    private boolean isBundledPlugin;

    private GoPluginBundleDescriptor descriptor;
    private List<GoPluginDescriptor> pluginDescriptors = new ArrayList<>();
    private InProgressAccumulator inProgressAccumulator;

    private GoPluginBundleDescriptorParser(String pluginJarFileLocation,
                                           File pluginBundleLocation,
                                           boolean isBundledPlugin) {
        this.pluginJarFileLocation = pluginJarFileLocation;
        this.pluginBundleLocation = pluginBundleLocation;
        this.isBundledPlugin = isBundledPlugin;

        inProgressAccumulator = new InProgressAccumulator();
    }

    public static GoPluginBundleDescriptor parseXML(InputStream pluginXml,
                                                    BundleOrPluginFileDetails bundleOrPluginJarFile) throws IOException, SAXException {
        return parseXML(pluginXml, bundleOrPluginJarFile.file().getAbsolutePath(), bundleOrPluginJarFile.extractionLocation(), bundleOrPluginJarFile.isBundledPlugin());
    }

    static GoPluginBundleDescriptor parseXML(InputStream pluginXML,
                                             String pluginJarFileLocation,
                                             File pluginBundleLocation,
                                             boolean isBundledPlugin) throws IOException, SAXException {
        Digester digester = initDigester();

        GoPluginBundleDescriptorParser parserForThisXML = new GoPluginBundleDescriptorParser(pluginJarFileLocation, pluginBundleLocation, isBundledPlugin);
        digester.push(parserForThisXML);

        digester.addCallMethod("gocd-bundle", "createBundle", 1);
        digester.addCallParam("gocd-bundle", 0, "version");

        digester.addCallMethod("gocd-bundle/plugins/plugin", "createPlugin", 1);
        digester.addCallParam("gocd-bundle/plugins/plugin", 0, "id");

        digester.addCallMethod("gocd-bundle/plugins/plugin/about", "createAbout", 4);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/name", 0);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/version", 1);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/target-go-version", 2);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/description", 3);

        digester.addCallMethod("gocd-bundle/plugins/plugin/about/vendor", "createVendor", 2);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/vendor/name", 0);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/vendor/url", 1);

        digester.addCallMethod("gocd-bundle/plugins/plugin/about/target-os/value", "addTargetOS", 1);
        digester.addCallParam("gocd-bundle/plugins/plugin/about/target-os/value", 0);

        digester.addCallMethod("gocd-bundle/plugins/plugin/extensions/extension", "addExtension", 1);
        digester.addCallParam("gocd-bundle/plugins/plugin/extensions/extension", 0, "class");

        digester.parse(pluginXML);

        return parserForThisXML.descriptor;
    }

    private static Digester initDigester() throws SAXNotRecognizedException, SAXNotSupportedException {
        Digester digester = new Digester();
        digester.setValidating(true);
        digester.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw new SAXException("XML Schema validation of GoCD Bundle Descriptor (bundle.xml) failed", exception);
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw new SAXException("XML Schema validation of GoCD Bundle Descriptor (bundle.xml) failed", exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw new SAXException("XML Schema validation of GoCD Bundle Descriptor (bundle.xml) failed", exception);
            }
        });
        digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource",
                GoPluginBundleDescriptorParser.class.getResourceAsStream("/gocd-bundle-descriptor.xsd"));
        return digester;
    }


    //used by digester
    public void createBundle(String version) {
        descriptor = new GoPluginBundleDescriptor(pluginDescriptors.toArray(new GoPluginDescriptor[0]));
    }

    //used by digester
    public void createPlugin(String id) {
        final GoPluginDescriptor descriptor = GoPluginDescriptor.builder()
                .id(id)
                .about(inProgressAccumulator.about)
                .pluginJarFileLocation(pluginJarFileLocation)
                .bundleLocation(pluginBundleLocation)
                .isBundledPlugin(isBundledPlugin)
                .extensionClasses(inProgressAccumulator.extensionClasses)
                .build();

        this.pluginDescriptors.add(descriptor);
        this.inProgressAccumulator = new InProgressAccumulator();
    }

    //used by digester
    public void createVendor(String name, String url) {
        inProgressAccumulator.vendor = new GoPluginDescriptor.Vendor(name, url);
    }

    //used by digester
    public void createAbout(String name, String version, String targetGoVersion, String description) {
        inProgressAccumulator.about = GoPluginDescriptor.About.builder()
                .name(name)
                .version(version)
                .targetGoVersion(targetGoVersion)
                .description(description)
                .vendor(inProgressAccumulator.vendor)
                .targetOperatingSystems(inProgressAccumulator.targetOperatingSystems).build();
    }

    //used by digester
    public void addTargetOS(String os) {
        inProgressAccumulator.targetOperatingSystems.add(os);
    }

    //used by digester
    public void addExtension(String klass) {
        inProgressAccumulator.extensionClasses.add(klass);
    }

    private class InProgressAccumulator {
        GoPluginDescriptor.Vendor vendor;
        GoPluginDescriptor.About about;
        List<String> targetOperatingSystems = new ArrayList<>();
        List<String> extensionClasses = new ArrayList<>();
    }
}
