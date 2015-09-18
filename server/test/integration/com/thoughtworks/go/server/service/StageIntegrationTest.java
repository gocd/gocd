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
import javax.sql.DataSource;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.exception.StageAlreadyBuildingException;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.server.dao.DatabaseAccessHelper.AGENT_UUID;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class StageIntegrationTest {
    @Autowired private BuildRepositoryService buildRepositoryService;
    @Autowired private StageService stageService;
    @Autowired private PipelineService pipelineService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DataSource dataSource;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private ScheduleHelper scheduleHelper;

    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private PipelineConfig mingle;
    private static final String DEV_STAGE = "dev";
    private static final String FT_STAGE = "ft";
    private static final String PIPELINE_NAME = "mingle";
    public Subversion svnRepo;
    private static final String HOSTNAME = "10.18.0.1";

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        TestRepo.internalTearDown();
    }

    @Before
    public void setUp() throws Exception {

        dbHelper.onSetUp();
        CONFIG_HELPER.onSetUp();
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.initializeConfigFile();

        TestRepo svnTestRepo = new SvnTestRepo("testsvnrepo");

        svnRepo = new SvnCommand(null, svnTestRepo.projectRepositoryUrl());
        CONFIG_HELPER.addPipeline(PIPELINE_NAME, DEV_STAGE, svnRepo, "foo");
        mingle = CONFIG_HELPER.addStageToPipeline(PIPELINE_NAME, FT_STAGE, "bar");
        CONFIG_HELPER.addAgent(HOSTNAME, AGENT_UUID);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSetStageOrder() throws Exception {
        createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);

        Stages stages = pipelineService.mostRecentFullPipelineByName(PIPELINE_NAME).getStages();
        assertThat("ft stage should come after dev stage", stages.byName(FT_STAGE).getOrderId() > stages.byName(DEV_STAGE).getOrderId(), is(true));
    }

    private Stage createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState stageState) throws Exception {
        Pipeline newPipeline = createPipelineWithFirstStageBuilding();
        Stage mostRecent = newPipeline.getFirstStage();
        JobInstance job = mostRecent.getJobInstances().first();
        if (stageState.equals(StageState.Failed)) {
            dbHelper.failStage(mostRecent);
        } else {
            dbHelper.passStage(mostRecent);
        }
        buildRepositoryService.updateStatusFromAgent(getBuildIdentifier(job.getId()), JobState.Completed,
                AGENT_UUID);
        Stage nextStage = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        dbHelper.buildingBuildInstance(nextStage);
        return nextStage;
    }

    public JobIdentifier getBuildIdentifier(long id) {
        return new JobIdentifier("", -2, "", "", "1", "", id);
    }

    private Pipeline createPipelineWithFirstStageBuilding() throws StageAlreadyBuildingException {
        Pipeline scheduledPipeline = schedulePipeline();
        dbHelper.saveBuildingStage(scheduledPipeline.getFirstStage());

        return pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(mingle.name()));
    }

    private Pipeline schedulePipeline() {
        return scheduleHelper.schedule(mingle, modifySomeFiles(mingle), DEFAULT_APPROVED_BY);
    }

}



