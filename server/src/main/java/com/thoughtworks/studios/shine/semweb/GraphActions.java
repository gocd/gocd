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

import java.util.ArrayList;
import java.util.List;

public class GraphActions {

    interface Action {
        void execute();
    }

    private Graph graph;
    private List<Action> actions = new ArrayList<>();

    public GraphActions(Graph graph) {
        this.graph = graph;
    }

    public void execute() {
        for (Action a : actions) {
            a.execute();
        }
    }

    public void queueRemoveResourceStatement(URIReference subject, RDFProperty predicate, URIReference object) {
        actions.add(new RemoveResourceStatementAction(graph, subject, predicate, object));
    }

    public void queueRemoveResourceStatement(URIReference subject, RDFType type) {
        actions.add(new RemoveResourceStatementAction(graph, subject, RDFOntology.TYPE, graph.getURIReference(type.getURIText())));
    }

    private static final class RemoveResourceStatementAction implements Action {
        private URIReference subject;
        private RDFProperty predicate;
        private URIReference object;
        private Graph graph;

        RemoveResourceStatementAction(Graph graph, URIReference subject, RDFProperty predicate, URIReference object) {
            this.graph = graph;
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
        }

        @Override
        public void execute() {
            graph.remove(subject, predicate, object);
        }
    }

}
