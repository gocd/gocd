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

import java.sql.SQLException;
import java.util.Collection;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.fixture.TwoPipelineGroups;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.PipelineJsonPresentationModel;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class JsonCurrentActivityServiceIntegrationTest {
	@Autowired private DatabaseAccessHelper dbHelper;
    private PipelineWithTwoStages fixture;

    @Autowired private JsonCurrentActivityService service;
    @Autowired private CachedCurrentActivityService currentActivityService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private StageStatusCache stageStatusCache;
    @Autowired private JobStatusCache jobStatusCache;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        fixture.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldReturnLatestStageForEachStageInThePipeline() {
        Pipeline pipeline = createdPipelineWithAllStagesPassed();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat(pipelineActivity.getLatestStage(0).getPipelineLabel(), is(pipeline.getLabel()));
        assertThat(pipelineActivity.getLatestStage(1).getPipelineLabel(), is(pipeline.getLabel()));
    }

    @Test
    public void shouldReturnStatusForEachStage() throws SQLException {
        createdPipelineWithAllStagesPassed();
        createPipelineWithFirstStageScheduled();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat(pipelineActivity.getLatestStage(0).currentStatus(), is("building"));
        assertThat(pipelineActivity.getLatestStage(1).currentStatus(), is("passed"));
    }

    @Test
    public void shouldReturnUniqueStageId() throws SQLException {
        Pipeline pipeline = createdPipelineWithAllStagesPassed();
        fixture.createPipelineWithFirstStageScheduled();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat(pipelineActivity.getLatestStage(0).uniqueStageId(),
                containsString(fixture.pipelineName + "-" + fixture.devStage + "-"));
        assertThat(pipelineActivity.getLatestStage(1).uniqueStageId(),
                containsString(fixture.pipelineName + "-" + fixture.ftStage + "-"));
    }

    @Test
    public void shouldNotBeAbleToRunStageIfAnyStageIsRunningInThatPipeline() throws SQLException {
        Pipeline pipeline = createdPipelineWithAllStagesPassed();
        fixture.createPipelineWithFirstStageScheduled();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat(pipelineActivity.getLatestStage(0).getCanRun(), is(false));
        assertThat(pipelineActivity.getLatestStage(1).getCanRun(), is(true));
    }

    @Test
    public void shouldNotBeAbleToCancelStageIfUserDoesNotHaveOperatePermission() throws Exception {
        configHelper.addSecurityWithAdminConfig();
        configHelper.setOperatePermissionForGroup("defaultGroup", "operate_user");
        configHelper.setViewPermissionForGroup("defaultGroup", "view_user");
        fixture.createPipelineWithFirstStageScheduled();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity("view_user");
        assertThat(pipelineActivity.getLatestStage(0).getCanCancel(), is(false));
    }

    @Test
    public void shouldBeAbleToCancelStageIfUserHasOperatePermission() throws Exception {
        configHelper.addSecurityWithAdminConfig();
        String defaultUser = CaseInsensitiveString.str(UserHelper.getUserName().getUsername());
        configHelper.setOperatePermissionForGroup("defaultGroup", defaultUser);
        configHelper.setViewPermissionForGroup("defaultGroup", defaultUser);
        fixture.createPipelineWithFirstStageScheduled();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity(defaultUser);
        assertThat(pipelineActivity.getLatestStage(0).getCanCancel(), is(true));
    }

    @Test
    public void shouldBeAbleToForcePipelineIfHasPermission() throws SQLException {
        Pipeline pipeline = createdPipelineWithAllStagesPassed();
        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat("Has permission to force the build", pipelineActivity.getCanForce(), is(true));
    }

    @Test
    public void shouldBeAbleToPausePipelineIfHasPermission() throws SQLException {
        createdPipelineWithAllStagesPassed();
        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat("Has permission to pause the pipeline", pipelineActivity.getCanPause(), is(true));
    }

    @Test
    public void shouldThrowExceptionWhenPipelineWithGivingNameCannotBeFound() throws Exception {
        TwoPipelineGroups twoPipelineGroups = new TwoPipelineGroups(configHelper);
        twoPipelineGroups.onSetUp();
        try {
            service.pipelinesActivity("user", "pipeline-Not-Exist");
            fail("should throw Exception");
        } catch (Exception e) {
            //pass
        } finally {
            twoPipelineGroups.onTearDown();
        }
    }

    @Test
    public void shouldGetActivityForAllPipelines() {
        Pipeline firstPipeline = createdPipelineWithAllStagesPassed();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat(pipelineActivity.getLatestStage(0).getPipelineLabel(), is(firstPipeline.getLabel()));

        Pipeline changedPipeline = createdPipelineWithAllStagesPassed();

        PipelineJsonPresentationModel pipelineActivity2 = pipelineActivity();
        assertThat(pipelineActivity2.getLatestStage(0).getPipelineLabel(), is(changedPipeline.getLabel()));
    }

    @Test
    public void shouldGetActivityForASinglePipeline() {
        Pipeline firstPipeline = createdPipelineWithAllStagesPassed();

        PipelineJsonPresentationModel pipelineActivity = pipelineActivity();
        assertThat(pipelineActivity.getLatestStage(0).getPipelineLabel(), is(firstPipeline.getLabel()));

        Pipeline changedPipeline = createdPipelineWithAllStagesPassed();

        PipelineJsonPresentationModel pipelineActivity2 = pipelineActivity();
        assertThat(pipelineActivity2.getLatestStage(0).getPipelineLabel(), is(changedPipeline.getLabel()));
    }

    private Pipeline createPipelineWithFirstStageScheduled() throws SQLException {
        Pipeline pipeline = fixture.createPipelineWithFirstStageScheduled();
        notifyListeners(pipeline);
        return pipeline;
    }

    private Pipeline createdPipelineWithAllStagesPassed() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        notifyListeners(pipeline);
        return pipeline;
    }

    private void notifyListeners(Pipeline pipeline) {
        for (Stage stage : pipeline.getStages()) {
            stageStatusCache.stageStatusChanged(stage);
            for (JobInstance job : stage.getJobInstances()) {
                jobStatusCache.jobStatusChanged(job);
            }
        }
    }

    private PipelineJsonPresentationModel pipelineActivity() {
        return pipelineActivity("user");
    }

    private PipelineJsonPresentationModel pipelineActivity(String username) {
        Collection<PipelineJsonPresentationModel> models = service.pipelinesActivity(username, fixture.pipelineName);
        assertThat(models.size(), is(1));
        PipelineJsonPresentationModel pipelineActivity = models.iterator().next();
        assertThat(pipelineActivity.getName(), is(fixture.pipelineName));
        assertThat(pipelineActivity.getStageCount(), is(2));
        return pipelineActivity;
    }
}
