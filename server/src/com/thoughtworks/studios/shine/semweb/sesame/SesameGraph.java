/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.studios.shine.semweb.sesame;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.MalformedSPARQLException;
import com.thoughtworks.studios.shine.semweb.MoreThanOneResultFoundException;
import com.thoughtworks.studios.shine.semweb.Namespace;
import com.thoughtworks.studios.shine.semweb.RDFOntology;
import com.thoughtworks.studios.shine.semweb.RDFProperty;
import com.thoughtworks.studios.shine.semweb.RDFType;
import com.thoughtworks.studios.shine.semweb.Resource;
import com.thoughtworks.studios.shine.semweb.URIReference;
import com.thoughtworks.studios.shine.semweb.UUIDURIGenerator;
import com.thoughtworks.studios.shine.semweb.UnsupportedSPARQLStatementException;
import com.thoughtworks.studios.shine.util.ArgumentUtil;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.openrdf.query.resultio.BooleanQueryResultWriter;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailQuery;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

public class SesameGraph implements Graph {
    private final RepositoryConnection conn;
    private org.openrdf.model.Resource[] contextResource;
    private Var contextVar;
    private List<Graph> tempGraphs = new ArrayList<>();
    private final static Logger LOGGER = Logger.getLogger(SesameGraph.class);

    private Map<RDFProperty, URI> sesameNativeTypeByRDFProperty = new HashMap<>();

    public SesameGraph(RepositoryConnection conn) {
        this(conn, null);
    }

    public SesameGraph(RepositoryConnection conn, String contextURI) {
        this.conn = conn;
        if (contextURI != null) {
            org.openrdf.model.Resource contextResource = ((SesameURIReference) getURIReference(contextURI)).getSesameNativeResource();
            this.contextResource = new org.openrdf.model.Resource[]{contextResource};
            contextVar = new Var("magic-context", conn.getValueFactory().createURI(contextResource.stringValue()));
            contextVar.setAnonymous(true);
        } else {
            this.contextResource = new org.openrdf.model.Resource[0];
        }
    }


    public void addNamespace(Namespace namespace) {
        try {
            conn.setNamespace(namespace.getPrefix(), namespace.getURIText());
        } catch (Exception e) {
            throw new ShineRuntimeException("Could not add namespace!" + namespace, e);
        }
    }


    public Namespace getNamespaceByPrefix(String prefix) {
        try {
            return new Namespace(prefix, conn.getNamespace(prefix));
        } catch (Exception e) {
            throw new ShineRuntimeException("Could not get namespace by prefix" + prefix, e);
        }
    }


