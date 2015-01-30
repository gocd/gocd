/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CcTrayJobStatusChangeHandlerTest {
    @Mock
    private CcTrayCache cache;
    @Captor
    ArgumentCaptor<ProjectStatus> statusCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldChangeActivityOfJobInCacheWithDetailsOfNewJob() throws Exception {
        Pair<ProjectStatus, ProjectStatus> oldAndNewStatuses = simulateJobStatusChangedToBuilding("job1");
        ProjectStatus newStatusInCache = oldAndNewStatuses.last();

        assertThat(activityOf(newStatusInCache), is("Building"));
    }

    @Test
    public void shouldNotChangeAnyPartOfStatusOtherThanActivityOfJobInCacheWithDetailsOfNewJobWhenNewJob_HasNotCompleted() throws Exception {
        Pair<ProjectStatus, ProjectStatus> oldAndNewStatuses = simulateJobStatusChangedToBuilding("job1");
        ProjectStatus oldStatusInCache = oldAndNewStatuses.first();
        ProjectStatus newStatusInCache = oldAndNewStatuses.last();

        assertThat(newStatusInCache.getLastBuildStatus(), is(oldStatusInCache.getLastBuildStatus()));
        assertThat(newStatusInCache.getLastBuildLabel(), is(oldStatusInCache.getLastBuildLabel()));
        assertThat(newStatusInCache.getLastBuildTime(), is(oldStatusInCache.getLastBuildTime()));
        assertThat(newStatusInCache.getBreakers(), is(oldStatusInCache.getBreakers()));
        assertThat(webUrlOf(newStatusInCache), is(webUrlFor("job1")));
    }

    @Test
    public void shouldChangeEveryPartOfStatusOfJobInCacheWithDetailsOfNewJobWhenNewJob_HasCompleted() throws Exception {
        Pair<ProjectStatus, ProjectStatus> oldAndNewStatuses = simulateJobStatusChangedToCompleted("job1");
        ProjectStatus newStatusInCache = oldAndNewStatuses.last();

        assertThat(activityOf(newStatusInCache), is("Sleeping"));
        assertThat(newStatusInCache.getLastBuildStatus(), is("Success"));
        assertThat(newStatusInCache.getLastBuildLabel(), is("label-1"));
        assertThat(newStatusInCache.getBreakers(), is(Collections.<String>emptySet()));
        assertThat(webUrlOf(newStatusInCache), is(webUrlFor("job1")));
    }

    private Pair<ProjectStatus, ProjectStatus> simulateJobStatusChangedToBuilding(String jobName) {
        ProjectStatus existingProjectStatus = setupJobStatusInCache(jobName);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        handler.call(JobInstanceMother.building(jobName));

        verify(cache).replace(eq(projectNameFor(jobName)), statusCaptor.capture());
        return new Pair<ProjectStatus, ProjectStatus>(existingProjectStatus, statusCaptor.getValue());
    }

    private Pair<ProjectStatus, ProjectStatus> simulateJobStatusChangedToCompleted(String jobName) {
        ProjectStatus existingProjectStatus = setupJobStatusInCache(jobName);

        CcTrayJobStatusChangeHandler handler = new CcTrayJobStatusChangeHandler(cache);
        handler.call(JobInstanceMother.completed(jobName));

        verify(cache).replace(eq(projectNameFor(jobName)), statusCaptor.capture());
        return new Pair<ProjectStatus, ProjectStatus>(existingProjectStatus, statusCaptor.getValue());
    }

    private ProjectStatus setupJobStatusInCache(final String jobName) {
        String projectName = projectNameFor(jobName);
        ProjectStatus status = new ProjectStatus(projectName, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor(jobName));
        when(cache.get(projectName)).thenReturn(status);
        return status;
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