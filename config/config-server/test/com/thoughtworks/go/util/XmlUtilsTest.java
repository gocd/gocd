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

package com.thoughtworks.go.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.thoughtworks.go.config.GoConfigSchema;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.registry.NoPluginsInstalled;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class XmlUtilsTest {

    @Test
    public void shouldThrowExceptionWithTranslatedErrorMessage() throws Exception {
        String xmlContent = "<foo name='invalid'/>";
        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
        try {
            final ConfigElementImplementationRegistry configElementImplementationRegistry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
            XmlUtils.validate(inputStream, GoConfigSchema.getCurrentSchema(), new XsdErrorTranslator(), new SAXBuilder(), configElementImplementationRegistry.xsds());
            fail("Should throw a XsdValidationException");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(XsdValidationException.class)));
        }
    }

    @Test
    public void shouldThrowExceptionWhenXmlIsMalformed() throws Exception {
        String xmlContent = "<foo name='invalid'";
        try {
            XmlUtils.validate(xmlContent, GoConfigSchema.getCurrentSchema(), new XsdErrorTranslator(), new SAXBuilder());
            fail("Should throw a XsdValidationException");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(XsdValidationException.class)));
        }
    }

    @Test
    public void shouldBuildXmlDocumentOutOfString() throws Exception {
        Document document =  XmlUtils.buildXmlDocument("<parent><child>one</child><child>one</child></parent>");
        assertThat(document.getRootElement().getName(),is("parent"));
        assertThat(document.getRootElement().getChildren().size(),is(2));
    }


}