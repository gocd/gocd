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
package com.thoughtworks.go.util;

import com.thoughtworks.go.config.GoConfigSchema;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.registry.NoPluginsInstalled;
import org.apache.commons.io.FileUtils;
import org.jdom2.input.JDOMParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.thoughtworks.go.util.XmlUtils.buildXmlDocument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XmlUtilsTest {

    private ConfigElementImplementationRegistry configElementImplementationRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        configElementImplementationRegistry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
    }

    @Test
    public void shouldThrowExceptionWithTranslatedErrorMessage() {
        String xmlContent = "<foo name='invalid'/>";
        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
        assertThatThrownBy(() -> buildXmlDocument(inputStream, GoConfigSchema.getCurrentSchema(), configElementImplementationRegistry.xsds()))
                .isInstanceOf(XsdValidationException.class);
    }

    @Test
    public void shouldThrowExceptionWhenXmlIsMalformed() {
        String xmlContent = "<foo name='invalid'";
        assertThatThrownBy(() -> buildXmlDocument(xmlContent, GoConfigSchema.getCurrentSchema()))
                .isInstanceOf(JDOMParseException.class)
                .hasMessageContaining("Error on line 1: XML document structures must start and end within the same entity");
    }

    @Test
    public void shouldDisableDocTypeDeclarationsWhenValidatingXmlDocuments() {
        assertThatThrownBy(() -> buildXmlDocument(xxeFileContent(), GoConfigSchema.getCurrentSchema()))
                .isInstanceOf(JDOMParseException.class)
                .hasMessageContaining("DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true");
    }

    @Test
    public void shouldDisableDocTypeDeclarationsWhenValidatingXmlDocumentsWithExternalXsds() {
        assertThatThrownBy(() -> buildXmlDocument(new ByteArrayInputStream(xxeFileContent().getBytes()), GoConfigSchema.getCurrentSchema(), configElementImplementationRegistry.xsds()))
                .isInstanceOf(JDOMParseException.class)
                .hasMessageContaining("DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true");
    }


    private String xxeFileContent() throws IOException {
        return FileUtils.readFileToString(new File(this.getClass().getResource("/data/xml-with-xxe.xml").getFile()), UTF_8);
    }

}
