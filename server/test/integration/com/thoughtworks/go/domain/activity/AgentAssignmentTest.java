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

package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class AgentAssignmentTest {
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        agentAssignment.clear();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
        agentAssignment.clear();
    }

    @Test
    public void shouldGetLatestActiveJobOnAgent() {
        JobInstance assigned = JobInstanceMother.assignedWithAgentId("dev", "uuid");
        agentAssignment.jobStatusChanged(assigned);

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid"), Is.is(assigned));
    }

    @Test
    public void shouldIgnoreScheduledJob() {
        JobInstance scheduled = JobInstanceMother.scheduled("dev");
        agentAssignment.jobStatusChanged(scheduled);

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid"), Is.is(nullValue()));
    }

    @Test
    public void shouldGetLatestActiveJobOnAgentFromDatabase() {
        Pipeline pipeline = fixture.createPipelineWithFirstStageAssigned("uuid");
        JobInstance expected = pipeline.getFirstStage().getJobInstances().first();

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid").getId(), Is.is(expected.getId()));
    }

    @Test
    public void shouldReturnNullIfNoActiveJobOnAgent() {
        JobInstance completed = JobInstanceMother.passed("dev");
        completed.setAgentUuid("uuid");
        agentAssignment.jobStatusChanged(completed);

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullAfterJobIsRescheduled() {
        JobInstance rescheduled = JobInstanceMother.rescheduled("dev", "uuid");
        agentAssignment.jobStatusChanged(rescheduled);

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid"), is(nullValue()));
    }
}

  
