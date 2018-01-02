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

package com.thoughtworks.studios.shine.semweb;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

public interface Graph extends TempGraphFactory {

    void addNamespace(Namespace namespace);

    Namespace getNamespaceByPrefix(String prefix);

    void addStatement(Resource subject, RDFProperty predicate, Resource object);

    void addStatement(Resource subject, RDFProperty predicate, Integer object);

    void addStatement(Resource subject, RDFProperty predicate, String object);

    void addStatement(Resource subject, RDFProperty predicate, Boolean object);

    void addTriplesFromGraph(Graph graph);

    void addTriplesFromTurtle(String rdf);

    void addTriplesFromRDFXMLAbbrev(Reader reader);

    Boolean ask(String sparql);

    void clearAllTriples();

    void close();

    boolean containsResourceWithURI(String URI);

    URIReference createFakeBlankNode(RDFType type);

    URIReference createURIReference(RDFType type, String uri);

    URIReference getURIReference(String uri);

    void renderSPARQLResultsAsXML(String sparql, OutputStream stream);

    void remove(Resource tripleSubject, RDFProperty triplePredicate, Resource tripleObject);

    void remove(Resource tripleSubject, RDFProperty triplePredicate, Integer integer);

    void remove(Resource tripleSubject, RDFProperty triplePredicate, String tripleObject);

    void removeTypeOn(Resource tripleSubject, RDFType tripleObject);

    List<BoundVariables> select(String sparql);

    BoundVariables selectFirst(String sparql);

    long size();

    void dump(Writer writer);

    void validate(String sparql);

    void persistToTurtle(OutputStream outputStream);

    void addTriplesFromTurtle(InputStream stream);
}
