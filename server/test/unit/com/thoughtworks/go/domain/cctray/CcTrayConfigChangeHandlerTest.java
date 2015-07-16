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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.domain.cctray.viewers.AllowedViewers;
import com.thoughtworks.go.domain.cctray.viewers.Viewers;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.DataStructureUtils.s;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CcTrayConfigChangeHandlerTest {
    @Mock
    private CcTrayCache cache;
    @Mock
    private CcTrayStageStatusLoader stageStatusLoader;
    @Mock
    private CcTrayViewAuthority ccTrayViewAuthority;
    @Captor
    ArgumentCaptor<List<ProjectStatus>> statusesCaptor;

    private GoConfigMother goConfigMother;
    private CcTrayConfigChangeHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        goConfigMother = new GoConfigMother();
        handler = new CcTrayConfigChangeHandler(cache, stageStatusLoader, ccTrayViewAuthority);
    }

    @Test
    public void shouldProvideCCTrayCacheWithAListOfAllProjectsInOrder() throws Exception {
        ProjectStatus pipeline1_stage1 = new ProjectStatus("pipeline1 :: stage", "Activity1", "Status1", "Label1", new Date(), "stage1-url");
        ProjectStatus pipeline1_stage1_job = new ProjectStatus("pipeline1 :: stage :: job", "Activity1-Job", "Status1-Job", "Label1-Job", new Date(), "job1-url");
        ProjectStatus pipeline2_stage1 = new ProjectStatus("pipeline2 :: stage", "Activity2", "Status2", "Label2", new Date(), "stage2-url");
        ProjectStatus pipeline2_stage1_job = new ProjectStatus("pipeline2 :: stage :: job", "Activity2-Job", "Status2-Job", "Label2-Job", new Date(), "job2-url");

        when(cache.get("pipeline1 :: stage")).thenReturn(pipeline1_stage1);
        when(cache.get("pipeline1 :: stage :: job")).thenReturn(pipeline1_stage1_job);
        when(cache.get("pipeline2 :: stage")).thenReturn(pipeline2_stage1);
        when(cache.get("pipeline2 :: stage :: job")).thenReturn(pipeline2_stage1_job);

        handler.call(GoConfigMother.configWithPipelines("pipeline2", "pipeline1")); /* Adds pipeline1 first in config. Then pipeline2. */

        verify(cache).replaceAllEntriesInCacheWith(eq(asList(pipeline1_stage1, pipeline1_stage1_job, pipeline2_stage1, pipeline2_stage1_job)));
        verifyZeroInteractions(stageStatusLoader);
    }

    @Test
    public void shouldPopulateNewCacheWithProjectsFromOldCacheWhenTheyExist() throws Exception {
        String stageProjectName = "pipeline1 :: stage";
        String jobProjectName = "pipeline1 :: stage :: job";

        ProjectStatus existingStageStatus = new ProjectStatus(stageProjectName, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        when(cache.get(stageProjectName)).thenReturn(existingStageStatus);

        ProjectStatus existingJobStatus = new ProjectStatus(jobProjectName, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job-url");
        when(cache.get(jobProjectName)).thenReturn(existingJobStatus);


        handler.call(GoConfigMother.configWithPipelines("pipeline1"));


        verify(cache).replaceAllEntriesInCacheWith(eq(asList(existingStageStatus, existingJobStatus)));
        verifyZeroInteractions(stageStatusLoader);
    }

    @Test
    public void shouldPopulateNewCacheWithStageAndJobFromDB_WhenAStageIsNotFoundInTheOldCache() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");

        String stageProjectName = "pipeline1 :: stage";
        String jobProjectName = "pipeline1 :: stage :: job";

        ProjectStatus statusOfStageInDB = new ProjectStatus(stageProjectName, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJobInDB = new ProjectStatus(jobProjectName, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job-url");
        when(cache.get(stageProjectName)).thenReturn(null);
        when(stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfigFor(config, "pipeline1"), stageConfigFor(config, "pipeline1", "stage")))
                .thenReturn(asList(statusOfStageInDB, statusOfJobInDB));

        handler.call(config);


        verify(cache).replaceAllEntriesInCacheWith(eq(asList(statusOfStageInDB, statusOfJobInDB)));
    }

    @Test
    public void shouldHandleNewStagesInConfig_ByReplacingStagesMissingInDBWithNullStagesAndJobs() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage1", "job1");
        goConfigMother.addStageToPipeline(config, "pipeline1", "stage2", "job2");

        String stage1ProjectName = "pipeline1 :: stage1";
        String job1ProjectName = "pipeline1 :: stage1 :: job1";
        String stage2ProjectName = "pipeline1 :: stage2";
        String job2ProjectName = "pipeline1 :: stage2 :: job2";

        ProjectStatus statusOfStage1InCache = new ProjectStatus(stage1ProjectName, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InCache = new ProjectStatus(job1ProjectName, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(stage1ProjectName)).thenReturn(statusOfStage1InCache);
        when(cache.get(job1ProjectName)).thenReturn(statusOfJob1InCache);

        when(cache.get(stage2ProjectName)).thenReturn(null);
        when(stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfigFor(config, "pipeline1"), stageConfigFor(config, "pipeline1", "stage2")))
                .thenReturn(Collections.<ProjectStatus>emptyList());


        handler.call(config);


        ProjectStatus expectedNullStatusForStage2 = new ProjectStatus.NullProjectStatus(stage2ProjectName);
        ProjectStatus expectedNullStatusForJob2 = new ProjectStatus.NullProjectStatus(job2ProjectName);
        verify(cache).replaceAllEntriesInCacheWith(eq(asList(statusOfStage1InCache, statusOfJob1InCache, expectedNullStatusForStage2, expectedNullStatusForJob2)));
    }

    /* Simulate adding a job, when server is down. DB does not know anything about that job. */
    @Test
    public void shouldHandleNewJobsInConfig_ByReplacingJobsMissingInDBWithNullJob() throws Exception {
        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage1", "job1", "NEW_JOB_IN_CONFIG");

        String stage1ProjectName = "pipeline1 :: stage1";
        String job1ProjectName = "pipeline1 :: stage1 :: job1";
        String projectNameOfNewJob = "pipeline1 :: stage1 :: NEW_JOB_IN_CONFIG";

        ProjectStatus statusOfStage1InDB = new ProjectStatus(stage1ProjectName, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InDB = new ProjectStatus(job1ProjectName, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(stage1ProjectName)).thenReturn(null);
        when(stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfigFor(config, "pipeline1"), stageConfigFor(config, "pipeline1", "stage1")))
                .thenReturn(asList(statusOfStage1InDB, statusOfJob1InDB));


        handler.call(config);


        ProjectStatus expectedNullStatusForNewJob = new ProjectStatus.NullProjectStatus(projectNameOfNewJob);
        verify(cache).replaceAllEntriesInCacheWith(eq(asList(statusOfStage1InDB, statusOfJob1InDB, expectedNullStatusForNewJob)));
    }

    /* Simulate adding a job, in a running system. Cache has the stage info, but not the job info. */
    @Test
    public void shouldHandleNewJobsInConfig_ByReplacingJobsMissingInConfigWithNullJob() throws Exception {
        String stage1ProjectName = "pipeline1 :: stage1";
        String job1ProjectName = "pipeline1 :: stage1 :: job1";
        String projectNameOfNewJob = "pipeline1 :: stage1 :: NEW_JOB_IN_CONFIG";

        ProjectStatus statusOfStage1InCache = new ProjectStatus(stage1ProjectName, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InCache = new ProjectStatus(job1ProjectName, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(stage1ProjectName)).thenReturn(statusOfStage1InCache);
        when(cache.get(job1ProjectName)).thenReturn(statusOfJob1InCache);
        when(cache.get(projectNameOfNewJob)).thenReturn(null);

        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage1", "job1", "NEW_JOB_IN_CONFIG");


        handler.call(config);


        ProjectStatus expectedNullStatusForNewJob = new ProjectStatus.NullProjectStatus(projectNameOfNewJob);
        verify(cache).replaceAllEntriesInCacheWith(eq(asList(statusOfStage1InCache, statusOfJob1InCache, expectedNullStatusForNewJob)));
        verifyZeroInteractions(stageStatusLoader);
    }

    @Test
    public void shouldRemoveExtraJobsFromCache_WhichAreNoLongerInConfig() throws Exception {
        String stage1ProjectName = "pipeline1 :: stage1";
        String job1ProjectName = "pipeline1 :: stage1 :: job1";
        String projectNameOfJobWhichWillBeRemoved = "pipeline1 :: stage1 :: JOB_IN_OLD_CONFIG";

        ProjectStatus statusOfStage1InCache = new ProjectStatus(stage1ProjectName, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InCache = new ProjectStatus(job1ProjectName, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        ProjectStatus statusOfOldJobInCache = new ProjectStatus(projectNameOfJobWhichWillBeRemoved, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job2-url");
        when(cache.get(stage1ProjectName)).thenReturn(statusOfStage1InCache);
        when(cache.get(job1ProjectName)).thenReturn(statusOfJob1InCache);
        when(cache.get(projectNameOfJobWhichWillBeRemoved)).thenReturn(statusOfOldJobInCache);

        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage1", "job1");


        handler.call(config);


        verify(cache).replaceAllEntriesInCacheWith(eq(asList(statusOfStage1InCache, statusOfJob1InCache)));
        verifyZeroInteractions(stageStatusLoader);
    }

    @Test
    public void shouldUpdateViewPermissionsForEveryProjectBasedOnViewPermissionsOfTheGroup() throws Exception {
        ProjectStatus pipeline1_stage1 = new ProjectStatus("pipeline1 :: stage", "Activity1", "Status1", "Label1", new Date(), "stage1-url");
        ProjectStatus pipeline1_stage1_job = new ProjectStatus("pipeline1 :: stage :: job", "Activity1-Job", "Status1-Job", "Label1-Job", new Date(), "job1-url");
        ProjectStatus pipeline2_stage2 = new ProjectStatus("pipeline2 :: stage", "Activity2", "Status2", "Label2", new Date(), "stage2-url");
        ProjectStatus pipeline2_stage2_job = new ProjectStatus("pipeline2 :: stage :: job", "Activity2-Job", "Status2-Job", "Label2-Job", new Date(), "job2-url");

        when(cache.get("pipeline1 :: stage1")).thenReturn(pipeline1_stage1);
        when(cache.get("pipeline1 :: stage1 :: job1")).thenReturn(pipeline1_stage1_job);
        when(cache.get("pipeline2 :: stage2")).thenReturn(pipeline2_stage2);
        when(cache.get("pipeline2 :: stage2 :: job2")).thenReturn(pipeline2_stage2_job);
        when(ccTrayViewAuthority.groupsAndTheirViewers()).thenReturn(m("group1", viewers("user1", "user2"), "group2", viewers("user3")));

        CruiseConfig config = GoConfigMother.defaultCruiseConfig();
        goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");


        handler.call(config);


        verify(cache).replaceAllEntriesInCacheWith(statusesCaptor.capture());
        List<ProjectStatus> statuses = statusesCaptor.getValue();
        assertThat(statuses.size(), is(4));

        assertThat(statuses.get(0).name(), is("pipeline1 :: stage1"));
        assertThat(statuses.get(0).canBeViewedBy("user1"), is(true));
        assertThat(statuses.get(0).canBeViewedBy("user2"), is(true));
        assertThat(statuses.get(0).canBeViewedBy("user3"), is(false));

        assertThat(statuses.get(1).name(), is("pipeline1 :: stage1 :: job1"));
        assertThat(statuses.get(1).canBeViewedBy("user1"), is(true));
        assertThat(statuses.get(1).canBeViewedBy("user2"), is(true));
        assertThat(statuses.get(1).canBeViewedBy("user3"), is(false));

        assertThat(statuses.get(2).name(), is("pipeline2 :: stage2"));
        assertThat(statuses.get(2).canBeViewedBy("user1"), is(false));
        assertThat(statuses.get(2).canBeViewedBy("user2"), is(false));
        assertThat(statuses.get(2).canBeViewedBy("user3"), is(true));

        assertThat(statuses.get(3).name(), is("pipeline2 :: stage2 :: job2"));
        assertThat(statuses.get(3).canBeViewedBy("user1"), is(false));
        assertThat(statuses.get(3).canBeViewedBy("user2"), is(false));
        assertThat(statuses.get(3).canBeViewedBy("user3"), is(true));
    }

    private Viewers viewers(String... users) {
        return new AllowedViewers(s(users));
    }

    private PipelineConfig pipelineConfigFor(CruiseConfig config, String pipelineName) {
        return config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
    }

    private StageConfig stageConfigFor(CruiseConfig config, String pipelineName, String stageName) {
        return pipelineConfigFor(config, pipelineName).getStage(new CaseInsensitiveString(stageName));
    }
}