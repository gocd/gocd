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

package com.thoughtworks.go.util.json;

import com.thoughtworks.go.server.web.JsonStringRenderer;
import org.hamcrest.core.Is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class JsonpStringTest {
    @Test
    public void shouldWrapJsonWithCallbackMethod() {
        JsonMap map = new JsonMap();
        map.put("pipelines", "blah");
        JsonpString jsonpString = new JsonpString(map, "callback");

        assertThat(JsonStringRenderer.render(jsonpString), Is.is("callback({ \"pipelines\": \"blah\" })"));
    }
}
