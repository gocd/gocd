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
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.CannotScheduleException;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ScheduleServiceRunOnAllAgentIntegrationTest {
    @Autowired private GoConfigService goConfigService;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineScheduler buildCauseProducer;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private GoCache goCache;

    @Autowired private DatabaseAccessHelper dbHelper;
    private GoConfigFileHelper CONFIG_HELPER;
    public Subversion repository;
    public static TestRepo testRepo;

    @BeforeClass
    public static void setupRepos() throws IOException {
        testRepo = new SvnTestRepo("testSvnRepo");
    }

    @AfterClass
    public static void tearDownConfigFileLocation() {
        TestRepo.internalTearDown();
    }

    @Before
    public void setup() throws Exception {
        CONFIG_HELPER = new GoConfigFileHelper();
        dbHelper.onSetUp();
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        CONFIG_HELPER.onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        goConfigService.forceNotifyListeners();
        agentAssignment.clear();
        goCache.clear();

        CONFIG_HELPER.addPipeline("blahPipeline", "blahStage", MaterialConfigsMother.hgMaterialConfig("file:///home/cruise/projects/cruisen/manual-testing/ant_hg/dummy"), "job1", "job2");
        CONFIG_HELPER.makeJobRunOnAllAgents("blahPipeline", "blahStage", "job2");

    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        pipelineScheduleQueue.clear();
        agentAssignment.clear();
        CONFIG_HELPER.onTearDown();
    }

    @Test
    public void shouldUpdateServerHealthWhenScheduleStageFails() throws Exception {
        try {
            scheduleService.scheduleStage(manualSchedule("blahPipeline"), "blahStage", "blahUser", new ScheduleService.NewStageInstanceCreator(goConfigService),
                    new ScheduleService.ExceptioningErrorHandler());
            fail("should throw CannotScheduleException");
        } catch (CannotScheduleException e) {

        }
        List<ServerHealthState> stateList = serverHealthService.filterByScope(HealthStateScope.forStage("blahPipeline", "blahStage"));
        assertThat(stateList.size(), is(1));
        assertThat(stateList.get(0).getMessage(), is("Failed to trigger stage [blahStage] pipeline [blahPipeline]"));
        assertThat(stateList.get(0).getDescription(), is("Could not find matching agents to run job [job2] of stage [blahStage]."));
    }

    @Test
    public void shouldUpdateServerHealthWhenSchedulePipelineFails() throws Exception {
        pipelineScheduleQueue.schedule("blahPipeline", saveMaterials(modifySomeFiles(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("blahPipeline")))));
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        List<ServerHealthState> stateList = serverHealthService.filterByScope(HealthStateScope.forStage("blahPipeline", "blahStage"));
        assertThat(stateList.size(), is(1));
        assertThat(stateList.get(0).getMessage(), is("Failed to trigger stage [blahStage] pipeline [blahPipeline]"));
        assertThat(stateList.get(0).getDescription(), is("Could not find matching agents to run job [job2] of stage [blahStage]."));
    }

    private BuildCause saveMaterials(BuildCause buildCause) {
        dbHelper.saveMaterials(buildCause.getMaterialRevisions());
        return buildCause;
    }

    private Pipeline manualSchedule(String pipelineName) {
        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(pipelineName, new Username(new CaseInsensitiveString("some user name")),
                new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        return pipelineService.mostRecentFullPipelineByName(pipelineName);
    }

}