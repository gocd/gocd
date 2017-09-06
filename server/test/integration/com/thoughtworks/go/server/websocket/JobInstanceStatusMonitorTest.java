/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.io.IOException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class JobInstanceStatusMonitorTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigService goConfigService;
    @Autowired private AgentRemoteHandler agentRemoteHandler;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private BuildAssignmentService buildAssignmentService;

    private static final String STAGE_NAME = "dev";
    private GoConfigFileHelper configHelper;

    public Subversion repository;
    public static TestRepo testRepo;
    private PipelineWithTwoStages fixture;
    private AgentStub agent;
    @Autowired private TimeProvider timeProvider;

    @BeforeClass
    public static void setupRepos() throws IOException {
        testRepo = new SvnTestRepo("testSvnRepo");
    }

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        TestRepo.internalTearDown();
    }

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        PipelineConfig evolveConfig = configHelper.addPipeline("evolve", STAGE_NAME, repository, "unit");
        configHelper.addPipeline("anotherPipeline", STAGE_NAME, repository, "anotherTest");
        configHelper.addPipeline("thirdPipeline", STAGE_NAME, repository, "yetAnotherTest");
        goConfigService.forceNotifyListeners();
        goCache.clear();

        agent = new AgentStub();
    }

    @After
    public void teardown() throws Exception {
        goCache.clear();
        fixture.onTearDown();
        dbHelper.onTearDown();
        configHelper.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        agentRemoteHandler.connectedAgents().clear();
    }

    @Test
    public void shouldSendCancelMessageIfJobIsCancelled() throws Exception {
        AgentConfig agentConfig = AgentMother.remoteAgent();
        configHelper.addAgent(agentConfig);
        fixture.createPipelineWithFirstStageScheduled();
        AgentRuntimeInfo info = AgentRuntimeInfo.fromServer(agentConfig, true, "location", 1000000l, "OS", false, timeProvider);
        info.setCookie("cookie");
        agentRemoteHandler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        buildAssignmentService.onTimer();

        assertThat(agent.messages.size(), is(1));
        Work work = MessageEncoding.decodeWork(agent.messages.get(0).getData());
        assertThat(work, instanceOf(BuildWork.class));
        JobPlan jobPlan = ((BuildWork) work).getAssignment().getPlan();
        final JobInstance instance = jobInstanceService.buildByIdWithTransitions(jobPlan.getJobId());
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceService.cancelJob(instance);
            }
        });

        assertThat(agent.messages.size(), is(2));
        assertThat(agent.messages.get(1).getAction(), is(Action.cancelBuild));
    }

    @Test
    public void shouldSendCancelMessageIfJobIsRescheduled() throws Exception {
        AgentConfig agentConfig = AgentMother.remoteAgent();
        configHelper.addAgent(agentConfig);
        fixture.createPipelineWithFirstStageScheduled();
        AgentRuntimeInfo info = AgentRuntimeInfo.fromServer(agentConfig, true, "location", 1000000l, "OS", false, timeProvider);
        info.setCookie("cookie");
        agentRemoteHandler.process(agent, new Message(Action.ping, MessageEncoding.encodeData(info)));

        buildAssignmentService.onTimer();

        assertThat(agent.messages.size(), is(1));
        assertThat(MessageEncoding.decodeWork(agent.messages.get(0).getData()), instanceOf(BuildWork.class));
        BuildWork work = (BuildWork) MessageEncoding.decodeWork(agent.messages.get(0).getData());
        JobPlan jobPlan = work.getAssignment().getPlan();
        final JobInstance instance = jobInstanceService.buildByIdWithTransitions(jobPlan.getJobId());
        scheduleService.rescheduleJob(instance);

        assertThat(agent.messages.size(), is(2));
        assertThat(agent.messages.get(1).getAction(), is(Action.cancelBuild));
    }
}
