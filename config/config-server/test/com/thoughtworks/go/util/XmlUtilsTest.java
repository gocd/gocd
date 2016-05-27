/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import com.thoughtworks.go.config.GoConfigSchema;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.registry.NoPluginsInstalled;
import org.jdom.input.JDOMParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.thoughtworks.go.util.XmlUtils.buildXmlDocument;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class XmlUtilsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private ConfigElementImplementationRegistry configElementImplementationRegistry;

    @Before
    public void setUp() throws Exception {
        configElementImplementationRegistry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
    }

    @Test
    public void shouldThrowExceptionWithTranslatedErrorMessage() throws Exception {
        String xmlContent = "<foo name='invalid'/>";
        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
        try {
            buildXmlDocument(inputStream, GoConfigSchema.getCurrentSchema(), configElementImplementationRegistry.xsds());
            fail("Should throw a XsdValidationException");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(XsdValidationException.class)));
        }
    }

    @Test
    public void shouldThrowExceptionWhenXmlIsMalformed() throws Exception {
        expectedException.expect(JDOMParseException.class);
        expectedException.expectMessage(containsString("Error on line 1: XML document structures must start and end within the same entity"));

        String xmlContent = "<foo name='invalid'";
        buildXmlDocument(xmlContent, GoConfigSchema.getCurrentSchema());
    }

    @Test
    public void shouldDisableDocTypeDeclarationsWhenValidatingXmlDocuments() throws Exception {
        expectDOCTYPEDisallowedException();
        buildXmlDocument(xxeFileContent(), GoConfigSchema.getCurrentSchema());
    }

    @Test
    public void shouldDisableDocTypeDeclarationsWhenValidatingXmlDocumentsWithExternalXsds() throws Exception {
        expectDOCTYPEDisallowedException();
        buildXmlDocument(new ByteArrayInputStream(xxeFileContent().getBytes()), GoConfigSchema.getCurrentSchema(), configElementImplementationRegistry.xsds());
    }

    private void expectDOCTYPEDisallowedException() {
        expectedException.expect(JDOMParseException.class);
        expectedException.expectMessage(containsString("DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true"));
    }

    private String xxeFileContent() throws IOException {
        return FileUtil.readContentFromFile(new File(this.getClass().getResource("/data/xml-with-xxe.xml").getFile()));
    }

}
