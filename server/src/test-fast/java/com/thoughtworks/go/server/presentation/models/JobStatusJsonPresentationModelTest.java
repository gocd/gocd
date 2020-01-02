/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.google.gson.Gson;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.util.JsonUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Map;

import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.helper.JobInstanceMother.*;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JobStatusJsonPresentationModelTest {

    @Test public void shouldShowBuildStatus() {
        JobInstance instance = assigned("test");
        instance.setId(12);
        instance.setAgentUuid("1234");

        final Agent agent = new Agent("1234","localhost", "1234", "cookie");

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance,
                agent, mock(DurationBean.class));
        Map json = presenter.toJsonHash();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"name\": \"test\",\n" +
                "  \"id\": \"12\",\n" +
                "  \"agent\": \"localhost\",\n" +
                "  \"current_status\": \"assigned\"\n" +
                "}");
    }

    @Test public void shouldShowBuildStatusForCompleted() {
        JobInstance instance = completed("test", Passed);

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map json = presenter.toJsonHash();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"name\": \"test\",\n" +
                "  \"current_status\": \"passed\"\n" +
                "}");
    }

    @Test public void shouldShowElapsedAndRemainingTimeForIncompleteBuild() throws Exception {
        JobInstance instance = building("test", new DateTime().minusSeconds(5).toDate());

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, mock(Agent.class),
                new DurationBean(instance.getId(), 10L));
        Map json = presenter.toJsonHash();

        assertThatJson(new Gson().toJson(json)).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"name\": \"test\",\n" +
                "  \"current_status\": \"building\",\n" +
                "  \"current_build_duration\": \"5\",\n" +
                "  \"last_build_duration\": \"10\"\n" +
                "}");
    }

    @Test
    public void shouldReturnNotYetAssignedIfAgentUuidIsNull() throws Exception {
        JobInstance instance = building("Plan1");
        instance.setAgentUuid(null);

        // "Not assigned" should depend on whether or not the JobInstance has an agentUuid, regardless of
        // the Agent object passed to the presenter, as this is the canonical definition of job assignment
        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, mock(Agent.class), mock(DurationBean.class));

        assertThatJson(new Gson().toJson(presenter.toJsonHash())).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n  \"agent\": \"Not yet assigned\"\n}");
    }

    @Test
    public void shouldReturnAgentHostname() throws Exception {
        JobInstance instance = building("Plan1");
        instance.setAgentUuid("1234");

        JobStatusJsonPresentationModel presenter =
                new JobStatusJsonPresentationModel(instance,
                        new Agent("1234","localhost", "address", "cookie"), mock(DurationBean.class));
        assertThatJson(new Gson().toJson(presenter.toJsonHash())).when(IGNORING_EXTRA_FIELDS).isEqualTo("{\n" +
                "  \"agent\": \"localhost\"\n" +
                "}");
    }

    @Test public void shouldShowArtifactTabwhenBuildPassed() throws Exception {
        JobInstance instance = JobInstanceMother.passed("plan1");
        JobStatusJsonPresentationModel buildStatusJson = new JobStatusJsonPresentationModel(instance);
        assertThat(buildStatusJson.getTabToShow(), is("#tab-artifacts"));
    }

    @Test public void shouldShowFailuresTabwhenBuildFailed() throws Exception {
        JobInstance instance = JobInstanceMother.failed("plan1");
        JobStatusJsonPresentationModel buildStatusJson = new JobStatusJsonPresentationModel(instance);
        assertThat(buildStatusJson.getTabToShow(), is("#tab-failures"));
    }

    @Test public void shouldShowDefaultTabwhenBuildIsNeitherFailedNorPassed() throws Exception {
        JobInstance instance = JobInstanceMother.cancelled("plan1");
        JobStatusJsonPresentationModel buildStatusJson = new JobStatusJsonPresentationModel(instance);
        assertThat(buildStatusJson.getTabToShow(), is(""));
    }

    @Test public void shouldEncodeBuildLocator() throws Exception {
        JobInstance instance = JobInstanceMother.completed("job-%", JobResult.Passed);
        instance.setIdentifier(new JobIdentifier("cruise-%", 1, "label-1", "dev-%", "1", "job-%", -1L));

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map json = presenter.toJsonHash();

        assertThat(JsonUtils.from(json).getString("buildLocator"), is("cruise-%25/1/dev-%25/1/job-%25"));
    }

    @Test public void shouldIncludeBuildLocatorForDisplay() throws Exception {
        JobInstance instance = JobInstanceMother.completed("job-%", JobResult.Passed);
        instance.setIdentifier(new JobIdentifier("cruise-%", 1, "label-1", "dev-%", "1", "job-%", -1L));

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map json = presenter.toJsonHash();

        assertThat(JsonUtils.from(json).getString("buildLocatorForDisplay"), is("cruise-%/label-1/dev-%/1/job-%"));
    }

}
