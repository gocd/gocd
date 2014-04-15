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

package com.thoughtworks.studios.shine.cruise;

import java.io.ByteArrayInputStream;

import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GoGRDDLResourceRDFizerTest {
    private XSLTTransformerRegistry XSLTTransformerRegistry;

    @Before
    public void setup() {
        XSLTTransformerRegistry = new XSLTTransformerRegistry();
    }

    @Test
    public void testCanHandleAcceptsCorrectRootElement() throws DocumentException {
        GoGRDDLResourceRDFizer rdfizer = new GoGRDDLResourceRDFizer("job", "cruise/job-grddl.xsl", new InMemoryTempGraphFactory(), XSLTTransformerRegistry, null);

        assertTrue(rdfizer.canHandle(doc("<job />")));
        assertFalse(rdfizer.canHandle(doc("<stage />")));
    }

    private Document doc(String xmlDocumentAsString) throws DocumentException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlDocumentAsString.getBytes());
        return new SAXReader().read(inputStream);
    }
}
