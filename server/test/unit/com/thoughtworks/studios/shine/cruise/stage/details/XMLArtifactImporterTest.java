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
import com.thoughtworks.studios.shine.semweb.*;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.dom4j.Document;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsFalse;
import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mockito.Mockito.*;

public class XMLArtifactImporterTest {
    public static final String URI = "http://example.com/example.owl#";
    public static final String PREFIX = "ex";

    public static final RDFType BAR_CLASS = new RDFType(URI + "Bar");
    public static final RDFProperty FOO_PROPERTY = new RDFProperty(URI + "foo");


    private String STUB_URL = "http://theurl";
    private XMLArtifactImporter artifactImporter;

    @Before
    public void setUp() throws Exception {
        artifactImporter = new XMLArtifactImporter(new SystemEnvironment());
    }

    @Test
    public void testNothingAnArtifactHandlerDoesCanStopTheImporting() throws Exception {
        artifactImporter.registerHandler(new ExplodingHandler());

        artifactImporter.importFile(null, null, new ByteArrayInputStream("<foo />".getBytes()));
        // We didn't explode. Yeah!
    }

    private String SAMPLE_TEST_ARTIFACT=
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
            "\"http://www.host.invalid/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<testsuite errors=\"0\" failures=\"0\" hostname=\"go-agent-1\" name=\"SmokeTest.scn\" tests=\"1\" time=\"243.47\" timestamp=\"2014-07-29T17:04:06\">\n" +
            "  <properties />\n" +
            "  <testcase classname=\"com.thoughtworks.test.XMLJunitOutputListener\" name=\"SmokeTest.scn\" time=\"243.47\" />\n" +
            "</testsuite>\n";

    @Test
    public void testShouldNotTryToDownloadAndVerifyDtdWhenParsingXmlByDefault() throws Exception {
        XMLRDFizer handler = mock(XMLRDFizer.class);
        when(handler.canHandle(any(Document.class))).thenReturn(false);
        artifactImporter.registerHandler(handler);

        artifactImporter.importFile(null, null, toInputStream(SAMPLE_TEST_ARTIFACT));


        //xml successfully read by SAXReader
        verify(handler).canHandle(any(Document.class));
    }

    @Test
    public void testShouldTryToDownloadAndVerifyDtdWhenParsingXmlByDefault() throws Exception {
        try {
            new SystemEnvironment().set(SystemEnvironment.SHOULD_VALIDATE_XML_AGAINST_DTD,true);
            XMLRDFizer handler = mock(XMLRDFizer.class);
            when(handler.canHandle(any(Document.class))).thenReturn(false);
            artifactImporter.registerHandler(handler);

            artifactImporter.importFile(null, null, toInputStream(SAMPLE_TEST_ARTIFACT));


            //xml read failed, could not download http://www.host.invalid/TR/xhtml1/DTD/xhtml1-transitional.dtd of SAMPLE_TEST_ARTIFACT
            verify(handler,never()).canHandle(any(Document.class));
        } finally {
            new SystemEnvironment().set(SystemEnvironment.SHOULD_VALIDATE_XML_AGAINST_DTD,false);
        }
    }

    @Test
    public void testANonXMLArtifactDoesntBlowUpTheUniverse() throws Exception {
        artifactImporter.registerHandler(new StubHandler(true));

        artifactImporter.importFile(null, null, new ByteArrayInputStream("XML is for lusers".getBytes()));
        // We didn't explode. Yeah!
    }

    @Test
    public void testImportedDataWasAdded() throws IOException {
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
