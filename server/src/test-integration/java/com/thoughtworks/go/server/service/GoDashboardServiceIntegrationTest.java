/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dashboard.GoDashboardCurrentStateLoader;
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})

public class GoDashboardServiceIntegrationTest {
    static {
        new SystemEnvironment().setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, "false");
    }

    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private GoDashboardService goDashboardService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoDashboardCurrentStateLoader currentStateLoader;
    @Autowired
    private PipelineConfigService pipelineConfigService;

    private GoConfigFileHelper configHelper;
    private ScheduleTestUtil u;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        currentStateLoader.reset();
    }

    @After
    public void tearDown() throws Exception {
        currentStateLoader.reset();
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldShowTheLatestStatusOfPipelineInstanceIfAFullConfigSaveIsPerformed() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1", "g_2");
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));
        String p1_1 = u.runAndPass(p1, "g_1");

        goDashboardService.updateCacheForAllPipelinesIn(goConfigService.cruiseConfig());
        List<GoDashboardPipelineGroup> pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance()
                .getCounter(), Matchers.is(new StageIdentifier(p1_1).getPipelineCounter()));

        BuildCause buildCauseForThirdRun = BuildCause.createWithModifications(u.mrs(u.mr(u.m(g1).material, true, "g_2")), "user");
        Pipeline p1_2 = scheduleService.schedulePipeline(p1.config.name().toString(), buildCauseForThirdRun);
        goDashboardService.updateCacheForPipeline(p1.config);

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance().getId(), Matchers.is(p1_2.getId()));

        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("environment")));
        goDashboardService.updateCacheForAllPipelinesIn(goConfigService.cruiseConfig());

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance().getId(), Matchers.is(p1_2.getId()));
    }

    @Test
    public void shouldNotReturnDeletedPipelineAsPartOfDashboard() throws Exception {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1", "g_2");
        ScheduleTestUtil.AddedPipeline addedPipeline = u.saveConfigWith(UUID.randomUUID().toString(), u.m(g1));
        String p1_1 = u.runAndPass(addedPipeline, "g_1");

        goDashboardService.updateCacheForAllPipelinesIn(goConfigService.cruiseConfig());
        List<GoDashboardPipelineGroup> pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance()
                .getCounter(), Matchers.is(new StageIdentifier(p1_1).getPipelineCounter()));

        BuildCause buildCauseForThirdRun = BuildCause.createWithModifications(u.mrs(u.mr(u.m(g1).material, true, "g_2")), "user");
        Pipeline p1_2 = scheduleService.schedulePipeline(addedPipeline.config.name().toString(), buildCauseForThirdRun);
        goDashboardService.updateCacheForPipeline(addedPipeline.config);

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance().getId(), Matchers.is(p1_2.getId()));

        pipelineConfigService.deletePipelineConfig(new Username("user"), addedPipeline.config, new DefaultLocalizedOperationResult());
        goDashboardService.updateCacheForPipeline(addedPipeline.config);

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(0));

        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("environment")));
        goDashboardService.updateCacheForAllPipelinesIn(goConfigService.cruiseConfig());

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(PipelineSelections.ALL, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(0));
    }
}
