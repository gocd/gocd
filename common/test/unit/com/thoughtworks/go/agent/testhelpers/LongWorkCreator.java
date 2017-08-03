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

package com.thoughtworks.go.agent.testhelpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class LongWorkCreator implements WorkCreator {
    public static final String PIPELINE_NAME = "studios";
    public static final String PIPELINE_LABEL = "100";
    public static final String STAGE_NAME = "pipeline";
    public static final String JOB_PLAN_NAME = "cruise-test-data";

    public static final File ARTIFACT_FILE = tempFile();
    public static final File ARTIFACT_FOLDER = tempFolder();
    private ArrayList<Builder> builder;

    private static File tempFile() {
        try {
            File tempFile = TestFileUtil.createTempFile("artifact.tmp");
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    private static File tempFolder() {
        File tmpdir = new File(SystemEnvironment.getProperty("java.io.tmpdir"), "goServerStub");
        tmpdir.deleteOnExit();
        final File dir = new File(tmpdir, "testdir");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }

        if (!dir.mkdirs()) {
            bomb("Unable to create " + dir.getAbsolutePath());
        }
        dir.deleteOnExit();
        ArrayList<File> files = new ArrayList<File>() {
            {
                add(new File(dir, "test1.txt"));
                add(new File(dir, "test2.txt"));
            }
        };
        for (File file : files) {
            try {
                if (!file.createNewFile()) {
                    bomb("Unable to create " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                bomb("Unable to create " + file.getAbsolutePath());
            }
            file.deleteOnExit();
        }
        return dir;
    }

    public Work getWork() {
        try {
            CruiseConfig config = GoConfigMother.pipelineHavingJob(PIPELINE_NAME, STAGE_NAME, JOB_PLAN_NAME, ARTIFACT_FILE.getAbsolutePath(), ARTIFACT_FOLDER.getAbsolutePath());
            BuildCause buildCause = BuildCause.createWithEmptyModifications();
            builder = new ArrayList<>();
            builder.add(new SleepBuilder());
            JobPlan instance = toBuildInstance(config);
            BuildAssignment buildAssignment = BuildAssignment.create(
                    instance,
                    buildCause,
                    builder,
                    new File(CruiseConfig.WORKING_BASE_DIR + PIPELINE_NAME));
            return new BuildWork(buildAssignment);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    public Work work(AgentIdentifier agentIdentifier) {
        return getWork();
    }

    private JobPlan toBuildInstance(final CruiseConfig config) throws Exception {
        JobConfig plan = config.jobConfigByName(PIPELINE_NAME, STAGE_NAME, JOB_PLAN_NAME, true);
        return JobInstanceMother.createJobPlan(plan, new JobIdentifier(PIPELINE_NAME, PIPELINE_LABEL, STAGE_NAME, "1", JOB_PLAN_NAME), new DefaultSchedulingContext());
    }

    public static class SleepBuilder extends Builder {

        public SleepBuilder() {
            super(new RunIfConfigs(), new NullBuilder(), "");
        }

        public void build(DefaultGoPublisher publisher,
                          EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            } finally {
            }
        }
    }
}
