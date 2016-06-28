/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.plugininfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;

import org.apache.commons.digester.Digester;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;


public final class GoPluginDescriptorParser {
/*
    <?xml version="1.0" encoding="utf-8" ?>
    <go-plugin id="testplugin.descriptorValidator" version="1">
        <about>
            <name>Plugin Descriptor Validator</name>
            <version>1.0.1</version>
            <target-go-version>12.4</target-go-version>
            <description>Validates its own plugin descriptor</description>
            <vendor>
                <name>ThoughtWorks Go Team</name>
                <url>www.thoughtworks.com</url>
            </vendor>
            <target-os>
              <value>Linux</value>
              <value>Windows</value>
            </target-os>
        </about>
    </go-plugin>
*/

    private GoPluginDescriptor.Vendor vendor;
    private GoPluginDescriptor.About about;
    private GoPluginDescriptor descriptor;
    private File pluginBundleLocation;
    private boolean isBundledPlugin;
    private List<String> targetOperatingSystems = new ArrayList<>();
    private String pluginJarFileLocation;

    private GoPluginDescriptorParser(String pluginJarFileLocation, File pluginBundleLocation, boolean isBundledPlugin) {
        this.pluginJarFileLocation = pluginJarFileLocation;
        this.pluginBundleLocation = pluginBundleLocation;
        this.isBundledPlugin = isBundledPlugin;
    }

    public static GoPluginDescriptor parseXML(InputStream pluginXML, String pluginJarFileLocation, File pluginBundleLocation, boolean isBundledPlugin) throws IOException, SAXException {
        Digester digester = initDigester();
        GoPluginDescriptorParser parserForThisXML = new GoPluginDescriptorParser(pluginJarFileLocation, pluginBundleLocation, isBundledPlugin);
        digester.push(parserForThisXML);

        digester.addCallMethod("go-plugin", "createPlugin", 2);
        digester.addCallParam("go-plugin", 0, "id");
        digester.addCallParam("go-plugin", 1, "version");

        digester.addCallMethod("go-plugin/about", "createAbout", 4);
        digester.addCallParam("go-plugin/about/name", 0);
        digester.addCallParam("go-plugin/about/version", 1);
        digester.addCallParam("go-plugin/about/target-go-version", 2);
        digester.addCallParam("go-plugin/about/description", 3);

        digester.addCallMethod("go-plugin/about/vendor", "createVendor", 2);
        digester.addCallParam("go-plugin/about/vendor/name", 0);
        digester.addCallParam("go-plugin/about/vendor/url", 1);

        digester.addCallMethod("go-plugin/about/target-os/value", "addTargetOS", 1);
        digester.addCallParam("go-plugin/about/target-os/value", 0);

        digester.parse(pluginXML);

        return parserForThisXML.descriptor;
    }

    private static Digester initDigester() throws SAXNotRecognizedException, SAXNotSupportedException {
        Digester digester = new Digester();
        digester.setValidating(true);
        digester.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw new SAXException("XML Schema validation of Plugin Descriptor(plugin.xml) failed",exception);
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw new SAXException("XML Schema validation of Plugin Descriptor(plugin.xml) failed",exception);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw new SAXException("XML Schema validation of Plugin Descriptor(plugin.xml) failed",exception);
            }
        });
        digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource",
                GoPluginDescriptorParser.class.getResourceAsStream("/plugin-descriptor.xsd"));
        return digester;
    }

    //used by digester
    public void createPlugin(String id, String version) {
        descriptor = new GoPluginDescriptor(id, version, about, pluginJarFileLocation, pluginBundleLocation, isBundledPlugin);
    }

    //used by digester
    public void createVendor(String name, String url) {
        vendor = new GoPluginDescriptor.Vendor(name, url);
    }

    //used by digester
    public void createAbout(String name, String version, String targetGoVersion, String description) {
        about = new GoPluginDescriptor.About(name, version, targetGoVersion, description, vendor, targetOperatingSystems);
    }

    //used by digester
    public void addTargetOS(String os) {
        targetOperatingSystems.add(os);
    }
}
