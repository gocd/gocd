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
package com.thoughtworks.go.config.parser;

import com.thoughtworks.go.config.AttributeAwareConfigTag;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.preprocessor.ClassAttributeCache;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.security.GoCipher;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GoConfigClassLoaderTest {
    @Mock
    private ConfigCache configCache;
    @Mock
    private GoCipher goCipher;
    @Mock
    private ConfigElementImplementationRegistry registry;
    @Mock
    private ConfigReferenceElements referenceElements;

    @Test
    public void shouldErrorOutIfElementDoesNotHaveConfigTagAnnotation() {
        final Element element = new Element("cruise");
        final GoConfigClassLoader<ConfigWithoutAnnotation> loader = GoConfigClassLoader.classParser(element, ConfigWithoutAnnotation.class, configCache, goCipher, registry, referenceElements);

        assertThatThrownBy(loader::parse)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to parse element <cruise> for class ConfigWithoutAnnotation");
    }

    @Test
    public void shouldContinueParsingWhenElementHasConfigTagAnnotation() {
        final Element element = new Element("example");
        when(configCache.getFieldCache()).thenReturn(new ClassAttributeCache.FieldCache());

        final GoConfigClassLoader<ConfigWithConfigTagAnnotation> loader = GoConfigClassLoader.classParser(element, ConfigWithConfigTagAnnotation.class, configCache, goCipher, registry, referenceElements);

        final ConfigWithConfigTagAnnotation configWithConfigTagAnnotation = loader.parse();

        assertNotNull(configWithConfigTagAnnotation);
    }

    @Test
    public void shouldContinueParsingWhenConfigClassHasValidAttributeAwareConfigTagAnnotation() {
        final Element element = new Element("example");
        element.setAttribute("type", "example-type");
        when(configCache.getFieldCache()).thenReturn(new ClassAttributeCache.FieldCache());

        final GoConfigClassLoader<ConfigWithAttributeAwareConfigTagAnnotation> loader = GoConfigClassLoader.classParser(element, ConfigWithAttributeAwareConfigTagAnnotation.class, configCache, goCipher, registry, referenceElements);

        final ConfigWithAttributeAwareConfigTagAnnotation configWithConfigTagAnnotation = loader.parse();

        assertNotNull(configWithConfigTagAnnotation);
    }

    @Test
    public void shouldErrorOutWhenConfigClassHasAttributeAwareConfigTagAnnotationButAttributeValueIsNotMatching() {
        final Element element = new Element("example");
        element.setAttribute("type", "foo-bar");

        final GoConfigClassLoader<ConfigWithAttributeAwareConfigTagAnnotation> loader = GoConfigClassLoader.classParser(element, ConfigWithAttributeAwareConfigTagAnnotation.class, configCache, goCipher, registry, referenceElements);

        assertThatThrownBy(loader::parse)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to determine type to generate. Type: com.thoughtworks.go.config.parser.ConfigWithAttributeAwareConfigTagAnnotation Element: \n" +
                        "\t<example type=\"foo-bar\" />");
    }

    @Test
    public void shouldErrorOutWhenConfigClassHasAttributeAwareConfigTagAnnotationButAttributeIsNotPresent() {
        final Element element = new Element("example");

        final GoConfigClassLoader<ConfigWithAttributeAwareConfigTagAnnotation> loader = GoConfigClassLoader.classParser(element, ConfigWithAttributeAwareConfigTagAnnotation.class, configCache, goCipher, registry, referenceElements);

        assertThatThrownBy(loader::parse)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expected attribute `type` to be present for \n\t<example />");
    }

    @Test
    public void shouldErrorOutWhenConfigClassHasAttributeAwareConfigTagAnnotationButAttributeIsMissingInConfig() {
        final Element element = new Element("example");

        final GoConfigClassLoader<AttributeAwareConfigTagWithBlankAttributeValue> loader = GoConfigClassLoader.classParser(element, AttributeAwareConfigTagWithBlankAttributeValue.class, configCache, goCipher, registry, referenceElements);

        assertThatThrownBy(loader::parse)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Type 'com.thoughtworks.go.config.parser.AttributeAwareConfigTagWithBlankAttributeValue' has invalid configuration for @AttributeAwareConfigTag. It must have `attribute` with non blank value.");
    }

    @Test
    public void shouldErrorOutWhenAttributeAwareConfigTagHasAttributeWithBlankValue() {
        final Element element = new Element("example");

        final GoConfigClassLoader<ConfigWithAttributeAwareConfigTagAnnotation> loader = GoConfigClassLoader.classParser(element, ConfigWithAttributeAwareConfigTagAnnotation.class, configCache, goCipher, registry, referenceElements);

        assertThatThrownBy(loader::parse)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expected attribute `type` to be present for \n\t<example />.");
    }

    @Test
    public void shouldErrorOutWhenAttributeAwareConfigTagClassHasConfigAttributeWithSameName() {
        final Element element = new Element("example");
        element.setAttribute("type", "example-type");
        when(configCache.getFieldCache()).thenReturn(new ClassAttributeCache.FieldCache());

        final GoConfigClassLoader<AttributeAwareConfigTagHasConfigAttributeWithSameName> loader = GoConfigClassLoader.classParser(element, AttributeAwareConfigTagHasConfigAttributeWithSameName.class, configCache, goCipher, registry, referenceElements);

        assertThatThrownBy(loader::parse)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Attribute `type` is not allowed in com.thoughtworks.go.config.parser.AttributeAwareConfigTagHasConfigAttributeWithSameName. You cannot use @ConfigAttribute  annotation with attribute name `type` when @AttributeAwareConfigTag is configured with same name.");
    }
}

class ConfigWithoutAnnotation {

}

@ConfigTag("example")
class ConfigWithConfigTagAnnotation {
}

@AttributeAwareConfigTag(value = "example", attribute = "type", attributeValue = "example-type")
class ConfigWithAttributeAwareConfigTagAnnotation {
}

@AttributeAwareConfigTag(value = "example", attribute = "", attributeValue = "example-type")
class AttributeAwareConfigTagWithBlankAttributeValue {
}

@AttributeAwareConfigTag(value = "example", attribute = "type", attributeValue = "example-type")
class AttributeAwareConfigTagHasConfigAttributeWithSameName {
    @ConfigAttribute("type")
    private String type;
}