    public void addStatement(Resource subject, RDFProperty predicate, Resource object) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) subject;
        URI sesameNativePredicate = getSesameNativeProperty(predicate);
        AbstractSesameResource sesameObject = (AbstractSesameResource) object;

        try {
            conn.add(sesameSubject.getSesameNativeResource(), sesameNativePredicate, sesameObject.getSesameNativeResource(), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException("Could not add statement << [" + subject + "] [" + predicate + "] [" + object + "] >>", e);
        }
    }


    public void addStatement(Resource subject, RDFProperty predicate, Integer object) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) subject;
        URI sesameNativePredicate = getSesameNativeProperty(predicate);

        try {
            conn.add(sesameSubject.getSesameNativeResource(),
                    sesameNativePredicate,
                    conn.getValueFactory().createLiteral(String.valueOf(object), XMLSchema.INTEGER), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException("Could not add statement << [" + subject + "] [" + predicate + "] [" + object + "] >>", e);
        }
    }


    public void addStatement(Resource subject, RDFProperty predicate, String object) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) subject;
        URI sesameNativePredicate = getSesameNativeProperty(predicate);

        try {
            conn.add(sesameSubject.getSesameNativeResource(),
                    sesameNativePredicate,
                    conn.getValueFactory().createLiteral(String.valueOf(object), XMLSchema.STRING), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException("Could not add statement << [" + subject + "] [" + predicate + "] [" + object + "] >>", e);
        }
    }


    public void addStatement(Resource subject, RDFProperty predicate, Boolean object) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) subject;
        URI sesameNativePredicate = getSesameNativeProperty(predicate);

        try {
            conn.add(sesameSubject.getSesameNativeResource(),
                    sesameNativePredicate,
                    conn.getValueFactory().createLiteral(String.valueOf(object), XMLSchema.BOOLEAN), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException("Could not add statement << [" + subject + "] [" + predicate + "] [" + object + "] >>", e);
        }
    }

    URI getSesameNativeProperty(RDFProperty predicate) {
        if (!sesameNativeTypeByRDFProperty.containsKey(predicate)) {
            String predicateURIText = predicate.getURIText();
            URI predicateURI = conn.getValueFactory().createURI(predicateURIText);
            sesameNativeTypeByRDFProperty.put(predicate, predicateURI);
        }

        return sesameNativeTypeByRDFProperty.get(predicate);
    }


    public void addTriplesFromGraph(Graph graph) {
        SesameGraph sesameNativeGraph = (SesameGraph) graph;
        try {
            conn.add(sesameNativeGraph.conn.getStatements(null, null, null, false), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void addTriplesFromTurtle(String rdf) {
        try {
            conn.add(new StringReader(rdf), "", RDFFormat.TURTLE, contextResource);
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void addTriplesFromTurtle(InputStream stream) {
        try {
            conn.add(stream, "", RDFFormat.TURTLE, contextResource);
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        } finally {
            closeStream(stream);
        }
    }

    private void closeStream(Closeable closable) {
        try {
            closable.close();
        } catch (IOException e) {
            throw new ShineRuntimeException(e);
        }
    }


    public Boolean ask(String sparqlAsk) {
        return getBooleanQueryResult(sparqlAsk);
    }


    public void clearAllTriples() {
        try {
            conn.clear();
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void close() {
        try {
            for (Graph tempGraph : tempGraphs) {
                tempGraph.close();
            }
            tempGraphs.clear();

            conn.commit();
            conn.close();

        } catch (RepositoryException e) {
            throw new ShineRuntimeException("Could not close graph!", e);
        }
    }


    public URIReference createFakeBlankNode(RDFType type) {
        return createURIReference(type, UUIDURIGenerator.nextType4());
    }


    public Graph createTempGraph() {
        try {
            if (!conn.isOpen()) {
                throw new IllegalStateException("Cannot create a temp graph on a closed graph!");
            }
            Repository inMemRepos = InMemoryRepositoryFactory.emptyRepository();

            String contextURI = null;
            if (contextResource.length > 0) {
                contextURI = contextResource[0].stringValue();
            }
            Graph tempGraph = new SesameGraph(inMemRepos.getConnection(), contextURI);
            tempGraphs.add(tempGraph);
            return tempGraph;
        } catch (RepositoryException ex) {
            throw new ShineRuntimeException("Unable to create temp graph!", ex);
        }
    }


    public void addTriplesFromRDFXMLAbbrev(Reader reader) {
        try {
            conn.add(reader, "", RDFFormat.RDFXML, contextResource);
        } catch (Exception e) {
            throw new ShineRuntimeException("Could not create graph from XML RDF!", e);
        } finally {
            closeStream(reader);
        }
    }


    public URIReference createURIReference(RDFType type, String uri) {
        ArgumentUtil.guaranteeNotNull(type, "Type may not be null.");
        ArgumentUtil.guaranteeNotNull(uri, "URI may not be null.");
        ArgumentUtil.guaranteeFalse("URI may not be a blank node!", uri.startsWith("_:"));

        URI sesameNativeURI = conn.getValueFactory().createURI(uri);

        try {
            conn.add(sesameNativeURI, RDF.TYPE, conn.getValueFactory().createURI(type.getURIText()), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        }

        return new SesameURIReference(sesameNativeURI);
    }


    public URIReference getURIReference(String uri) {
        // TODO: JS - I'm not sure that this is a good idea. Is there a way to do this without "creating" the uri?
        return new SesameURIReference(conn.getValueFactory().createURI(uri));
    }

    org.openrdf.model.Value getPropertyValue(org.openrdf.model.Resource resource, RDFProperty rdfProperty) {
        try {
            return conn.getStatements(resource, getSesameNativeProperty(rdfProperty), null, false).next().getObject();
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        } catch (NoSuchElementException e) {
            return null;
        }
    }


    public void renderSPARQLResultsAsXML(String sparql, OutputStream stream) {
        try {
            Query query = conn.prepareQuery(QueryLanguage.SPARQL, sparql);
            contextualize(query);
            if (query instanceof TupleQuery) {
                renderTupleQuery(query, new SPARQLResultsXMLWriter(stream));
            } else {
                renderBooleanQuery(query, new SPARQLBooleanXMLWriter(stream));
            }
            stream.flush();
        } catch (UnsupportedSPARQLStatementException e) {
            throw e;
        } catch (Exception e) {
            throw new ShineRuntimeException("Could not render sparql results as XML: <<" + sparql + ">>", e);
        }
    }

    private void renderBooleanQuery(Query query, BooleanQueryResultWriter writer) throws IOException, QueryEvaluationException {
        writer.write(((BooleanQuery) query).evaluate());
    }

    private void renderTupleQuery(Query query, TupleQueryResultWriter writer) throws QueryEvaluationException, TupleQueryResultHandlerException {
        TupleQueryResult tupleQueryResult = ((TupleQuery) query).evaluate();
        writer.startQueryResult(tupleQueryResult.getBindingNames());
        while (tupleQueryResult.hasNext()) {
            writer.handleSolution(tupleQueryResult.next());
        }
        writer.endQueryResult();
    }


    public void remove(Resource tripleSubject, RDFProperty triplePredicate, Resource tripleObject) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) tripleSubject;
        URI sesameNativePredicate = getSesameNativeProperty(triplePredicate);
        AbstractSesameResource sesameObject = (AbstractSesameResource) tripleObject;

        try {
            conn.remove(sesameSubject.getSesameNativeResource(),
                    sesameNativePredicate,
                    sesameObject.getSesameNativeResource(), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void remove(Resource tripleSubject, RDFProperty triplePredicate, Integer integer) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) tripleSubject;
        URI sesameNativePredicate = getSesameNativeProperty(triplePredicate);

        try {
            conn.remove(sesameSubject.getSesameNativeResource(),
                    sesameNativePredicate,
                    conn.getValueFactory().createLiteral(String.valueOf(integer), XMLSchema.INTEGER), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void removeTypeOn(Resource tripleSubject, RDFType tripleObject) {
        remove(tripleSubject, RDFOntology.TYPE, getURIReference(tripleObject.getURIText()));
    }



    public void remove(Resource tripleSubject, RDFProperty triplePredicate, String tripleObject) {
        AbstractSesameResource sesameSubject = (AbstractSesameResource) tripleSubject;
        URI sesameNativePredicate = getSesameNativeProperty(triplePredicate);

        try {
            conn.remove(sesameSubject.getSesameNativeResource(),
                    sesameNativePredicate,
                    conn.getValueFactory().createLiteral(tripleObject, XMLSchema.STRING), contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        }
    }


    public List<BoundVariables> select(String sparqlSelect) {
        List<BoundVariables> results = new LinkedList<>();
        TupleQueryResult tupleQueryResult = getTupleQueryResult(sparqlSelect);

        try {
            while (tupleQueryResult.hasNext()) {
                results.add(new SesameBoundVariables(tupleQueryResult.getBindingNames(), tupleQueryResult.next()));
            }
        } catch (QueryEvaluationException e) {
            throw new ShineRuntimeException(e);
        }

        return results;
    }

    private TupleQueryResult getTupleQueryResult(String sparqlSelect) {
        try {
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlSelect);
            contextualize(tupleQuery);
            return tupleQuery.evaluate();
        } catch (UnsupportedSPARQLStatementException e) {
            throw e;
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        }
    }

    private void contextualize(Query query) throws Exception {
        if (contextVar == null) {
            return;
        }

        TupleExpr tupleExpr = ((SailQuery) query).getParsedQuery().getTupleExpr();
        tupleExpr.visit(new QueryModelVisitorBase() {

            public void meet(StatementPattern node) throws Exception {
                if (node.getContextVar() != null) {
                    throw new UnsupportedSPARQLStatementException("Attempted to execute a SPARQL statement with a GRAPH clause against a context aware graph.");
                }
                node.setContextVar(contextVar);
            }
        });
    }

    private boolean getBooleanQueryResult(String sparqlSelect) {
        try {
            BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, sparqlSelect);
            contextualize(booleanQuery);
            return booleanQuery.evaluate();
        } catch (UnsupportedSPARQLStatementException e) {
            throw e;
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        }
    }


    public BoundVariables selectFirst(String sparqlSelect) {
        BoundVariables boundVariables;

        TupleQueryResult tupleQueryResult = getTupleQueryResult(sparqlSelect);

        try {
            if (!tupleQueryResult.hasNext()) {
                return null;
            }

            boundVariables = new SesameBoundVariables(tupleQueryResult.getBindingNames(), tupleQueryResult.next());

            if (tupleQueryResult.hasNext()) {
                tupleQueryResult.close();
                throw new MoreThanOneResultFoundException(sparqlSelect);
            }

        } catch (QueryEvaluationException e) {
            throw new ShineRuntimeException("Could not parse query: <<" + sparqlSelect + ">>", e);
        }

        return boundVariables;
    }


    public long size() {
        try {
            return conn.size(contextResource);
        } catch (RepositoryException e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void dump(Writer writer) {
        try {
            if (contextResource.length == 0) {
                RepositoryResult<org.openrdf.model.Resource> results = conn.getContextIDs();
                while (results.hasNext()) {
                    org.openrdf.model.Resource context = results.next();
                    writer.append("Dumping context:" + context + "\n");
                    conn.export(new RDFXMLWriter(writer), context);
                }
                dumpTriplesNotInContext(writer);
            } else {
                for (int i = 0; i < contextResource.length; i++) {
                    writer.append("Dumping context:" + contextResource[i].stringValue() + "\n");
                    conn.export(new RDFXMLWriter(writer), contextResource);
                }
            }
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void validate(String arq) {
        try {
            Query query = conn.prepareQuery(QueryLanguage.SPARQL, arq);
            contextualize(query);
        } catch (UnsupportedSPARQLStatementException e) {
            throw e;
        } catch (MalformedQueryException e) {
            throw new MalformedSPARQLException(e);
        } catch (Exception e) {
            throw new ShineRuntimeException(e);
        }
    }


    public void persistToTurtle(OutputStream outputStream) {
        try {
            conn.export(new N3Writer(outputStream));
        } catch (OpenRDFException e) {
            throw new ShineRuntimeException(e);
        }
    }

    private void dumpTriplesNotInContext(Writer writer) {
        try {
            writer.append("Statements not in any context: \n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RDFXMLWriter xmlWriter = new RDFXMLWriter(writer);
        xmlWriter.startRDF();
        try {
            RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
            while (result.hasNext()) {
                Statement statement = result.next();
                if (statement.getContext() == null) {
                    xmlWriter.handleStatement(statement);
                }
            }
        } catch (RepositoryException | RDFHandlerException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                xmlWriter.endRDF();
            } catch (RDFHandlerException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public boolean containsResourceWithURI(String URI) {
        return ask("ASK WHERE { <" + URI + "> ?p ?o . }");
    }


    public String toString() {
        if (contextResource.length == 0) {
            return "Graph bound to default context.";
        }
        return "Graph bound to " + contextResource[0];
    }
}
