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

package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HttpOperationResultTest {
    private HttpOperationResult httpOperationResult;

    @Before public void setUp() {
        httpOperationResult = new HttpOperationResult();
    }
    
    @Test public void shouldReturn401ForInsufficientPermissions() throws Exception {
        httpOperationResult.unauthorized("permission denied","blah blah", HealthStateType.general(HealthStateScope.forPipeline("baboon")));
        assertThat(httpOperationResult.httpCode(), is(401));
        assertThat(httpOperationResult.canContinue(), is(false));
        assertThat(httpOperationResult.message(), is("permission denied"));
    }

    @Test public void shouldReturn202IfEverythingWorks() throws Exception {
        httpOperationResult.accepted("Request to schedule pipeline 'baboon' accepted","blah blah", HealthStateType.general(HealthStateScope.forPipeline("baboon")));
        assertThat(httpOperationResult.httpCode(), is(202));
        assertThat(httpOperationResult.canContinue(), is(true));
        assertThat(httpOperationResult.message(), is("Request to schedule pipeline 'baboon' accepted"));
    }

    @Test public void shouldReturn409IfPipelineCannotBeScheduled() throws Exception {
        httpOperationResult.conflict("Pipeline is already scheduled", "", null);
        assertThat(httpOperationResult.httpCode(), is(409));
        assertThat(httpOperationResult.canContinue(), is(false));
        assertThat(httpOperationResult.message(), is("Pipeline is already scheduled"));
    }

    @Test public void shouldReturn404ForPipelineThatDoesntExist() throws Exception {
        httpOperationResult.notFound("pipeline baboon doesn't exist", "", null);
        assertThat(httpOperationResult.httpCode(), is(404));
        assertThat(httpOperationResult.canContinue(), is(false));
        assertThat(httpOperationResult.message(), is("pipeline baboon doesn't exist"));
    }

    @Test
    public void shouldReturn406ForNotAcceptable() {
        httpOperationResult.notAcceptable("not acceptable", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode(), is(406));
        assertThat(httpOperationResult.canContinue(), is(false));
        assertThat(httpOperationResult.message(), is("not acceptable"));
    }

    @Test
    public void shouldReturnMessageWithDescription() {
        httpOperationResult.notAcceptable("message", "description", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode(), is(406));
        assertThat(httpOperationResult.message(), is("message"));
        assertThat(httpOperationResult.detailedMessage(), is("message { description }\n"));
    }

    @Test
    public void successShouldReturnTrueIfStatusIs2xx(){
        assertThat(httpOperationResult.isSuccess(), is(true));

        httpOperationResult.notAcceptable("not acceptable", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.isSuccess(), is(false));

        httpOperationResult.ok("message");
        assertThat(httpOperationResult.isSuccess(), is(true));

        httpOperationResult.error("message", "desc", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.isSuccess(), is(false));

        httpOperationResult.accepted("Request to schedule pipeline 'baboon' accepted", "blah blah", HealthStateType.general(HealthStateScope.forPipeline("baboon")));
        assertThat(httpOperationResult.isSuccess(), is(true));
    }

    @Test
    public void shouldReturnOnlyMessage_whenDescriptionIsBlank() {
        httpOperationResult.notAcceptable("message", "", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode(), is(406));
        assertThat(httpOperationResult.message(), is("message"));
        assertThat(httpOperationResult.detailedMessage(), is("message\n"));
    }

    @Test
    public void shouldReturnOnlyMessage_whenServerHealthStateDoesntExist() {
        httpOperationResult.ok("message");
        assertThat(httpOperationResult.httpCode(), is(200));
        assertThat(httpOperationResult.message(), is("message"));
        assertThat(httpOperationResult.detailedMessage(), is("message\n"));
    }

    @Test
    public void shouldSet_generic400_forError_becauseKeepingStatus_0_isNotCivilizedByHttpStandards() {
        httpOperationResult.error("message", "desc", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode(), is(400));
        assertThat(httpOperationResult.message(), is("message"));
        assertThat(httpOperationResult.detailedMessage(), is("message { desc }\n"));
    }

    @Test public void shouldReturn500WhenInternalServerErrorOccurs() throws Exception {
        httpOperationResult.internalServerError("error occurred during deletion of agent. Could not delete.", null);
        assertThat(httpOperationResult.httpCode(), is(500));
        assertThat(httpOperationResult.canContinue(), is(false));
        assertThat(httpOperationResult.message(), is("error occurred during deletion of agent. Could not delete."));
    }
}
