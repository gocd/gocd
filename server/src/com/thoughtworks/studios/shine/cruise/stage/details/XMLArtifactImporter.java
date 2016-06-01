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

package com.thoughtworks.studios.shine.cruise.stage.details;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.URIReference;
import com.thoughtworks.studios.shine.semweb.XMLRDFizer;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

public class XMLArtifactImporter {

    private final static Logger LOGGER = Logger.getLogger(XMLArtifactImporter.class);

    SystemEnvironment systemEnvironment;

    private List<XMLRDFizer> handlers = new LinkedList<>();

    public XMLArtifactImporter(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public void registerHandler(XMLRDFizer handler) {
        handlers.add(handler);
    }

    public void importFile(Graph graph, URIReference parentJob, InputStream in) {
        try {
            SAXReader saxReader = new SAXReader();
            if (systemEnvironment.get(SystemEnvironment.SHOULD_VALIDATE_XML_AGAINST_DTD)) {
                saxReader.setValidation(true);
            } else {
                saxReader.setEntityResolver(getCustomEntityResolver());
            }
            Document doc = saxReader.read(in);
            importXML(graph, parentJob, doc);
        } catch (DocumentException e) {
            LOGGER.warn("Invalid xml provided.", e);
        }
    }

    private void importXML(Graph graph, URIReference parentJob, Document doc) {
        for (XMLRDFizer handler : handlers) {
            if (handler.canHandle(doc)) {
                try {
                    Graph artifactGraph = handler.importFile(parentJob.getURIText(), doc);
                    LOGGER.debug("Imported a total of " + artifactGraph.size() + " triples in artifact.");
                    graph.addTriplesFromGraph(artifactGraph);
                    return;
                } catch (Exception e) {
                    LOGGER.warn(handler.getClass().getName() + " was unable to handle document it said it could.  Exception happened while importing artifacts for job: " + parentJob + "...", e);
                }
            }
        }
    }

    private EntityResolver getCustomEntityResolver() {

        return new EntityResolver() {
            @Override
            public InputSource resolveEntity(String s, String s2) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        };
    }
}
