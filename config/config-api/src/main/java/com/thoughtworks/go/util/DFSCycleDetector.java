/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.util;

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class DFSCycleDetector {
    public final void topoSort(final CaseInsensitiveString root, final PipelineDependencyState pipelineDependencyState) {
        Map<CaseInsensitiveString, CycleState> state = new HashMap<>();
        Stack<CaseInsensitiveString> visiting = new Stack<>();

        CycleState prevState = state.putIfAbsent(root, CycleState.VISITING);
        if (prevState == null) {
            tsort(root, pipelineDependencyState, state, visiting);
            state.put(root, CycleState.VISITED);
        } else if (prevState == CycleState.VISITING) {
            throw new RuntimeException("Unexpected node in visiting state: " + root);
        }
        assertHasVisitedAllNodesInTree(state);
    }

    private void tsort(final CaseInsensitiveString root, final PipelineDependencyState pipelineDependencyState, final Map<CaseInsensitiveString, CycleState> state, Stack<CaseInsensitiveString> visiting) {
        visiting.push(root);

        // Make sure we exist
        validateRootExists(root, pipelineDependencyState, visiting);
        Node stage = pipelineDependencyState.getDependencyMaterials(root);
        stage.getDependencies().stream()
            .map(Node.DependencyNode::getPipelineName)
            .forEach(cur -> {
                    CycleState prevState = state.putIfAbsent(cur, CycleState.VISITING);
                    if (prevState == null) {
                        tsort(cur, pipelineDependencyState, state, visiting);
                        state.put(cur, CycleState.VISITED);
                    } else if (prevState == CycleState.VISITING) {
                        // Currently visiting this node, so have a cycle
                        throwCircularException(cur, visiting);
                    }
                });
        popAndAssertTopIsConsistent(visiting, root);
    }

    private void assertHasVisitedAllNodesInTree(Map<CaseInsensitiveString, CycleState> state) {
        for (Map.Entry<CaseInsensitiveString, CycleState> cycleStateEntry : state.entrySet()) {
            if (cycleStateEntry.getValue() == CycleState.VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: " + cycleStateEntry.getKey());
            }
        }
    }

    private void popAndAssertTopIsConsistent(Stack<CaseInsensitiveString> visiting, CaseInsensitiveString root) {
        CaseInsensitiveString p = visiting.pop();
        if (!root.equals(p)) {
            throw new RuntimeException("Unexpected internal error: expected to pop " + root + " but got " + p);
        }
    }

    private void validateRootExists(CaseInsensitiveString root, PipelineDependencyState pipelineDependencyState, Stack<CaseInsensitiveString> visiting) {
        if (!pipelineDependencyState.hasPipeline(root)) {
            StringBuilder sb = new StringBuilder("Pipeline '");
            sb.append(root);
            sb.append("' does not exist.");
            visiting.pop();
            if (!visiting.empty()) {
                CaseInsensitiveString parent = visiting.peek();
                sb.append(" It is used from pipeline '");
                sb.append(parent);
                sb.append("'.");
            }
            throw new RuntimeException(sb.toString());
        }
    }

    private static void throwCircularException(CaseInsensitiveString end, Stack<CaseInsensitiveString> stk) {
        StringBuilder sb = new StringBuilder("Circular dependency: ");
        sb.append(end);
        CaseInsensitiveString c;
        do {
            c = stk.pop();
            sb.append(" <- ");
            sb.append(c);
        } while (!c.equals(end));
        throw new RuntimeException(new String(sb));
    }

    private enum CycleState {
        VISITED, VISITING
    }
}
