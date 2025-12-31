/*
 * Copyright Thoughtworks, Inc.
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
import jakarta.xml.bind.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

/*
    <?xml version="1.0" encoding="utf-8" ?>
    <go-plugin id="testplugin.descriptorValidator" version="1">
        <about>
            <name>Plugin Descriptor Validator</name>
            <version>1.0.1</version>
            <target-go-version>17.12</target-go-version>
            <description>Validates its own plugin descriptor</description>
            <vendor>
                <name>GoCD Team</name>
                <url>https://gocd.org</url>
            </vendor>
            <target-os>
              <value>Linux</value>
              <value>Windows</value>
            </target-os>
        </about>
    </go-plugin>

Also Parses an XML of this kind (see below). Also see gocd-bundle-descriptor.xsd.

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
public final class GoPluginDescriptorParser {

    private static final ThreadLocal<Map<Class<?>, JAXBContext>> CONTEXTS = ThreadLocal.withInitial(() -> Map.of(
        GoPluginDescriptor.class, getJaxbContext(GoPluginDescriptor.class),
        GoPluginBundleDescriptor.class, getJaxbContext(GoPluginBundleDescriptor.class)
    ));

    private static final ThreadLocal<Map<Class<?>, Schema>> SCHEMAS = ThreadLocal.withInitial(() -> Map.of(
        GoPluginDescriptor.class, schemaFor(GoPluginDescriptor.class, "/plugin-descriptor.xsd"),
        GoPluginBundleDescriptor.class, schemaFor(GoPluginBundleDescriptor.class, "/gocd-bundle-descriptor.xsd")
    ));

    private GoPluginDescriptorParser() {}

    public static GoPluginBundleDescriptor parseXml(InputStream pluginXml,
                                                    BundleOrPluginFileDetails bundleOrPluginJarFile) throws JAXBException, XMLStreamException {
        return parseXml(pluginXml, bundleOrPluginJarFile.file().getAbsolutePath(), bundleOrPluginJarFile.extractionLocation(), bundleOrPluginJarFile.isBundledPlugin());
    }

    static GoPluginBundleDescriptor parseXml(InputStream pluginXML,
                                             String pluginJarFileLocation,
                                             File pluginBundleLocation,
                                             boolean isBundledPlugin) throws JAXBException, XMLStreamException {
        GoPluginDescriptor plugin = deserializeXML(pluginXML, GoPluginDescriptor.class, "plugin.xml");
        plugin.pluginJarFileLocation(pluginJarFileLocation);
        plugin.bundleLocation(pluginBundleLocation);
        plugin.isBundledPlugin(isBundledPlugin);
        return new GoPluginBundleDescriptor(plugin);
    }

    public static GoPluginBundleDescriptor parseBundleXml(InputStream pluginXml,
                                                          BundleOrPluginFileDetails bundleOrPluginJarFile) throws JAXBException, XMLStreamException {
        return parseBundleXml(pluginXml, bundleOrPluginJarFile.file().getAbsolutePath(), bundleOrPluginJarFile.extractionLocation(), bundleOrPluginJarFile.isBundledPlugin());
    }

    static GoPluginBundleDescriptor parseBundleXml(InputStream pluginXML,
                                                   String pluginJarFileLocation,
                                                   File pluginBundleLocation,
                                                   boolean isBundledPlugin) throws JAXBException, XMLStreamException {

        GoPluginBundleDescriptor bundle = deserializeXML(pluginXML, GoPluginBundleDescriptor.class, "bundle.xml");
        bundle.pluginDescriptors().forEach(d -> {
            d.setBundleDescriptor(bundle);
            d.version(bundle.version());
            d.pluginJarFileLocation(pluginJarFileLocation);
            d.bundleLocation(pluginBundleLocation);
            d.isBundledPlugin(isBundledPlugin);
        });
        return bundle;
    }

    private static <T> T deserializeXML(InputStream pluginXML, Class<T> klass, String resourceType) throws JAXBException, XMLStreamException {
        XMLStreamReader data = streamReaderFor(pluginXML);
        final Unmarshaller unmarshaller = CONTEXTS.get().get(klass).createUnmarshaller();
        unmarshaller.setSchema(SCHEMAS.get().get(klass));

        try {
            return unmarshaller.unmarshal(data, klass).getValue();
        } catch (UnmarshalException e) {
            // there is no non-frustrating way to customize error messages (without other pitfalls anyway),
            // and `UnmarshalException` instances are rarely informative; assume a validation error.
            if (e.getMessage() == null) {
                throw new ValidationException("XML Schema validation of Plugin Descriptor(" + resourceType + ") failed", e.getCause());
            }
            throw e;
        }
    }

    private static XMLStreamReader streamReaderFor(InputStream pluginXML) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return factory.createXMLStreamReader(pluginXML);
    }

    // Returns a JAXBContext - these are thread-safe and can be re-used
    private static <T> JAXBContext getJaxbContext(Class<T> klass) {
        try {
            return JAXBContext.newInstance(klass);
        } catch (JAXBException e) {
            throw new DataBindingException(e);
        }
    }

    // Returns a new schema. Note that schemas are thread-safe and can be reused.
    private static Schema schemaFor(Class<?> klass, String schemaResourcePath) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return schemaFactory.newSchema(klass.getResource(schemaResourcePath));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
