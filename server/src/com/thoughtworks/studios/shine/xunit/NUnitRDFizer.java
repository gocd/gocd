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

package com.thoughtworks.studios.shine.xunit;


import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.XMLRDFizer;
import com.thoughtworks.studios.shine.semweb.grddl.GrddlTransformException;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;

public class NUnitRDFizer implements XMLRDFizer {
    private AntJUnitReportRDFizer jUnitRDFizer;
    private Transformer transformer;

    public NUnitRDFizer(AntJUnitReportRDFizer jUnitRDFizer, XSLTTransformerRegistry xsltTransformerRegistry) {
        this.jUnitRDFizer = jUnitRDFizer;
        this.transformer = xsltTransformerRegistry.getTransformer("xunit/nunit-to-junit.xsl");
    }


    public boolean canHandle(Document doc) {
        String root = doc.getRootElement().getName();
        return "test-results".equals(root);
    }

    public Graph importFile(String parentURI, Document document) throws GrddlTransformException {
        DocumentResult result = new DocumentResult();
        DocumentSource source = new DocumentSource(document);
        try {
            transformer.transform(source, result);
            return jUnitRDFizer.importFile(parentURI, result.getDocument());
        } catch (TransformerException e) {
            throw new ShineRuntimeException(e);
        }
    }
}
