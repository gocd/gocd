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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.helper.JobInstanceMother.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.mockito.Mockito.mock;

public class JobStatusJsonPresentationModelTest {

    @Test
    public void shouldShowBuildStatus() {
        JobInstance instance = assigned("test");
        instance.setId(12);
        instance.setAgentUuid("1234");

        final Agent agent = new Agent("1234", "localhost", "1234", "cookie");

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance,
            agent, mock(DurationBean.class));
        Map<String, Object> json = presenter.toJsonHash();

        assertThatJson(JsonHelper.toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
            {
              "name": "test",
              "id": "12",
              "agent": "localhost",
              "current_status": "assigned"
            }""");
    }

    @Test
    public void shouldShowBuildStatusForCompleted() {
        JobInstance instance = completed("test", Passed);

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map<String, Object> json = presenter.toJsonHash();

        assertThatJson(JsonHelper.toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
            {
              "name": "test",
              "current_status": "passed"
            }""");
    }

    @Test
    public void shouldShowElapsedAndRemainingTimeForIncompleteBuild() {
        JobInstance instance = building("test", Date.from(Instant.now().minusSeconds(5)));

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, mock(Agent.class),
            new DurationBean(instance.getId(), 10L));
        Map<String, Object> json = presenter.toJsonHash();

        assertThatJson(JsonHelper.toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
            {
              "name": "test",
              "current_status": "building",
              "current_build_duration": "5",
              "last_build_duration": "10"
            }""");
    }

    @Test
    public void shouldReturnNotYetAssignedIfAgentUuidIsNull() {
        JobInstance instance = building("Plan1");
        instance.setAgentUuid(null);

        // "Not assigned" should depend on whether or not the JobInstance has an agentUuid, regardless of
        // the Agent object passed to the presenter, as this is the canonical definition of job assignment
        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, mock(Agent.class), mock(DurationBean.class));

        assertThatJson(JsonHelper.toJson(presenter.toJsonHash())).when(IGNORING_EXTRA_FIELDS)
            .isEqualTo("""
                {
                  "agent": "Not yet assigned"
                }""");
    }

    @Test
    public void shouldReturnAgentHostname() {
        JobInstance instance = building("Plan1");
        instance.setAgentUuid("1234");

        JobStatusJsonPresentationModel presenter =
            new JobStatusJsonPresentationModel(instance,
                new Agent("1234", "localhost", "address", "cookie"), mock(DurationBean.class));
        assertThatJson(JsonHelper.toJson(presenter.toJsonHash())).when(IGNORING_EXTRA_FIELDS).isEqualTo("""
            {
              "agent": "localhost"
            }""");
    }

    @Test
    public void shouldEncodeBuildLocator() {
        JobInstance instance = JobInstanceMother.completed("job-%", JobResult.Passed);
        instance.setIdentifier(new JobIdentifier("cruise-%", 1, "label-1", "dev-%", "1", "job-%", -1L));

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map<String, Object> json = presenter.toJsonHash();

        assertThatJson(json).node("buildLocator").isEqualTo("cruise-%25/1/dev-%25/1/job-%25");
    }

    @Test
    public void shouldIncludeBuildLocatorForDisplay() {
        JobInstance instance = JobInstanceMother.completed("job-%", JobResult.Passed);
        instance.setIdentifier(new JobIdentifier("cruise-%", 1, "label-1", "dev-%", "1", "job-%", -1L));

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map<String, Object> json = presenter.toJsonHash();

        assertThatJson(json).node("buildLocatorForDisplay").isEqualTo("cruise-%/label-1/dev-%/1/job-%");
    }

}
