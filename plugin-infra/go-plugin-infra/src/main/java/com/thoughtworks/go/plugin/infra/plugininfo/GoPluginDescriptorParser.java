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
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class GoPluginDescriptorParser {
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

    private GoPluginDescriptorParser() {
    }

    public static GoPluginBundleDescriptor parseXML(InputStream pluginXml,
                                                    BundleOrPluginFileDetails bundleOrPluginJarFile) throws IOException, JAXBException, XMLStreamException, SAXException {
        return parseXML(pluginXml, bundleOrPluginJarFile.file().getAbsolutePath(), bundleOrPluginJarFile.extractionLocation(), bundleOrPluginJarFile.isBundledPlugin());
    }

    static GoPluginBundleDescriptor parseXML(InputStream pluginXML,
                                             String pluginJarFileLocation,
                                             File pluginBundleLocation,
                                             boolean isBundledPlugin) throws IOException, JAXBException, XMLStreamException, SAXException {
        GoPluginDescriptor plugin = deserializeXML(pluginXML, GoPluginDescriptor.class);
        plugin.pluginJarFileLocation(pluginJarFileLocation);
        plugin.bundleLocation(pluginBundleLocation);
        plugin.isBundledPlugin(isBundledPlugin);
        return new GoPluginBundleDescriptor(plugin);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T deserializeXML(InputStream pluginXML, Class<T> klass) throws JAXBException, XMLStreamException, SAXException {
        JAXBContext ctx = JAXBContext.newInstance(klass);
        XMLStreamReader data = XMLInputFactory.newInstance().createXMLStreamReader(pluginXML);
        final Unmarshaller unmarshaller = ctx.createUnmarshaller();
        unmarshaller.setSchema(SchemaFactory.
                newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).
                newSchema(GoPluginDescriptorParser.class.getResource("/plugin-descriptor.xsd")));

        try {
            final JAXBElement<T> result = unmarshaller.unmarshal(data, klass);
            return result.getValue();
        } catch (UnmarshalException e) {
            // there is no non-frustrating way to customize error messages (without other pitfalls anyway),
            // and `UnmarshalException` instances are rarely informative; assume a validation error.
            if (null == e.getMessage()) {
                throw new ValidationException("XML Schema validation of Plugin Descriptor(plugin.xml) failed", e.getCause());
            }
            throw e;
        }
    }
}
