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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.UnknownHostException;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})

public class JobAssignmentIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao cruiseConfigDao;
    @Autowired private BuildAssignmentService assignmentService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private AgentService agentService;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages fixture;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(cruiseConfigDao);
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).usingThreeJobs().onSetUp();
        systemEnvironment = new SystemEnvironment();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
    }

    @Test
    public void shouldAssignJobToRemoteAgent() throws UnknownHostException {
        AgentInstance local = setupLocalAgent();
        AgentInstance remote = setupRemoteAgent();
        fixture.createPipelineWithFirstStageScheduled();

        assignmentService.onTimer();

        assignmentService.assignWorkToAgent(local);

        assignmentService.onTimer();

        Work work = assignmentService.assignWorkToAgent(remote);
        assertThat(work, instanceOf(BuildWork.class));
    }

    @Test
    public void shouldNotAssignJobToRemoteAgentIfReachedLimit() throws UnknownHostException {
        AgentInstance local = setupLocalAgent();
        AgentInstance remote = setupRemoteAgent();
        AgentInstance remote2 = setupRemoteAgent();
        fixture.createPipelineWithFirstStageScheduled();

        assignmentService.onConfigChange(null);

        assignmentService.assignWorkToAgent(local);
        assignmentService.assignWorkToAgent(remote);
        Work work = assignmentService.assignWorkToAgent(remote2);
        assertThat(work, instanceOf(NoWork.class));
    }

    @Test
    public void shouldAssignJobToLocalAgentEvenReachedLimit() throws UnknownHostException {
        AgentInstance local = setupLocalAgent();
        AgentInstance remote = setupRemoteAgent();
        fixture.createPipelineWithFirstStageScheduled();

        assignmentService.onTimer();

        assignmentService.assignWorkToAgent(remote);
        Work work = assignmentService.assignWorkToAgent(local);
        assertThat(work, instanceOf(BuildWork.class));
    }

    private AgentInstance setupRemoteAgent() {
        Agent agent = AgentMother.remoteAgent();
        agentService.saveOrUpdate(agent);
        AgentInstance instance = AgentInstance.createFromAgent(agent, systemEnvironment, agentStatusChangeListener());
        instance.enable();
        return instance;
    }

    private AgentInstance setupLocalAgent() throws UnknownHostException {
        Agent agent = AgentMother.localAgent();
        agentService.saveOrUpdate(agent);
        return AgentInstance.createFromAgent(agent, systemEnvironment, agentStatusChangeListener());
    }

    private AgentStatusChangeListener agentStatusChangeListener() {
        return new AgentStatusChangeListener() {
            @Override
            public void onAgentStatusChange(AgentInstance agentInstance) {

            }
        };
    }

}
