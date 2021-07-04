/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.helper.JobInstanceMother;
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

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CcTrayJobStatusChangeHandlerTest {
    @Mock
    private CcTrayCache cache;
    @Captor
    ArgumentCaptor<ProjectStatus> statusCaptor;


    @Test
    public void shouldGetJobStatusWithNewActivity_NotTheOneInCache() throws Exception {
        ProjectStatus status = new ProjectStatus(projectNameFor("job1"), "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        when(cache.get(projectNameFor("job1"))).thenReturn(status);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.building("job1"), new HashSet<>());

        assertThat(activityOf(newStatus), is("Building"));
    }

    @Test
    public void shouldCreateNewStatusWithOldValuesFromCache_WhenNewJob_HasNotCompleted() throws Exception {
        ProjectStatus oldStatusInCache = new ProjectStatus(projectNameFor("job1"), "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        when(cache.get(projectNameFor("job1"))).thenReturn(oldStatusInCache);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.building("job1"), new HashSet<>());

        assertThat(newStatus.getLastBuildStatus(), is(oldStatusInCache.getLastBuildStatus()));
        assertThat(newStatus.getLastBuildLabel(), is(oldStatusInCache.getLastBuildLabel()));
        assertThat(newStatus.getLastBuildTime(), is(oldStatusInCache.getLastBuildTime()));
        assertThat(newStatus.getBreakers(), is(oldStatusInCache.getBreakers()));
        assertThat(webUrlOf(newStatus), is(webUrlFor("job1")));
    }

    @Test
    public void shouldCreateNewStatusWithNoPartsUsedFromCache_WhenNewJob_HasCompleted() throws Exception {
        ProjectStatus oldStatusInCache = new ProjectStatus(projectNameFor("job1"), "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        when(cache.get(projectNameFor("job1"))).thenReturn(oldStatusInCache);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.completed("job1"), new HashSet<>());

        assertThat(activityOf(newStatus), is("Sleeping"));
        assertThat(newStatus.getLastBuildStatus(), is("Success"));
        assertThat(newStatus.getLastBuildLabel(), is("label-1"));
        assertThat(newStatus.getBreakers(), is(Collections.<String>emptySet()));
        assertThat(webUrlOf(newStatus), is(webUrlFor("job1")));
    }

    @Test
    public void shouldUpdateBreakersAlongWithOtherFields() throws Exception {
        String jobName = "job1";

        Set<String> breakers = new HashSet<>();
        breakers.add("abc");
        breakers.add("def");

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.completed(jobName), breakers);

        assertThat(newStatus.getBreakers(), is(breakers));
    }

    @Test
    public void shouldReuseViewersListFromExistingStatusWhenCreatingNewStatus() throws Exception {
        Users viewers = new AllowedUsers(s("viewer1", "viewer2"), Collections.singleton(new PluginRoleConfig("admin", "ldap")));

        ProjectStatus oldStatusInCache = new ProjectStatus(projectNameFor("job1"), "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("job1"));
        oldStatusInCache.updateViewers(viewers);
        when(cache.get(projectNameFor("job1"))).thenReturn(oldStatusInCache);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        ProjectStatus newStatus = handler.statusFor(JobInstanceMother.building("job1"), new HashSet<>());

        assertThat(newStatus.viewers(), is(viewers));
    }

    @Test
    public void shouldUpdateValueInCacheWhenJobHasChanged() throws Exception {
        String jobName = "job1";
        ProjectStatus existingStatusInCache = new ProjectStatus(projectNameFor(jobName), "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor(jobName));
        when(cache.get(projectNameFor(jobName))).thenReturn(existingStatusInCache);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        JobInstance completedJob = JobInstanceMother.completed(jobName);
        handler.call(completedJob);

        verify(cache).put(statusCaptor.capture());
        ProjectStatus newStatusInCache = statusCaptor.getValue();
        assertThat(newStatusInCache.name(), is(projectNameFor(jobName)));
        assertThat(newStatusInCache.getLastBuildStatus(), is("Success"));
        assertThat(newStatusInCache.getLastBuildLabel(), is("label-1"));
        assertThat(newStatusInCache.getLastBuildTime(), is(completedJob.getCompletedDate()));
        assertThat(newStatusInCache.getBreakers(), is(Collections.<String>emptySet()));
        assertThat(activityOf(newStatusInCache), is("Sleeping"));
        assertThat(webUrlOf(newStatusInCache), is(webUrlFor(jobName)));
    }

    private String projectNameFor(String jobName) {
        return "pipeline :: stage :: " + jobName;
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
