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
package com.thoughtworks.go.domain;

import static com.thoughtworks.go.config.RunIfConfig.PASSED;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.thoughtworks.go.config.RunIfConfig;
import org.junit.jupiter.api.Test;

public class RunIfConfigsTest {

    @Test
    public void shouldMatchWhenContainsCondition() {
        RunIfConfigs configs = new RunIfConfigs(PASSED);
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Passed.toLowerCase())), is(true));
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Failed.toLowerCase())), is(false));
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Cancelled.toLowerCase())), is(false));
    }

    @Test
    public void shouldMatchAnyWhenAnyIsDefined() {
        RunIfConfigs configs = new RunIfConfigs(RunIfConfig.ANY);
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Passed.toLowerCase())), is(true));
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Failed.toLowerCase())), is(true));
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Cancelled.toLowerCase())), is(true));
    }

    @Test
    public void testOnlyMatchPassedWhenNoneIsDefined() {
        RunIfConfigs configs = new RunIfConfigs();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Passed.toLowerCase())), is(true));
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Failed.toLowerCase())), is(false));
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Cancelled.toLowerCase())), is(false));
    }

    @Test
    public void shouldAddErrorsToErrorCollectionOfTheCollectionAsWellAsEachRunIfConfig() {
        RunIfConfigs configs = new RunIfConfigs();
        RunIfConfig config = new RunIfConfig("passed");
        config.addError("status", "some error");
        configs.add(config);
        configs.addError("key", "some error");
        assertThat(configs.errors().on("key"), is("some error"));
        assertThat(configs.get(0).errors().on("status"), is("some error"));
    }

}
