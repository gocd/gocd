/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Tab;
import com.thoughtworks.go.config.Tabs;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helpers.DataUtils;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.LogFile;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JobDetailServiceTest {
    private Mockery context = new ClassMockery();
    private JobDetailService jobDetailService;
    private ArtifactsService artifactsService;
    private GoConfigService configService;
    private JobInstanceDao jobInstanceDao;
    private JobInstance job;
    private Tab customizedTab;

    @Before
    public void setUp() throws Exception {
        artifactsService = context.mock(ArtifactsService.class);
        configService = context.mock(GoConfigService.class);
        jobInstanceDao = context.mock(JobInstanceDao.class);
        jobDetailService = new JobDetailService(artifactsService, jobInstanceDao, configService);

        final String jobName = "jobConfig1";
        final int id = 1;
        job = JobInstanceMother.completed(jobName, JobResult.Failed);
        job.setId(id);
        job.setIdentifier(new JobIdentifier("pipeline", "Label:1", "stage", "1", jobName));

        final Tabs tabs = new Tabs();
        customizedTab = new Tab("myArtifacts", "my/artifact/path");
        tabs.add(customizedTab);

        context.checking(new Expectations() {
            {
                one(configService).getCustomizedTabs("pipeline", "stage", jobName);
                will(returnValue(tabs));
            }
        });

    }

    @Test
    public void shouldCreateBuildInstanceDetailFromLogFileForCompletedBuild() throws Exception {
        final String jobConfigName = "jobConfig1";
        final int id = 1;
        final JobInstance completed = JobInstanceMother.completed(jobConfigName, JobResult.Failed);
        completed.setId(id);
        completed.setIdentifier(new JobIdentifier("pipeline", "1", "stage", "1", jobConfigName));
        final HashMap properties = new HashMap();
        final File artifactsRoot = new File("artifacts");
        context.checking(new Expectations() {
            {
                one(artifactsService).getInstanceLogFile(completed.getIdentifier());
                LogFile buildLogFile = new LogFile(DataUtils.getFailedBuildLbuildAsFile().getFile());
                will(returnValue(buildLogFile));
                one(artifactsService).parseLogFile(buildLogFile, completed.isPassed());
                will(returnValue(properties));
                one(artifactsService).findArtifact(completed.getIdentifier(), "");
                will(returnValue(artifactsRoot));
            }
        });
        jobDetailService.loadBuildInstanceLog(completed);
        assertThat(completed.getName(), is(jobConfigName));
        assertThat(properties.get("artifactfolder"), is(artifactsRoot));
    }
}
