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

package com.thoughtworks.studios.shine.cruise;

import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import com.thoughtworks.studios.shine.semweb.XMLRDFizer;
import com.thoughtworks.studios.shine.semweb.grddl.GRDDLTransformer;
import com.thoughtworks.studios.shine.semweb.grddl.GrddlTransformException;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import java.io.IOException;

public class GoGRDDLResourceRDFizer implements XMLRDFizer {
    private final GRDDLTransformer grddlTransformer;
    private final String rootNodeName;
    private TempGraphFactory graphFactory;
    private XmlApiService xmlApiService;

    public GoGRDDLResourceRDFizer(String rootNodeName, String grddlResourcePath, TempGraphFactory graphFactory, XSLTTransformerRegistry transformerRegistry, XmlApiService xmlApiService) {
        this.rootNodeName = rootNodeName;
        this.graphFactory = graphFactory;
        this.xmlApiService = xmlApiService;
        this.grddlTransformer = new GRDDLTransformer(transformerRegistry, grddlResourcePath);
    }

    public boolean canHandle(Document doc) {
        return rootNodeName.equals(doc.getRootElement().getName());
    }

    public Graph importFile(String parentURI, Document doc) {
        try {
            return grddlTransformer.transform(doc, graphFactory);
        } catch (GrddlTransformException e) {
            throw new ShineRuntimeException(e);
        }
    }

    public Graph importURIUsingGRDDL(final XmlRepresentable xmlRepresentable, final String baseUri) throws GoIntegrationException {
        try {
            Document doc = xmlApiService.write(xmlRepresentable, baseUri);
            return importDocumentUsingGRDDL(doc);
        } catch (DocumentException | IOException e) {
            throw new GoIntegrationException(e);
        }
    }

    public Graph importDocumentUsingGRDDL(Document document) throws GoIntegrationException {
        return importFile(null, document);
    }
}
