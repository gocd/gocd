/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class AutoBuildIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineService pipelineService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private GoConfigService goConfigService;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private MaterialChecker materialChecker;
    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private DependencyMaterialUpdateNotifier notifier;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ScheduleTestUtil scheduleUtil;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        scheduleUtil = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        notifier.disableUpdates();
    }

    @After
    public void teardown() throws Exception {
        notifier.enableUpdates();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldCreateBuildCauseOnMaterialConfigChange() throws Exception {
        //down_pipe <- svn
        //         ^   /
        //         | /          => down_pipe <- up_pipe <- svn
        //      up_pipe
        //

        SvnMaterial svn = scheduleUtil.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1"};
        scheduleUtil.checkinInOrder(svn, svn_revs);

        ScheduleTestUtil.AddedPipeline up_pipe = scheduleUtil.saveConfigWith("up_pipe", scheduleUtil.m(svn));
        ScheduleTestUtil.AddedPipeline down_pipe = scheduleUtil.saveConfigWith("down_pipe", scheduleUtil.m(svn), scheduleUtil.m(up_pipe));

        String up_pipe_1 = scheduleUtil.runAndPass(up_pipe, "s1");
        pipelineTimeline.update();

        String down_pipe_1 = scheduleUtil.runAndPass(down_pipe, "s1", up_pipe_1);
        pipelineTimeline.update();

        down_pipe.config.removeMaterialConfig(svn.config());

        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        currentConfig.pipelineConfigByName(new CaseInsensitiveString("down_pipe")).removeMaterialConfig(svn.config());
        configHelper.writeConfigFile(currentConfig);
        goConfigDao.load();

        MaterialRevisions given = scheduleUtil.mrs(scheduleUtil.mr(up_pipe, true, up_pipe_1));

        MaterialRevisions expected = scheduleUtil.mrs(scheduleUtil.mr(up_pipe, true, up_pipe_1));

        AutoBuild autoBuildType = new AutoBuild(goConfigService, pipelineService, "down_pipe", systemEnvironment, materialChecker);
        assertThat(autoBuildType.onModifications(given, true, null).getMaterialRevisions(), is(expected));
    }
}
