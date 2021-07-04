/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
@EnableRuleMigrationSupport
public class AgentAssignmentTest {
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper;

    @BeforeEach
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        agentAssignment.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        fixture.onTearDown();
        agentAssignment.clear();
    }

    @Test
    public void shouldGetLatestActiveJobOnAgent() {
        JobInstance assigned = JobInstanceMother.assignedWithAgentId("dev", "uuid");
        agentAssignment.jobStatusChanged(assigned);

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid"), is(assigned));
    }

    @Test
    public void shouldIgnoreScheduledJob() {
        JobInstance scheduled = JobInstanceMother.scheduled("dev");
        agentAssignment.jobStatusChanged(scheduled);

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid"), is(nullValue()));
    }

    @Test
    public void shouldGetLatestActiveJobOnAgentFromDatabase() {
        Pipeline pipeline = fixture.createPipelineWithFirstStageAssigned("uuid");
        JobInstance expected = pipeline.getFirstStage().getJobInstances().first();

        assertThat(agentAssignment.latestActiveJobOnAgent("uuid").getId(), is(expected.getId()));
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


