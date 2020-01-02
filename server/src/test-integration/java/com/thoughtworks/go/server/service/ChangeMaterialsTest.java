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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.exception.StageAlreadyBuildingException;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.StubScheduleCheckCompletedListener;
import com.thoughtworks.go.server.scheduling.ScheduleCheckCompletedTopic;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ChangeMaterialsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private ScheduleCheckCompletedTopic topic;
    @Autowired
    private ScheduleHelper scheduleHelper;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    private GoConfigFileHelper cruiseConfig;

    private PipelineConfig mingle;
    private static final String DEV_STAGE = "dev";
    private static final String FT_STAGE = "ft";
    private Pipeline pipeline;
    private HgTestRepo hgTestRepo;
    private static P4TestRepo p4TestRepo;
    private static final String PIPELINE_NAME = "mingle";
    private Username username;
    private StubScheduleCheckCompletedListener listener;
    @ClassRule
    public static final TemporaryFolder classTemporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void startP4Server() {
        try {
            p4TestRepo = P4TestRepo.createP4TestRepo(classTemporaryFolder, classTemporaryFolder.newFolder());
            p4TestRepo.onSetup();
        } catch (Exception e) {
            bomb(e);
        }
    }

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        p4TestRepo.onTearDown();
    }

    @Before
    public void setUp() throws Exception {
        username = new Username(new CaseInsensitiveString("gli"));

        dbHelper.onSetUp();
        cruiseConfig = new GoConfigFileHelper().usingCruiseConfigDao(goConfigDao);
        cruiseConfig.onSetUp();
        cruiseConfig.initializeConfigFile();

        hgTestRepo = new HgTestRepo(temporaryFolder);

        SvnTestRepo svnRepo = new SvnTestRepo(temporaryFolder);
        cruiseConfig.addPipeline(PIPELINE_NAME, DEV_STAGE, svnRepo.materialConfig(), "foo");
        mingle = cruiseConfig.addStageToPipeline(PIPELINE_NAME, FT_STAGE, "bar");
        pipeline = dbHelper.newPipelineWithAllStagesPassed(mingle);
        listener = new StubScheduleCheckCompletedListener();
        topic.addListener(listener);
    }

    @After
    public void teardown() throws Exception {
        cruiseConfig.initializeConfigFile();
        dbHelper.onTearDown();
        TestRepo.internalTearDown();
        cruiseConfig.onTearDown();
    }

    @Test
    public void shouldAutoScheduleWithLatestModificationFromNewMaterialAfterChangedMaterial() throws Exception {
        cruiseConfig.replaceMaterialWithHgRepoForPipeline(PIPELINE_NAME, hgTestRepo.projectRepositoryUrl());

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(PIPELINE_NAME);
        assertThat("Should schedule new instance after changed material", mostRecent.getId(),
                is(not(pipeline.getId())));
        MaterialRevisions materialRevisions = mostRecent.getBuildCause().getMaterialRevisions();

        assertThat(materialRevisions.totalNumberOfModifications(), is(hgTestRepo.latestModifications().size()));
    }

    //TODO: CS&DY Revisit this test to use materials properly
    @Test
    public void shouldManualScheduleWithLatestModificationFromNewMaterialAfterChangedMaterial() throws Exception, StageAlreadyBuildingException {
        cruiseConfig.replaceMaterialWithHgRepoForPipeline(PIPELINE_NAME, hgTestRepo.projectRepositoryUrl());

        scheduleHelper.manuallySchedulePipelineWithRealMaterials(PIPELINE_NAME, username);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(PIPELINE_NAME);

        assertThat("Should schedule new instance after changed material", mostRecent.getId(), is(not(pipeline.getId())));
        MaterialRevisions materialRevisions = mostRecent.getBuildCause().getMaterialRevisions();
        assertEquals(hgTestRepo.latestModifications().get(0).getModifiedTime(), materialRevisions.getDateOfLatestModification());
    }

    @Test
    public void p4MaterialFromConfigShouldBeEqualWithP4MaterialFromDb() throws Exception {
        String p4view = "//depot/... //localhost/...";
        cruiseConfig.replaceMaterialConfigForPipeline(PIPELINE_NAME, p4TestRepo.materialConfig(p4view));
        mingle = goConfigDao.load().pipelineConfigByName(new CaseInsensitiveString(PIPELINE_NAME));

        assertThat(mingle.materialConfigs().get(0), is(instanceOf(P4MaterialConfig.class)));

        scheduleHelper.manuallySchedulePipelineWithRealMaterials(PIPELINE_NAME, username);

        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(PIPELINE_NAME);
        assertThat(mostRecent.getMaterials().first(), is(new MaterialConfigConverter().toMaterial(mingle.materialConfigs().first())));
    }

}
