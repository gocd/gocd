/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.security.GoConfigPipelinePermissionsAuthority;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.permissions.PipelinePermission;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.domain.cctray.ProjectStatus.Key.keyFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CcTrayConfigChangeHandlerTest {
    private final ProjectStatus.Key p1s1 = keyFrom("pipeline1", "stage");
    private final ProjectStatus.Key p1s1Job = keyFrom("pipeline1", "stage", "job");
    private final ProjectStatus.Key p1NewJob = keyFrom("pipeline1", "stage", "NEW_JOB_IN_CONFIG");

    private final ProjectStatus.Key p1s2 = keyFrom("pipeline1", "stage2");
    private final ProjectStatus.Key p1s2Job = keyFrom("pipeline1", "stage2", "job");

    private final ProjectStatus.Key p2s1 = keyFrom("pipeline2", "stage");
    private final ProjectStatus.Key p2s1Job = keyFrom("pipeline2", "stage", "job");

    @Mock
    private CcTrayCache cache;
    @Mock
    private CcTrayStageStatusLoader stageStatusLoader;
    @Mock
    private GoConfigPipelinePermissionsAuthority pipelinePermissionsAuthority;
    @Captor
    ArgumentCaptor<List<ProjectStatus>> statusesCaptor;

    private GoConfigMother goConfigMother;
    private CcTrayConfigChangeHandler handler;
    private PluginRoleUsersStore pluginRoleUsersStore;

    @BeforeEach
    public void setUp() {
        goConfigMother = new GoConfigMother();
        handler = new CcTrayConfigChangeHandler(cache, stageStatusLoader, pipelinePermissionsAuthority);
        lenient().when(pipelinePermissionsAuthority.permissionsForPipeline(any())).thenReturn(Permissions.NOONE);
        pluginRoleUsersStore = PluginRoleUsersStore.instance();
    }

    @AfterEach
    public void tearDown() {
        pluginRoleUsersStore.clearAll();
    }

    @Test
    public void shouldProvideCCTrayCacheWithAListOfAllProjectsInOrder() {
        ProjectStatus pipeline1_stage1 = new ProjectStatus(p1s1, 0, "Activity1", "Status1", "Label1", new Date(), "stage1-url");
        ProjectStatus pipeline1_stage1_job = new ProjectStatus(p1s1Job, 0, "Activity1-Job", "Status1-Job", "Label1-Job", new Date(), "job1-url");
        ProjectStatus pipeline2_stage1 = new ProjectStatus(p2s1, 0, "Activity2", "Status2", "Label2", new Date(), "stage2-url");
        ProjectStatus pipeline2_stage1_job = new ProjectStatus(p2s1Job,0, "Activity2-Job", "Status2-Job", "Label2-Job", new Date(), "job2-url");

        when(cache.get(p1s1)).thenReturn(pipeline1_stage1);
        when(cache.get(p1s1Job)).thenReturn(pipeline1_stage1_job);
        when(cache.get(p2s1)).thenReturn(pipeline2_stage1);
        when(cache.get(p2s1Job)).thenReturn(pipeline2_stage1_job);

        handler.call(GoConfigMother.configWithPipelines("pipeline2", "pipeline1")); /* Adds pipeline1 first in config. Then pipeline2. */

        verify(cache).replaceAll(eq(List.of(pipeline1_stage1, pipeline1_stage1_job, pipeline2_stage1, pipeline2_stage1_job)));
        verifyNoInteractions(stageStatusLoader);
    }

    @Test
    public void shouldPopulateNewCacheWithProjectsFromOldCacheWhenTheyExist() {
        ProjectStatus existingStageStatus = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        when(cache.get(p1s1)).thenReturn(existingStageStatus);

        ProjectStatus existingJobStatus = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job-url");
        when(cache.get(p1s1Job)).thenReturn(existingJobStatus);

        handler.call(GoConfigMother.configWithPipelines("pipeline1"));

        verify(cache).replaceAll(eq(List.of(existingStageStatus, existingJobStatus)));
        verifyNoInteractions(stageStatusLoader);
    }

    @Test
    public void shouldPopulateNewCacheWithStageAndJobFromDB_WhenAStageIsNotFoundInTheOldCache() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");

        ProjectStatus statusOfStageInDB = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJobInDB = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job-url");
        when(cache.get(p1s1)).thenReturn(null);
        when(stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfigFor(config, "pipeline1"), stageConfigFor(config, "pipeline1", "stage")))
            .thenReturn(List.of(statusOfStageInDB, statusOfJobInDB));

        handler.call(config);

        verify(cache).replaceAll(eq(List.of(statusOfStageInDB, statusOfJobInDB)));
    }

    @Test
    public void shouldHandleNewStagesInConfig_ByReplacingStagesMissingInDBWithNullStagesAndJobs() {
        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage", "job");
        goConfigMother.addStageToPipeline(config, "pipeline1", "stage2", "job");

        ProjectStatus statusOfStage1InCache = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InCache = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(p1s1)).thenReturn(statusOfStage1InCache);
        when(cache.get(p1s1Job)).thenReturn(statusOfJob1InCache);

        when(cache.get(p1s2)).thenReturn(null);
        when(stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfigFor(config, "pipeline1"), stageConfigFor(config, "pipeline1", "stage2")))
            .thenReturn(Collections.emptyList());

        handler.call(config);

        ProjectStatus expectedNullStatusForStage2 = new ProjectStatus.NullProjectStatus(p1s2);
        ProjectStatus expectedNullStatusForJob2 = new ProjectStatus.NullProjectStatus(p1s2Job);
        verify(cache).replaceAll(eq(List.of(statusOfStage1InCache, statusOfJob1InCache, expectedNullStatusForStage2, expectedNullStatusForJob2)));
    }

    /* Simulate adding a job, when server is down. DB does not know anything about that job. */
    @Test
    public void shouldHandleNewJobsInConfig_ByReplacingJobsMissingInDBWithNullJob() {
        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage", "job", "NEW_JOB_IN_CONFIG");

        ProjectStatus statusOfStage1InDB = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InDB = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(p1s1)).thenReturn(null);
        when(stageStatusLoader.getStatusesForStageAndJobsOf(pipelineConfigFor(config, "pipeline1"), stageConfigFor(config, "pipeline1", "stage")))
            .thenReturn(List.of(statusOfStage1InDB, statusOfJob1InDB));

        handler.call(config);


        ProjectStatus expectedNullStatusForNewJob = new ProjectStatus.NullProjectStatus(p1NewJob);
        verify(cache).replaceAll(eq(List.of(statusOfStage1InDB, statusOfJob1InDB, expectedNullStatusForNewJob)));
    }

    /* Simulate adding a job, in a running system. Cache has the stage info, but not the job info. */
    @Test
    public void shouldHandleNewJobsInConfig_ByReplacingJobsMissingInConfigWithNullJob() {
        ProjectStatus statusOfStage1InCache = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InCache = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(p1s1)).thenReturn(statusOfStage1InCache);
        when(cache.get(p1s1Job)).thenReturn(statusOfJob1InCache);
        when(cache.get(p1NewJob)).thenReturn(null);

        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage", "job", "NEW_JOB_IN_CONFIG");

        handler.call(config);

        ProjectStatus expectedNullStatusForNewJob = new ProjectStatus.NullProjectStatus(p1NewJob);
        verify(cache).replaceAll(eq(List.of(statusOfStage1InCache, statusOfJob1InCache, expectedNullStatusForNewJob)));
        verifyNoInteractions(stageStatusLoader);
    }

    @Test
    public void shouldRemoveExtraJobsFromCache_WhichAreNoLongerInConfig() {
        ProjectStatus statusOfStage1InCache = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "stage-url");
        ProjectStatus statusOfJob1InCache = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "job1-url");
        when(cache.get(p1s1)).thenReturn(statusOfStage1InCache);
        when(cache.get(p1s1Job)).thenReturn(statusOfJob1InCache);

        CruiseConfig config = new BasicCruiseConfig();
        goConfigMother.addPipeline(config, "pipeline1", "stage", "job");

        handler.call(config);

        verify(cache).replaceAll(eq(List.of(statusOfStage1InCache, statusOfJob1InCache)));
        verifyNoInteractions(stageStatusLoader);
    }

    @Test
    public void shouldUpdateViewPermissionsForEveryProjectBasedOnViewPermissionsOfTheGroup() {
        PluginRoleConfig admin = new PluginRoleConfig("admin", "ldap");
        pluginRoleUsersStore.assignRole("user4", admin);

        Permissions pipeline1Permissions = new Permissions(viewers("user1", "user2"), Users.NOONE, Users.NOONE, PipelinePermission.NOONE);
        Permissions pipeline2Permissions = new Permissions(new AllowedUsers(Set.of("user3"), Set.of(admin)), Users.NOONE, Users.NOONE, PipelinePermission.NOONE);
        when(pipelinePermissionsAuthority.pipelinesAndTheirPermissions()).thenReturn(Map.of(cis("pipeline1"), pipeline1Permissions, cis("pipeline2"), pipeline2Permissions));

        CruiseConfig config = GoConfigMother.defaultCruiseConfig();
        goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage", "job");

        handler.call(config);

        verify(cache).replaceAll(statusesCaptor.capture());
        List<ProjectStatus> statuses = statusesCaptor.getValue();
        assertThat(statuses.size()).isEqualTo(4);

        assertThat(statuses.getFirst().name()).isEqualTo("pipeline1 :: stage");
        assertThat(statuses.getFirst().canBeViewedBy("user1")).isTrue();
        assertThat(statuses.getFirst().canBeViewedBy("user2")).isTrue();
        assertThat(statuses.getFirst().canBeViewedBy("user3")).isFalse();
        assertThat(statuses.getFirst().canBeViewedBy("user4")).isFalse();

        assertThat(statuses.get(1).name()).isEqualTo("pipeline1 :: stage :: job");
        assertThat(statuses.get(1).canBeViewedBy("user1")).isTrue();
        assertThat(statuses.get(1).canBeViewedBy("user2")).isTrue();
        assertThat(statuses.get(1).canBeViewedBy("user3")).isFalse();
        assertThat(statuses.get(1).canBeViewedBy("user4")).isFalse();

        assertThat(statuses.get(2).name()).isEqualTo("pipeline2 :: stage2");
        assertThat(statuses.get(2).canBeViewedBy("user1")).isFalse();
        assertThat(statuses.get(2).canBeViewedBy("user2")).isFalse();
        assertThat(statuses.get(2).canBeViewedBy("user3")).isTrue();
        assertThat(statuses.get(2).canBeViewedBy("user4")).isTrue();

        assertThat(statuses.get(3).name()).isEqualTo("pipeline2 :: stage2 :: job2");
        assertThat(statuses.get(3).canBeViewedBy("user1")).isFalse();
        assertThat(statuses.get(3).canBeViewedBy("user2")).isFalse();
        assertThat(statuses.get(3).canBeViewedBy("user3")).isTrue();
        assertThat(statuses.get(3).canBeViewedBy("user4")).isTrue();
    }

    @Test
    public void shouldUpdateCacheWithPipelineDetailsWhenPipelineConfigChanges() {
        ProjectStatus statusOfp1s1InCache = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "p1-stage-url");
        ProjectStatus statusOfp1j1InCache = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "p1-job-url");
        when(cache.get(p1s1)).thenReturn(statusOfp1s1InCache);
        when(cache.get(p1s1Job)).thenReturn(statusOfp1j1InCache);

        PipelineConfig pipeline1Config = GoConfigMother.pipelineHavingJob("pipeline1", "stage", "job", "arts", "dir").pipelineConfigByName(cis("pipeline1"));

        handler.call(pipeline1Config);
        @SuppressWarnings("unchecked") ArgumentCaptor<ArrayList<ProjectStatus>> argumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(cache).replaceForPipeline(eq("pipeline1"), argumentCaptor.capture());

        List<ProjectStatus> allValues = argumentCaptor.getValue();
        assertThat(allValues.get(0).key()).isEqualTo(p1s1);
        assertThat(allValues.get(1).key()).isEqualTo(p1s1Job);

        verify(cache, atLeastOnce()).get(p1s1);
        verify(cache, atLeastOnce()).get(p1s1Job);
        verifyNoMoreInteractions(cache);
    }

    @Test
    public void shouldUpdateCacheWhenPipelineIsDeleted() {
        ProjectStatus statusOfp1s1InCache = new ProjectStatus(p1s1, 0, "OldActivity", "OldStatus", "OldLabel", new Date(), "p1-stage-url");
        ProjectStatus statusOfp1j1InCache = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "p1-job-url");
        when(cache.get(p1s1)).thenReturn(statusOfp1s1InCache);
        when(cache.get(p1s1Job)).thenReturn(statusOfp1j1InCache);


        PipelineConfig pipeline1Config = GoConfigMother.pipelineHavingJob("pipeline1", "stage", "job", "arts", "dir").pipelineConfigByName(cis("pipeline1"));

        // Pipeline exists and everyone can see it
        when(pipelinePermissionsAuthority.permissionsForPipeline(cis("pipeline1"))).thenReturn(Permissions.EVERYONE);
        handler.call(pipeline1Config);
        verify(cache).replaceForPipeline(eq("pipeline1"), statusesCaptor.capture());
        clearInvocations(cache);
        assertThat(statusesCaptor.getValue())
            .allSatisfy(s -> assertThat(s.canBeViewedBy("someone")).describedAs("status can be viewed by anyone").isTrue());

        when(pipelinePermissionsAuthority.permissionsForPipeline(cis("pipeline1"))).thenReturn(Permissions.NOONE);
        // Pipeline is deleted; meaning no-one can see it
        handler.call(pipeline1Config);
        verify(cache).replaceForPipeline(eq("pipeline1"), statusesCaptor.capture());
        assertThat(statusesCaptor.getValue())
            .allSatisfy(s -> assertThat(s.canBeViewedBy("someone")).describedAs("status can be viewed by no-one").isFalse());
    }

    @Test
    public void shouldUpdateCacheWithAppropriateViewersForProjectStatusWhenPipelineConfigChanges() {
        ProjectStatus statusOfp1s1InCache = new ProjectStatus(p1s1, 0,"OldActivity", "OldStatus", "OldLabel", new Date(), "p1-stage-url");
        ProjectStatus statusOfp1j1InCache = new ProjectStatus(p1s1Job, 0, "OldActivity-Job", "OldStatus-Job", "OldLabel-Job", new Date(), "p1-job-url");
        when(cache.get(p1s1)).thenReturn(statusOfp1s1InCache);
        when(cache.get(p1s1Job)).thenReturn(statusOfp1j1InCache);

        PipelineConfig pipeline1Config = GoConfigMother.pipelineHavingJob("pipeline1", "stage", "job", "arts", "dir").pipelineConfigByName(cis("pipeline1"));
        when(pipelinePermissionsAuthority.permissionsForPipeline(pipeline1Config.name())).thenReturn(new Permissions(viewers("user1", "user2"), null, null, null));

        handler.call(pipeline1Config);
        verify(cache).replaceForPipeline(eq("pipeline1"), statusesCaptor.capture());

        List<ProjectStatus> allValues = statusesCaptor.getValue();
        assertThat(allValues.getFirst().key()).isEqualTo(p1s1);
        assertThat(allValues.getFirst().viewers().contains("user1")).isTrue();
        assertThat(allValues.getFirst().viewers().contains("user2")).isTrue();
        assertThat(allValues.getFirst().viewers().contains("user3")).isFalse();
        assertThat(allValues.getLast().key()).isEqualTo(p1s1Job);
        assertThat(allValues.getLast().viewers().contains("user1")).isTrue();
        assertThat(allValues.getLast().viewers().contains("user2")).isTrue();
        assertThat(allValues.getLast().viewers().contains("user3")).isFalse();
    }

    private Users viewers(String... users) {
        return new AllowedUsers(Set.of(users), Collections.emptySet());
    }

    private PipelineConfig pipelineConfigFor(CruiseConfig config, String pipelineName) {
        return config.pipelineConfigByName(cis(pipelineName));
    }

    private StageConfig stageConfigFor(CruiseConfig config, String pipelineName, String stageName) {
        return pipelineConfigFor(config, pipelineName).getStage(cis(stageName));
    }
}
