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

package com.thoughtworks.go.util;

import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import com.thoughtworks.go.config.CaseInsensitiveString;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class DFSCycleDetector {

    public final void topoSort(CaseInsensitiveString root, Hashtable<CaseInsensitiveString, Node> targetTable) throws Exception {
        Hashtable<CaseInsensitiveString, CycleState> state = new Hashtable<CaseInsensitiveString, CycleState>();
        Stack<CaseInsensitiveString> visiting = new Stack<CaseInsensitiveString>();

        if (!state.containsKey(root)) {
            tsort(root, targetTable, state, visiting);
        } else if (state.get(root) == CycleState.VISITING) {
            throw ExceptionUtils.bomb("Unexpected node in visiting state: " + root);
        }

        assertHasVisitedAllNodesInTree(state);
    }

    private void assertHasVisitedAllNodesInTree(Hashtable<CaseInsensitiveString, CycleState> state) {
        for (Map.Entry<CaseInsensitiveString, CycleState> cycleStateEntry : state.entrySet()) {
            if (cycleStateEntry.getValue() == CycleState.VISITING) {
                throw ExceptionUtils.bomb("Unexpected node in visiting state: " + cycleStateEntry.getKey());
            }
        }
    }

    private void tsort(CaseInsensitiveString root, Hashtable<CaseInsensitiveString, Node> stageTable,
                       Hashtable<CaseInsensitiveString, CycleState> state, Stack<CaseInsensitiveString> visiting) throws Exception {
        state.put(root, CycleState.VISITING);
        visiting.push(root);

        // Make sure we exist
        validateRootExists(root, stageTable, visiting);

        Node stage = stageTable.get(root);
        for (CaseInsensitiveString cur : stage.getDependencies()) {
            if (!state.containsKey(cur)) {
                // Not been visited
                tsort(cur, stageTable, state, visiting);
            } else if (state.get(cur) == CycleState.VISITING) {
                // Currently visiting this node, so have a cycle
                throwCircularException(cur, visiting);
            }
        }
        popAndAssertTopIsConsistent(visiting, root);
        state.put(root, CycleState.VISITED);
    }

    private void popAndAssertTopIsConsistent(Stack<CaseInsensitiveString> visiting, CaseInsensitiveString root) {
        CaseInsensitiveString p = visiting.pop();
        if (!root.equals(p)) {
            throw ExceptionUtils.bomb("Unexpected internal error: expected to pop " + root + " but got " + p);
        }
    }

    private void validateRootExists(CaseInsensitiveString root, Hashtable<CaseInsensitiveString, Node> stageTable, Stack<CaseInsensitiveString> visiting) throws Exception {
        if (!stageTable.containsKey(root)) {
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
