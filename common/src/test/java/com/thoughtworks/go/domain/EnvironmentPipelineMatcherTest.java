/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentPipelineConfig;
import com.thoughtworks.go.config.EnvironmentPipelinesConfig;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EnvironmentPipelineMatcherTest {

    @Test
    public void shouldMatchPipelineNameIgnoringCase() {
        EnvironmentPipelineMatcher matcher = new EnvironmentPipelineMatcher(new CaseInsensitiveString("env"), Arrays.asList("uuid1", "uuid2"),
                new EnvironmentPipelinesConfig() {{
                    add(new EnvironmentPipelineConfig(new CaseInsensitiveString("pipeline1")));
                    add(new EnvironmentPipelineConfig(new CaseInsensitiveString("pipeline2")));
                }});

        assertThat(matcher.hasPipeline(jobPlan("PipeLine1").getPipelineName()), is(true));
        assertThat(matcher.hasPipeline(jobPlan("PIPELINE1").getPipelineName()), is(true));
        assertThat(matcher.hasPipeline(jobPlan("PIPELine1").getPipelineName()), is(true));
        assertThat(matcher.hasPipeline(jobPlan("PipeLine2").getPipelineName()), is(true));
    }

    private DefaultJobPlan jobPlan(String pipelineName) {
        return new DefaultJobPlan(null, null, 0, new JobIdentifier(pipelineName, 0, "label", "stage", "1", "blahBuildName", 0L), null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
    }
}
