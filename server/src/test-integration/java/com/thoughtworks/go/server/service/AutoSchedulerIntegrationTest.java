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
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.fixture.ArtifactsDiskIsFull;
import com.thoughtworks.go.fixture.ConfigWithFreeEditionLicense;
import com.thoughtworks.go.fixture.PreCondition;
import com.thoughtworks.go.fixture.TwoPipelineGroups;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.serverhealth.ServerHealthMatcher.containsState;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class AutoSchedulerIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineScheduler pipelineScheduler;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired private GoConfigService configService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PreCondition configWithFreeEditionLicense;
    private TwoPipelineGroups twoPipelineGroups;

    @Before
    public void setUp() throws Exception {
        GoConfigFileHelper configFileHelper = new GoConfigFileHelper().usingCruiseConfigDao(goConfigDao);

        dbHelper.onSetUp();
        configFileHelper.onSetUp();

        configFileHelper.usingCruiseConfigDao(goConfigDao);

        configWithFreeEditionLicense = new ConfigWithFreeEditionLicense(configFileHelper);
        configWithFreeEditionLicense.onSetUp();
        configService.forceNotifyListeners();

        twoPipelineGroups = new TwoPipelineGroups(configFileHelper, temporaryFolder);
        twoPipelineGroups.onSetUp();
        serverHealthService.removeAllLogs();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        twoPipelineGroups.onTearDown();
        configWithFreeEditionLicense.onTearDown();
    }

    @Test
    public void shouldProduceBuildCauseForFirstGroupPipeline() throws Exception {
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(CaseInsensitiveString.str(twoPipelineGroups.pipelineInFirstGroup()));
        assertThat(pipelineScheduleQueue.hasBuildCause(twoPipelineGroups.pipelineInFirstGroup()), is(true));
    }

    @Test
    public void shouldNotProduceBuildCauseForNonFirstGroupPipelineWhenUsingNonEnterpriseEdition() throws Exception {
        scheduleHelper.autoSchedulePipelinesWithRealMaterials();
        assertThat("shouldNotProduceBuildCauseForNonFirstGroupPipelineUsingNonEnterpriseEdition",
                pipelineScheduleQueue.hasBuildCause(twoPipelineGroups.pipelineInSecondGroup()), is(false));
    }

    @Test
    public void shouldLogErrorIntoServerHealthServiceWhenArtifactsDiskSpaceIsFull() throws Exception {
        serverHealthService.removeAllLogs();
        ArtifactsDiskIsFull full = new ArtifactsDiskIsFull();
        full.onSetUp();
        if (!configService.artifactsDir().exists()) {
            configService.artifactsDir().createNewFile();
        }
        try {
            pipelineScheduler.onTimer();
            HealthStateType healthStateType = HealthStateType.artifactsDiskFull();
            assertThat(serverHealthService, containsState(healthStateType, HealthStateLevel.ERROR, "GoCD Server has run out of artifacts disk space. Scheduling has been stopped"));
        } finally {
            full.onTearDown();
            configService.artifactsDir().delete();
        }
    }

}
