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

package com.thoughtworks.go.server.service;

import java.io.IOException;
import java.sql.SQLException;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ScheduleServiceRescheduleHungJobsIntegrationTest {

    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private BuildAssignmentService buildAssignmentService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private StageService stageService;
    @Autowired private GoCache goCache;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private InstanceFactory instanceFactory;

    private PipelineConfig evolveConfig;
    private static final String STAGE_NAME = "dev";
    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    public Subversion repository;
    public static TestRepo testRepo;

    @BeforeClass
    public static void setupRepos() throws IOException {
        testRepo = new SvnTestRepo("testSvnRepo");
    }

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        TestRepo.internalTearDown();
    }

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        evolveConfig = CONFIG_HELPER.addPipeline("evolve", STAGE_NAME, repository, "unit");
        CONFIG_HELPER.addPipeline("studios", "stageName", repository, "functional");
        goCache.clear();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        CONFIG_HELPER.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
    }

    @Test
    public void shouldNotRescheduleCancelledBuilds() throws SQLException {
        String agentId = "uuid";
        final Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, modifySomeFiles(evolveConfig), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), "md5-test", new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        buildAssignmentService.assignWorkToAgent(agent(new AgentConfig(agentId)));

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stageOf(pipeline));
            }
        });
        
        scheduleService.rescheduleHungJobs();

        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(buildOf(pipeline).getId());
        assertThat(reloaded.getState(), is(JobState.Completed));
        assertThat(reloaded.getResult(), is(JobResult.Cancelled));
    }

    @Test
    public void shouldRescheduleHungBuildWhenAgentTryToGetWorkWithSameUuid() throws Exception {
        AgentConfig agentConfig = AgentMother.localAgent();
        AgentInstance instance = agent(agentConfig);
        BuildCause buildCause = modifySomeFiles(evolveConfig);
        dbHelper.saveMaterials(buildCause.getMaterialRevisions());
        Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, buildCause, new DefaultSchedulingContext(DEFAULT_APPROVED_BY), "md5-test", new TimeProvider());
        buildAssignmentService.onTimer();

        Stage stage = pipeline.getFirstStage();
        JobInstance jobInstance = stage.getJobInstances().get(0);
        jobInstance.setAgentUuid(agentConfig.getUuid());
        jobInstance.changeState(JobState.Building);
        pipelineDao.saveWithStages(pipeline);

        buildAssignmentService.onTimer();
        buildAssignmentService.assignWorkToAgent(instance);
        buildAssignmentService.onTimer();
        buildAssignmentService.assignWorkToAgent(instance);

        final Stage reloadedStage = stageDao.stageById(stage.getId());
        final JobInstance rescheduledJob = reloadedStage.getJobInstances().getByName(jobInstance.getName());
        assertThat(rescheduledJob.getState(), is(JobState.Assigned));
    }

    private JobInstance buildOf(Pipeline pipeline) {
        return stageOf(pipeline).getJobInstances().first();
    }

    private Stage stageOf(Pipeline pipeline) {
        Stage stage = pipeline.getStages().first();
        for (JobInstance jobInstance : stage.getJobInstances()) {
            jobInstance.setIdentifier(new JobIdentifier(pipeline.getName(), -1, pipeline.getLabel(), stage.getName(),
                    String.valueOf(stage.getCounter()), jobInstance.getName()));
        }
        return stage;
    }

    private AgentInstance agent(AgentConfig agentConfig) {
        return AgentInstance.create(agentConfig, false, new SystemEnvironment());
    }
}
