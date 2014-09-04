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

package com.thoughtworks.studios.shine;

import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.GraphTestHelper;

import static org.junit.Assert.fail;

public class AssertUtils {
    public static void assertAskIsFalse(Graph graph, String... falsehoodAsks) {
        StringBuilder sb = new StringBuilder();
        boolean failures = false;
        for (String ask : falsehoodAsks) {
            if (graph.ask(ask)) {
                failures = true;
                sb.append(ask);
                sb.append("\n");
            }
        }

        if (failures) {
            fail("Graph:\n" + GraphTestHelper.dumpRDFXMLToString(graph) +
                    "\n\n" +
                    "Asks that are expecting to be false:\n" +
                    sb.toString());
        }
    }

    public static void assertAskIsTrue(Graph graph, String... truthAsks) {
        StringBuilder sb = new StringBuilder();
        boolean failures = false;
        for (String ask : truthAsks) {
            if (!graph.ask(ask)) {
                failures = true;
                sb.append(ask);
                sb.append("\n");
            }
        }

        if (failures) {
            fail("Graph:\n" +
                    com.thoughtworks.studios.shine.semweb.GraphTestHelper.dumpRDFXMLToString(graph) +
                    "\n\n" +
                    "Asks that are expecting to be true:\n" +
                    sb.toString());
        }
    }
}
