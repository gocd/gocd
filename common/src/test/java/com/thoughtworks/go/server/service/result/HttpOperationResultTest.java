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
package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpOperationResultTest {
    private HttpOperationResult httpOperationResult;

    @BeforeEach
    public void setUp() {
        httpOperationResult = new HttpOperationResult();
    }

    @Test
    public void shouldReturn202IfEverythingWorks() {
        httpOperationResult.accepted("Request to schedule pipeline 'baboon' accepted","blah blah", HealthStateType.general(HealthStateScope.forPipeline("baboon")));
        assertThat(httpOperationResult.httpCode()).isEqualTo(202);
        assertThat(httpOperationResult.canContinue()).isTrue();
        assertThat(httpOperationResult.message()).isEqualTo("Request to schedule pipeline 'baboon' accepted");
    }

    @Test
    public void shouldReturn409IfPipelineCannotBeScheduled() {
        httpOperationResult.conflict("Pipeline is already scheduled", "", null);
        assertThat(httpOperationResult.httpCode()).isEqualTo(409);
        assertThat(httpOperationResult.canContinue()).isFalse();
        assertThat(httpOperationResult.message()).isEqualTo("Pipeline is already scheduled");
    }

    @Test
    public void shouldReturn404ForPipelineThatDoesntExist() {
        httpOperationResult.notFound("pipeline baboon doesn't exist", "", null);
        assertThat(httpOperationResult.httpCode()).isEqualTo(404);
        assertThat(httpOperationResult.canContinue()).isFalse();
        assertThat(httpOperationResult.message()).isEqualTo("pipeline baboon doesn't exist");
    }

    @Test
    public void shouldReturn406ForNotAcceptable() {
        httpOperationResult.notAcceptable("not acceptable", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode()).isEqualTo(406);
        assertThat(httpOperationResult.canContinue()).isFalse();
        assertThat(httpOperationResult.message()).isEqualTo("not acceptable");
    }

    @Test
    public void shouldReturnMessageWithDescription() {
        httpOperationResult.notAcceptable("message", "description", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode()).isEqualTo(406);
        assertThat(httpOperationResult.message()).isEqualTo("message");
        assertThat(httpOperationResult.detailedMessage()).isEqualTo("message { description }\n");
    }

    @Test
    public void successShouldReturnTrueIfStatusIs2xx(){
        assertThat(httpOperationResult.isSuccess()).isTrue();

        httpOperationResult.notAcceptable("not acceptable", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.isSuccess()).isFalse();

        httpOperationResult.ok("message");
        assertThat(httpOperationResult.isSuccess()).isTrue();

        httpOperationResult.error("message", "desc", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.isSuccess()).isFalse();

        httpOperationResult.accepted("Request to schedule pipeline 'baboon' accepted", "blah blah", HealthStateType.general(HealthStateScope.forPipeline("baboon")));
        assertThat(httpOperationResult.isSuccess()).isTrue();
    }

    @Test
    public void shouldReturnOnlyMessage_whenDescriptionIsBlank() {
        httpOperationResult.notAcceptable("message", "", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode()).isEqualTo(406);
        assertThat(httpOperationResult.message()).isEqualTo("message");
        assertThat(httpOperationResult.detailedMessage()).isEqualTo("message\n");
    }

    @Test
    public void shouldReturnOnlyMessage_whenServerHealthStateDoesntExist() {
        httpOperationResult.ok("message");
        assertThat(httpOperationResult.httpCode()).isEqualTo(200);
        assertThat(httpOperationResult.message()).isEqualTo("message");
        assertThat(httpOperationResult.detailedMessage()).isEqualTo("message\n");
    }

    @Test
    public void shouldSet_generic400_forError_becauseKeepingStatus_0_isNotCivilizedByHttpStandards() {
        httpOperationResult.error("message", "desc", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode()).isEqualTo(400);
        assertThat(httpOperationResult.message()).isEqualTo("message");
        assertThat(httpOperationResult.detailedMessage()).isEqualTo("message { desc }\n");
    }

    @Test
    public void shouldOmitDescriptionFromServerHealthStateWhenMessageAndDescriptionIsSame() {
        httpOperationResult.error("message", "message", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(httpOperationResult.httpCode()).isEqualTo(400);
        assertThat(httpOperationResult.message()).isEqualTo("message");
        assertThat(httpOperationResult.detailedMessage()).isEqualTo("message\n");
    }

    @Test
    public void shouldReturn500WhenInternalServerErrorOccurs() {
        httpOperationResult.internalServerError("error occurred during deletion of agent. Could not delete.", null);
        assertThat(httpOperationResult.httpCode()).isEqualTo(500);
        assertThat(httpOperationResult.canContinue()).isFalse();
        assertThat(httpOperationResult.message()).isEqualTo("error occurred during deletion of agent. Could not delete.");
    }
}
