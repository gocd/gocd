/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.studios.shine.xunit;


import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.XSLTTransformerExecutor;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.XMLRDFizer;
import com.thoughtworks.studios.shine.semweb.grddl.GrddlTransformException;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

public class NUnitRDFizer implements XMLRDFizer {
    private AntJUnitReportRDFizer jUnitRDFizer;
    private final XSLTTransformerRegistry xsltTransformerRegistry;

    public NUnitRDFizer(AntJUnitReportRDFizer jUnitRDFizer, XSLTTransformerRegistry xsltTransformerRegistry) {
        this.jUnitRDFizer = jUnitRDFizer;
        this.xsltTransformerRegistry = xsltTransformerRegistry;
    }


    public boolean canHandle(Document doc) {
        String root = doc.getRootElement().getName();
        return "test-results".equals(root);
    }

    public Graph importFile(final String parentURI, Document document) throws GrddlTransformException {
        final DocumentResult result = new DocumentResult();
        final DocumentSource source = new DocumentSource(document);
        try {
            return xsltTransformerRegistry.transformWithCorrectClassLoader(XSLTTransformerRegistry.XUNIT_NUNIT_TO_JUNIT_XSL, new XSLTTransformerExecutor<Graph>() {
                @Override
                public Graph execute(Transformer transformer) throws TransformerException, GrddlTransformException {
                    transformer.transform(source, result);
                    return jUnitRDFizer.importFile(parentURI, result.getDocument());
                }
            });
        } catch (TransformerException e) {
            throw new ShineRuntimeException(e);
        }
    }
}
