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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import org.junit.Test;

public class BuildPlansTest {

    @Test
    public void shouldFindBuildPlanByName() {
        JobConfigs jobConfigs = new JobConfigs();
        jobConfigs.add(jobConfig("Test"));
        assertThat(jobConfigs.containsName(new CaseInsensitiveString("Poo")), is(false));
    }

    @Test
    public void shouldNotBombIfABuildPlanWithSameNameIsSetAgain() throws Exception {
        JobConfigs jobConfigs = new JobConfigs();
        jobConfigs.add(jobConfig("Test"));
        jobConfigs.set(0, jobConfig("Test"));
    }

    @Test
    public void shouldBombIfABuildPlanWithSameNameIsAdded() throws Exception {
        JobConfigs jobConfigs = new JobConfigs();
        jobConfigs.add(jobConfig("Test"));
        try {
            jobConfigs.add(jobConfig("Test"));
            fail("Should not be able to add build plan with the same name again");
        } catch (RuntimeException ignored) {
        }
    }

    @Test
    public void shouldBombIfABuildPlanWithSameNameWithDifferentCaseIsAdded() throws Exception {
        JobConfigs jobConfigs = new JobConfigs();
        jobConfigs.add(jobConfig("Test"));
        try {
            jobConfigs.add(jobConfig("test"));
            fail("Should not be able to add build plan with the same name again");
        } catch (RuntimeException ignored) {
        }
    }

    private JobConfig jobConfig(String jobConfigName) {
        return new JobConfig(new CaseInsensitiveString(jobConfigName), null, null);
    }
}
