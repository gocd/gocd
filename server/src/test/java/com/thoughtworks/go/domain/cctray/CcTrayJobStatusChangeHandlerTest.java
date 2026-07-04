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

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.domain.cctray.ProjectStatus.Key.keyFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CcTrayJobStatusChangeHandlerTest {
    @Mock
    private CcTrayCache cache;
    @Captor
    ArgumentCaptor<ProjectStatus> statusCaptor;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> new ProjectStatus.NullProjectStatus(invocation.getArgument(0), invocation.getArgument(1)))
            .when(cache).getOrDefault(any(ProjectStatus.Key.class), anyInt());
    }

    @Test
    public void shouldGetJobStatusWithNewActivity_NotTheOneInCache() {
        ProjectStatus existingStageStatus = new ProjectStatus(keyFor("job1"), 0, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        when(cache.getOrDefault(eq(existingStageStatus.key()), anyInt())).thenReturn(existingStageStatus);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.building("job1"), new HashSet<>());

        assertThat(activityOf(newStatus)).isEqualTo("Building");
    }

    @Test
    public void shouldCreateNewStatusWithOldValuesFromCache_WhenNewJob_HasNotCompleted() {
        ProjectStatus existingStageStatus = new ProjectStatus(keyFor("job1"), 0, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        when(cache.getOrDefault(eq(existingStageStatus.key()), anyInt())).thenReturn(existingStageStatus);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.building("job1"), new HashSet<>());

        assertThat(newStatus.getLastBuildStatus()).isEqualTo(existingStageStatus.getLastBuildStatus());
        assertThat(newStatus.getLastBuildLabel()).isEqualTo(existingStageStatus.getLastBuildLabel());
        assertThat(newStatus.getLastBuildTime()).isEqualTo(existingStageStatus.getLastBuildTime());
        assertThat(newStatus.getBreakers()).isEqualTo(existingStageStatus.getBreakers());
        assertThat(webUrlOf(newStatus)).isEqualTo(webUrlFor("job1"));
    }

    @Test
    public void shouldCreateNewStatusWithNoPartsUsedFromCache_WhenNewJob_HasCompleted() {
        ProjectStatus existingStageStatus = new ProjectStatus(keyFor("job1"), 0, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        when(cache.getOrDefault(eq(existingStageStatus.key()), anyInt())).thenReturn(existingStageStatus);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.completed("job1"), new HashSet<>());

        assertThat(activityOf(newStatus)).isEqualTo("Sleeping");
        assertThat(newStatus.getLastBuildStatus()).isEqualTo("Success");
        assertThat(newStatus.getLastBuildLabel()).isEqualTo("label-1");
        assertThat(newStatus.getBreakers()).isEqualTo(Collections.<String>emptySet());
        assertThat(webUrlOf(newStatus)).isEqualTo(webUrlFor("job1"));
    }

    @Test
    public void shouldUpdateBreakersAlongWithOtherFields() {
        String jobName = "job1";

        Set<String> breakers = new HashSet<>();
        breakers.add("abc");
        breakers.add("def");

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);

        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.completed(jobName), breakers);

        assertThat(newStatus.getBreakers()).isEqualTo(breakers);
    }

    @Test
    public void shouldReuseViewersListFromExistingStatusWhenCreatingNewStatus() {
        Users viewers = new AllowedUsers(Set.of("viewer1", "viewer2"), Set.of(new PluginRoleConfig("admin", "ldap")));

        ProjectStatus existingStageStatus = new ProjectStatus(keyFor("job1"), 0, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        existingStageStatus.updateViewers(viewers);
        when(cache.getOrDefault(eq(existingStageStatus.key()), anyInt())).thenReturn(existingStageStatus);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.building("job1"), new HashSet<>());

        assertThat(newStatus.viewers()).isEqualTo(viewers);
    }

    @Test
    public void shouldUpdateValueInCacheWhenJobHasChanged() {
        String jobName = "job1";
        ProjectStatus existingStageStatus = new ProjectStatus(keyFor(jobName), 0, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor(jobName));
        when(cache.getOrDefault(eq(existingStageStatus.key()), anyInt())).thenReturn(existingStageStatus);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        JobInstance completedJob = JobInstanceMother.completed(jobName);
        handler.call(completedJob);

        verify(cache).put(statusCaptor.capture());
        ProjectStatus newStatusInCache = statusCaptor.getValue();
        assertThat(newStatusInCache.key()).isEqualTo(keyFor(jobName));
        assertThat(newStatusInCache.getLastBuildStatus()).isEqualTo("Success");
        assertThat(newStatusInCache.getLastBuildLabel()).isEqualTo("label-1");
        assertThat(newStatusInCache.getLastBuildTime()).isEqualTo(completedJob.getCompletedDate());
        assertThat(newStatusInCache.getBreakers()).isEqualTo(Collections.<String>emptySet());
        assertThat(activityOf(newStatusInCache)).isEqualTo("Sleeping");
        assertThat(webUrlOf(newStatusInCache)).isEqualTo(webUrlFor(jobName));
    }

    private ProjectStatus.Key keyFor(String jobName) {
        return keyFrom("pipeline", "stage", jobName);
    }

    private String webUrlOf(ProjectStatus status) {
        return status.ccTrayXmlElement("some-path").getAttribute("webUrl").getValue();
    }

    private String activityOf(ProjectStatus status) {
        return status.ccTrayXmlElement("some-path").getAttribute("activity").getValue();
    }

    private String webUrlFor(final String jobName) {
        return "some-path/tab/build/detail/pipeline/1/stage/1/" + jobName;
    }
}
