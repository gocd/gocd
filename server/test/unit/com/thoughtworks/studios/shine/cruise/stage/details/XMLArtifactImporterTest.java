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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.RDFProperty;
import com.thoughtworks.studios.shine.semweb.RDFType;
import com.thoughtworks.studios.shine.semweb.URIReference;
import com.thoughtworks.studios.shine.semweb.XMLRDFizer;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.dom4j.Document;
import org.junit.Test;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsFalse;
import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;

public class XMLArtifactImporterTest {
    public static final String URI = "http://example.com/example.owl#";
    public static final String PREFIX = "ex";

    public static final RDFType BAR_CLASS = new RDFType(URI + "Bar");
    public static final RDFProperty FOO_PROPERTY = new RDFProperty(URI + "foo");


    private String STUB_URL = "http://theurl";

    @Test
    public void testNothingAnArtifactHandlerDoesCanStopTheImporting() throws Exception {
        XMLArtifactImporter artifactImporter = new XMLArtifactImporter();
        artifactImporter.registerHandler(new ExplodingHandler());

        artifactImporter.importFile(null, null, new ByteArrayInputStream("<foo />".getBytes()));
        // We didn't explode. Yeah!
    }

    @Test
    public void testANonXMLArtifactDoesntBlowUpTheUniverse() throws Exception {
        XMLArtifactImporter artifactImporter = new XMLArtifactImporter();
        artifactImporter.registerHandler(new StubHandler(true));

        artifactImporter.importFile(null, null, new ByteArrayInputStream("XML is for lusers".getBytes()));
        // We didn't explode. Yeah!
    }

    @Test
    public void testImportedDataWasAdded() throws IOException {
        XMLArtifactImporter artifactImporter = new XMLArtifactImporter();
        artifactImporter.registerHandler(new StubHandler(true));

        Graph graph = new InMemoryTempGraphFactory().createTempGraph();
        URIReference parent = graph.createURIReference(BAR_CLASS, "http://parent/");

        artifactImporter.importFile(graph, parent, new ByteArrayInputStream("<foo />".getBytes()));

        String ask =
                "PREFIX ex: <http://example.com/example.owl#> " +

                        "ASK WHERE { " +
                        "<http://parent/> ex:foo <http://yadda/> " +
                        "}";

        assertAskIsTrue(graph, (ask));
    }

    @Test
    public void testSubsequentHandlerIsUsedWhenFirstCannotHandleArtifact() throws Exception {
        XMLArtifactImporter artifactImporter = new XMLArtifactImporter();
        artifactImporter.registerHandler(new StubHandler(false, "http://foobar/"));
        artifactImporter.registerHandler(new StubHandler(true, "http://A-OK/"));

        Graph graph = new InMemoryTempGraphFactory().createTempGraph();
        URIReference parent = graph.createURIReference(BAR_CLASS, "http://parent/");

        artifactImporter.importFile(graph, parent, new ByteArrayInputStream("<foo />".getBytes()));

        String askForFirstHandler =
                "PREFIX ex: <http://example.com/example.owl#> " +

                        "ASK WHERE { " +
                        "<http://parent/> ex:foo <http://foobar/> " +
                        "}";

        assertAskIsFalse(graph, askForFirstHandler);
        String askForSecondHandler =
                "PREFIX ex: <http://example.com/example.owl#> " +

                        "ASK WHERE { " +
                        "<http://parent/> ex:foo <http://A-OK/> " +
                        "}";

        assertAskIsTrue(graph, askForSecondHandler);

    }

    @Test
    public void testSubsequentHandlerIsUsedWhenFirstHandlerBlowsUp () throws Exception {
        XMLArtifactImporter artifactImporter = new XMLArtifactImporter();
        artifactImporter.registerHandler(new ExplodingHandler());
        artifactImporter.registerHandler(new StubHandler(true, "http://A-OK/"));

        Graph graph = new InMemoryTempGraphFactory().createTempGraph();
        URIReference parent = graph.createURIReference(BAR_CLASS, "http://parent/");

        artifactImporter.importFile(graph, parent, new ByteArrayInputStream("<foo />".getBytes()));

        String askForSecondHandler =
                "PREFIX ex: <http://example.com/example.owl#> " +

                        "ASK WHERE { " +
                        "<http://parent/> ex:foo <http://A-OK/> " +
                        "}";

        assertAskIsTrue(graph, askForSecondHandler);

    }

    class StubHandler implements XMLRDFizer {
        private boolean canHandle;
        private String stubResourceURI;

        public StubHandler(boolean canHandle) {
            this(canHandle, "http://yadda/");
        }

        public StubHandler(boolean canHandle, String stubResourceURI) {
            this.canHandle = canHandle;
            this.stubResourceURI = stubResourceURI;
        }

        public boolean canHandle(Document doc) {
            return canHandle;
        }

        public Graph importFile(String parent, Document doc) {
            Graph graph = new InMemoryTempGraphFactory().createTempGraph();
            URIReference newResource = graph.createURIReference(BAR_CLASS, stubResourceURI);
            URIReference parentResource = graph.getURIReference(parent);
            graph.addStatement(parentResource, FOO_PROPERTY, newResource);

            return graph;
        }
    }

    class ExplodingHandler implements XMLRDFizer {
        public boolean canHandle(Document doc) {
            return true;
        }

        public Graph importFile(String parent, Document doc) {
            throw new RuntimeException();
        }
    }
}
