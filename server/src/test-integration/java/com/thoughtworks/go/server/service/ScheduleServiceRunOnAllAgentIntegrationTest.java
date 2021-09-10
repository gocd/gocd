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
package com.thoughtworks.go.server.service;

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
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ScheduleServiceRunOnAllAgentIntegrationTest {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private PipelineScheduler buildCauseProducer;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired
    private AgentAssignment agentAssignment;
    @Autowired
    private GoCache goCache;
    @Autowired
    private DependencyMaterialUpdateNotifier notifier;

    @Autowired
    private DatabaseAccessHelper dbHelper;
    private GoConfigFileHelper CONFIG_HELPER;
    public Subversion repository;
    public static TestRepo testRepo;

    @BeforeAll
    public static void setupRepos(@TempDir Path tempDir) throws IOException {
        testRepo = new SvnTestRepo(tempDir);
    }

    @BeforeEach
    public void setup() throws Exception {
        CONFIG_HELPER = new GoConfigFileHelper();
        dbHelper.onSetUp();
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        goConfigService.forceNotifyListeners();
        agentAssignment.clear();
        goCache.clear();

        CONFIG_HELPER.addPipeline("blahPipeline", "blahStage", MaterialConfigsMother.hgMaterialConfig(UUID.randomUUID().toString()), "job1", "job2");
        CONFIG_HELPER.makeJobRunOnAllAgents("blahPipeline", "blahStage", "job2");
        notifier.disableUpdates();

    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        notifier.enableUpdates();
        FileUtils.deleteQuietly(goConfigService.artifactsDir());
        pipelineScheduleQueue.clear();
        agentAssignment.clear();
        CONFIG_HELPER.onTearDown();
    }

    @Test
    public void shouldUpdateServerHealthWhenScheduleStageFails() {
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
    public void shouldUpdateServerHealthWhenSchedulePipelineFails() {
        pipelineScheduleQueue.schedule(new CaseInsensitiveString("blahPipeline"), saveMaterials(modifySomeFiles(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("blahPipeline")))));
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
        final HashMap<String, String> revisions = new HashMap<>();
        final HashMap<String, String> environmentVariables = new HashMap<>();
        buildCauseProducer.manualProduceBuildCauseAndSave(pipelineName, new Username(new CaseInsensitiveString("some user name")),
                new ScheduleOptions(revisions, environmentVariables, new HashMap<>()), new ServerHealthStateOperationResult());
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        return pipelineService.mostRecentFullPipelineByName(pipelineName);
    }

}
