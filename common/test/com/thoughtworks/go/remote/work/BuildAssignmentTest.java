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

package com.thoughtworks.go.remote.work;

import java.util.ArrayList;

import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.ArtifactPropertiesGenerators;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;

public class BuildAssignmentTest {
    @Test public void shouldStartWithNoEnvironmentContext() throws Exception {
        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), BuildCause.createManualForced(), new ArrayList<Builder>(), null);
        assertThat(buildAssignment.initialEnvironmentVariableContext(), is(new EnvironmentVariableContext()));
    }

    @Test public void shouldEnhanceInitialEnvironmentContext() throws Exception {
        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), BuildCause.createManualForced(), new ArrayList<Builder>(), null);

        buildAssignment.enhanceEnvironmentVariables(new EnvironmentVariableContext("foo", "bar"));

        assertThat(buildAssignment.initialEnvironmentVariableContext(), is(new EnvironmentVariableContext("foo", "bar")));
    }

    private DefaultJobPlan jobForPipeline(String pipelineName) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier);
    }
}
