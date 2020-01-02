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

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageSqlMapDao;
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.Filters;
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

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
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
    @Autowired
    private GoDashboardConfigChangeHandler goDashboardConfigChangeHandler;
    @Autowired
    private StageSqlMapDao stageSqlMapDao;
    @Autowired
    private GoDashboardStageStatusChangeHandler goDashboardStageStatusChangeHandler;

    private GoConfigFileHelper configHelper;
    private ScheduleTestUtil u;
    private Username user = new Username("user");

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
        List<GoDashboardPipelineGroup> pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance()
                .getCounter(), Matchers.is(new StageIdentifier(p1_1).getPipelineCounter()));

        BuildCause buildCauseForThirdRun = BuildCause.createWithModifications(u.mrs(u.mr(u.m(g1).material, true, "g_2")), "user");
        Pipeline p1_2 = scheduleService.schedulePipeline(p1.config.name(), buildCauseForThirdRun);
        goDashboardService.updateCacheForPipeline(p1.config);

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance().getId(), Matchers.is(p1_2.getId()));

        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("environment")));
        goDashboardService.updateCacheForAllPipelinesIn(goConfigService.cruiseConfig());

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
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
        List<GoDashboardPipelineGroup> pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance()
                .getCounter(), Matchers.is(new StageIdentifier(p1_1).getPipelineCounter()));

        BuildCause buildCauseForThirdRun = BuildCause.createWithModifications(u.mrs(u.mr(u.m(g1).material, true, "g_2")), "user");
        Pipeline p1_2 = scheduleService.schedulePipeline(addedPipeline.config.name(), buildCauseForThirdRun);
        goDashboardService.updateCacheForPipeline(addedPipeline.config);

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().size(), is(1));
        assertThat(pipelineGroupsOnDashboard.get(0).allPipelines().iterator().next().model().getLatestPipelineInstance().getId(), Matchers.is(p1_2.getId()));

        pipelineConfigService.deletePipelineConfig(new Username("user"), addedPipeline.config, new DefaultLocalizedOperationResult());
        goDashboardService.updateCacheForPipeline(addedPipeline.config);

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(0));

        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("environment")));
        goDashboardService.updateCacheForAllPipelinesIn(goConfigService.cruiseConfig());

        pipelineGroupsOnDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user"));
        assertThat(pipelineGroupsOnDashboard, hasSize(0));
    }

    @Test
    public void ensureDashboardReturnsJustOneInstanceOfPipelineAddedViaAPiConsistentlyImmaterialOfDifferentFlowsWhichCauseTheBackendCacheToGetUpdated() {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1, "g_1");
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));
        String p1Counter1 = u.runAndPass(p1, "g_1");

        goDashboardConfigChangeHandler.call(goConfigService.cruiseConfig());

        List<GoDashboardPipelineGroup> originalDashboard = goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, user);
        assertThat(originalDashboard, hasSize(1));
        assertThat(originalDashboard.get(0).allPipelines(), hasSize(1));
        assertThat(originalDashboard.get(0).allPipelines().iterator().next().model().getActivePipelineInstances(), hasSize(1));

        PipelineConfig newPipeline = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), g1.config());
        pipelineConfigService.createPipelineConfig(user, newPipeline, new DefaultLocalizedOperationResult(), goConfigService.cruiseConfig().getGroups().first().getGroup());
        goDashboardConfigChangeHandler.call(goConfigService.cruiseConfig().pipelineConfigByName(newPipeline.name()));

        assertThereIsExactlyOneInstanceOfEachPipelineOnDashboard(goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, user));

        String newPipelineCounter1 = u.runAndPass(new ScheduleTestUtil.AddedPipeline(newPipeline, new DependencyMaterial(newPipeline.name(), newPipeline.first().name())), "g_1");
        goDashboardStageStatusChangeHandler.call(stageSqlMapDao.mostRecentPassed(newPipeline.name().toString(), newPipeline.first().name().toString()));

        assertThereIsExactlyOneInstanceOfEachPipelineOnDashboard(goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, user));

        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("new-environment")));
        goDashboardConfigChangeHandler.call(goConfigService.cruiseConfig());

        assertThereIsExactlyOneInstanceOfEachPipelineOnDashboard(goDashboardService.allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, user));
    }

    private void assertThereIsExactlyOneInstanceOfEachPipelineOnDashboard(List<GoDashboardPipelineGroup> dashboardAfterPipelineCreationViaApi) {
        assertThat(dashboardAfterPipelineCreationViaApi, hasSize(1));//pipeline group
        assertThat(dashboardAfterPipelineCreationViaApi.get(0).allPipelines(), hasSize(goConfigService.cruiseConfig().getAllPipelineNames().size()));
        Iterator<GoDashboardPipeline> iterator = dashboardAfterPipelineCreationViaApi.get(0).allPipelines().iterator();
        while (iterator.hasNext()){
            GoDashboardPipeline dashboardPipeline = iterator.next();
            assertThat(dashboardPipeline.model().getName(), dashboardPipeline.model().getActivePipelineInstances(), hasSize(1));
        }
    }

}
