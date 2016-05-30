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

package com.thoughtworks.studios.shine.semweb.grddl;

import java.io.InputStream;
import java.io.StringReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;

public class GRDDLTransformer {
    private Transformer transformer;
    private final static Logger LOGGER = Logger.getLogger(GRDDLTransformer.class);

    public GRDDLTransformer(Transformer xsltTransformer) {
        this.transformer = xsltTransformer;
    }

    public Graph transform(Document inputDoc, TempGraphFactory graphFactory) throws GrddlTransformException {
        DocumentResult result = new DocumentResult();
        try {
            DocumentSource source = new DocumentSource(inputDoc);
            transformer.transform(source, result);
            // TODO: likely need to optimize with some sort of streaming document reader here
            Graph graph = graphFactory.createTempGraph();
            graph.addTriplesFromRDFXMLAbbrev(new StringReader(result.getDocument().asXML()));
            return graph;
        } catch (ShineRuntimeException e) {
            LOGGER.error("Could not convert to a graph. The document was: \n" + result.getDocument().asXML(), e);
            throw e;
        } catch (TransformerException e) {
            LOGGER.warn("Could not perform grddl transform. The document was: \n" + result.getDocument().asXML(), e);
            throw new GrddlTransformException(e);
        }
    }

    public Graph transform(InputStream xml, TempGraphFactory graphFactory) throws GrddlTransformException {
        try {
            Document inputDoc = new SAXReader().read(xml);
            return transform(inputDoc, graphFactory);
        } catch (DocumentException e) {
            throw new GrddlTransformException(e);
        }
    }

}
