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

import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import static com.thoughtworks.go.util.json.JsonList.jsonList;
import org.junit.Test;

public class JsonTesterTest {
    @Test public void shouldParseNewJsonCorrectly() throws Exception {
        JsonMap json = new JsonMap();
        json.put("one", "1");
        json.put("two",
                jsonList("monkey", "baboon"));
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
        JsonMap json = new JsonMap();
        json.put("first", "1");
        json.put("second",
                jsonList("a", "b", "c"));
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
    
    @Test public void shouldAllowPartialListMatches() throws Exception {
        JsonList json = new JsonList();
        json.add(map("one", "1"));
        json.add(map("two", "2"));
        json.add(map("six", "6"));
        JsonTester tester = new JsonTester(json);
        tester.shouldContain(
                  "[ { 'one' : '1' },  "
                + "  { 'two' : '2' } ] "
        );

        tester.shouldContain(
                  "[ { 'two' : '2' }, "
                + "  { 'six' : '6' } ]"
        );
    }


    @Test public void shouldSupportRegX() throws Exception {
        JsonList json = new JsonList();
        json.add(map("one", "11"));
        json.add(map("two", "2"));
        json.add(map("six", "6"));
        JsonTester tester = new JsonTester(json);
        tester.shouldContain(
                  "[ { 'one' : \"RegEx:\\\\d+\" },  "
                + "  { 'two' : '2' } ] "
        );
    }

    private JsonMap map(String key, String value) {
        JsonMap jsonMap = new JsonMap();
        jsonMap.put(key, value);
        return jsonMap;
    }
}
