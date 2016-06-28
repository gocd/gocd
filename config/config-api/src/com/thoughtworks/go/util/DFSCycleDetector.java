/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import com.thoughtworks.go.config.CaseInsensitiveString;

public class DFSCycleDetector {
    public final void topoSort(final CaseInsensitiveString root, final PipelineDependencyState pipelineDependencyState) throws Exception {
        Hashtable<CaseInsensitiveString, CycleState> state = new Hashtable<>();
        Stack<CaseInsensitiveString> visiting = new Stack<>();

        if (!state.containsKey(root)) {
            tsort(root, pipelineDependencyState, state, visiting);
        } else if (state.get(root) == CycleState.VISITING) {
            throw ExceptionUtils.bomb("Unexpected node in visiting state: " + root);
        }
        assertHasVisitedAllNodesInTree(state);
    }

    private void tsort(final CaseInsensitiveString root, final PipelineDependencyState pipelineDependencyState, final Hashtable<CaseInsensitiveString, CycleState> state, Stack<CaseInsensitiveString> visiting) throws Exception {
        state.put(root, CycleState.VISITING);
        visiting.push(root);

        // Make sure we exist
        validateRootExists(root, pipelineDependencyState, visiting);
        Node stage = pipelineDependencyState.getDependencyMaterials(root);
        for (Node.DependencyNode cur : stage.getDependencies()) {
            if (!state.containsKey(cur.getPipelineName())) {
                // Not been visited
                tsort(cur.getPipelineName(), pipelineDependencyState, state, visiting);
            } else if (state.get(cur.getPipelineName()) == CycleState.VISITING) {
                // Currently visiting this node, so have a cycle
                throwCircularException(cur.getPipelineName(), visiting);
            }
        }
        popAndAssertTopIsConsistent(visiting, root);
        state.put(root, CycleState.VISITED);
    }

    private void assertHasVisitedAllNodesInTree(Hashtable<CaseInsensitiveString, CycleState> state) {
        for (Map.Entry<CaseInsensitiveString, CycleState> cycleStateEntry : state.entrySet()) {
            if (cycleStateEntry.getValue() == CycleState.VISITING) {
                throw ExceptionUtils.bomb("Unexpected node in visiting state: " + cycleStateEntry.getKey());
            }
        }
    }

    private void popAndAssertTopIsConsistent(Stack<CaseInsensitiveString> visiting, CaseInsensitiveString root) {
        CaseInsensitiveString p = visiting.pop();
        if (!root.equals(p)) {
            throw ExceptionUtils.bomb("Unexpected internal error: expected to pop " + root + " but got " + p);
        }
    }

    private void validateRootExists(CaseInsensitiveString root, PipelineDependencyState pipelineDependencyState, Stack<CaseInsensitiveString> visiting) throws Exception {
        if (!pipelineDependencyState.hasPipeline(root)) {
            StringBuffer sb = new StringBuffer("Pipeline \"");
            sb.append(root);
            sb.append("\" does not exist.");
            visiting.pop();
            if (!visiting.empty()) {
                CaseInsensitiveString parent = visiting.peek();
                sb.append(" It is used from pipeline \"");
                sb.append(parent);
                sb.append("\".");
            }
            throw new Exception(sb.toString());
        }
    }

    private static void throwCircularException(CaseInsensitiveString end, Stack<CaseInsensitiveString> stk) throws Exception {
        StringBuffer sb = new StringBuffer("Circular dependency: ");
        sb.append(end);
        CaseInsensitiveString c;
        do {
            c = stk.pop();
            sb.append(" <- ");
            sb.append(c);
        } while (!c.equals(end));
        throw new Exception(new String(sb));
    }

    private static enum CycleState {
        VISITED, VISITING
    }
}
