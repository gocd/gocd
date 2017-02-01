/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import org.junit.Test;

import java.util.*;

public class JsonTesterTest {
    @Test public void shouldParseNewJsonCorrectly() throws Exception {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("one", "1");
        json.put("two",
                Arrays.asList("monkey", "baboon"));
        new JsonTester(json).is(
                "{ 'one' : '1',  "
                        + "  'two' : [     "
                        + "    'monkey',   "
                        + "    'baboon'    "
                        + "  ]             "
                        + "}"
        );
    }

    @Test public void shouldAllowPartialMapMatch() throws Exception {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("first", "1");
        json.put("second",
                Arrays.asList("a", "b", "c"));
        JsonTester tester = new JsonTester(json);
        tester.shouldContain(
                "{ 'first' : '1' }"
        );
        tester.shouldContain(
                "{ 'second' : [ "
                        + "    'a',       "
                        + "    'c',       "
                        + "    'b'        "
                        + "  ]            "
                        + "}              "
        );
    }

}
