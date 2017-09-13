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

package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.util.JsonTester;
import com.thoughtworks.go.util.JsonUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JobStatusJsonPresentationModelTest {

    @Test public void shouldShowBuildStatus() throws Exception {
        JobInstance instance = JobInstanceMother.assigned("test");
        instance.setId(12);
        instance.setAgentUuid("1234");

        final Agent agent = new Agent("1234", "cookie", "localhost", "1234");

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance,
                agent, mock(DurationBean.class), null);
        Map json = presenter.toJsonHash();

        new JsonTester(json).shouldContain(
                "{ 'name' : 'test',"
                        + " 'id' : '12', "
                        + " 'agent' : 'localhost', "
                        + " 'current_status' : 'assigned' "
                        + "}"
        );
    }

    @Test public void shouldShowBuildStatusForCompleted() throws Exception {
        JobInstance instance = JobInstanceMother.completed("test", JobResult.Passed);

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance);
        Map json = presenter.toJsonHash();

        new JsonTester(json).shouldContain(
                "{ 'name' : 'test',"
                        + " 'current_status' : 'passed' "
                        + "}"
        );
    }

    @Test public void shouldShowElapsedAndRemainingTimeForIncompleteBuild() throws Exception {
        JobInstance instance = JobInstanceMother.building("test", new DateTime().minusSeconds(5).toDate());

        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, mock(Agent.class),
                new DurationBean(instance.getId(), 10L), null);
        Map json = presenter.toJsonHash();

        new JsonTester(json).shouldContain(
                "{ 'name' : 'test',"
                        + " 'current_status' : 'building', "
                        + " 'current_build_duration' : '5', "
                        + " 'last_build_duration' : '10' "
                        + "}"
        );
    }

    @Test
    public void shouldReturnNotYetAssignedIfAgentUuidIsNull() throws Exception {
        JobInstance instance = JobInstanceMother.building("Plan1");
        instance.setAgentUuid(null);

        // "Not assigned" should depend on whether or not the JobInstance has an agentUuid, regardless of
        // the Agent object passed to the presenter, as this is the canonical definition of job assignment
        JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, mock(Agent.class), mock(DurationBean.class), null);

        JsonTester tester = new JsonTester(presenter.toJsonHash());
        tester.shouldContain(" { 'agent' : 'Not yet assigned' } ");
    }

    @Test
    public void shouldReturnAgentHostname() throws Exception {
        JobInstance instance = JobInstanceMother.building("Plan1");
        instance.setAgentUuid("1234");

        JobStatusJsonPresentationModel presenter =
                new JobStatusJsonPresentationModel(instance,
                        new Agent("1234", "cookie","localhost", "address"), mock(DurationBean.class), null);
        JsonTester tester = new JsonTester(presenter.toJsonHash());
        tester.shouldContain(" { 'agent' : 'localhost' } ");
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

    @Test
    public void shouldIncludeElasticAgentIdForElasticAgents() throws Exception {
        JobInstance instance = JobInstanceMother.completed("job-%", JobResult.Passed);
        instance.setIdentifier(new JobIdentifier("cruise-%", 1, "label-1", "dev-%", "1", "job-%", -1L));

        final AgentConfig agentConfig = new AgentConfig();
        agentConfig.setElasticAgentId("elastic_agent_id-1");
        agentConfig.setElasticPluginId("cd.go.docker.swarm");

        final JobStatusJsonPresentationModel presenter = new JobStatusJsonPresentationModel(instance, null,  new DurationBean(instance.getId()), agentConfig);
        final Map json = presenter.toJsonHash();

        assertThat(JsonUtils.from(json).getString("elastic_agent_id"), is("elastic_agent_id-1"));
    }
}
