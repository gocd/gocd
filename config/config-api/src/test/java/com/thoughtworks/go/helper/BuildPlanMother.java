/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.config.ArtifactTypeConfigs;

public class BuildPlanMother {

    public static JobConfigs jobConfigs(String... buildNames) {
        JobConfigs jobConfigs = new JobConfigs();
        for (String buildName : buildNames) {
            jobConfigs.add(new JobConfig(new CaseInsensitiveString(buildName), new ResourceConfigs(), new ArtifactTypeConfigs()));
        }
        return jobConfigs;
    }

    public static JobConfigs withBuildPlans(String... jobConfigNames) {
        JobConfigs jobConfigs = new JobConfigs();
        for (String planName : jobConfigNames) {
            jobConfigs.add(new JobConfig(new CaseInsensitiveString(planName), new ResourceConfigs(), new ArtifactTypeConfigs()));
        }
        return jobConfigs;
    }

}
