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

package com.thoughtworks.go.performance;

import com.thoughtworks.go.util.JsonTester;
import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *   These tests should contain simple json assertions to make sure the response isn't empty;
 *   put fine-grained assertions in the corresponding jwebunit tests.
 */
public class PipelineJsonPerfTest extends BaseJsonPerf {

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldListAllPipelines() throws Exception {
        String json = web.httpRequest("pipelineStatus.json", RequestMethod.GET);
        final JsonTester jsonTester = new JsonTester(json);
        jsonTester.shouldContain(
                "{ 'pipelines' : [ { 'name' : '" + pipelineName + "' }] }"
        );
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldRetrievePipelineHistory() throws Exception {
        String json = web.httpRequest("pipelineHistory.json", RequestMethod.GET, "?pipelineName=" + pipelineName);
        new JsonTester(json).shouldContain(
                "{ 'pipelineName' : '" + pipelineName + "' }");
    }
}