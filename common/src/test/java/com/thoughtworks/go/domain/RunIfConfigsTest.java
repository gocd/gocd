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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.RunIfConfig;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.config.RunIfConfig.PASSED;
import static org.assertj.core.api.Assertions.assertThat;

public class RunIfConfigsTest {

    @Test
    public void shouldMatchWhenContainsCondition() {
        RunIfConfigs configs = new RunIfConfigs(PASSED);
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Passed.toLowerCase()))).isTrue();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Failed.toLowerCase()))).isFalse();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Cancelled.toLowerCase()))).isFalse();
    }

    @Test
    public void shouldMatchAnyWhenAnyIsDefined() {
        RunIfConfigs configs = new RunIfConfigs(RunIfConfig.ANY);
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Passed.toLowerCase()))).isTrue();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Failed.toLowerCase()))).isTrue();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Cancelled.toLowerCase()))).isTrue();
    }

    @Test
    public void testOnlyMatchPassedWhenNoneIsDefined() {
        RunIfConfigs configs = new RunIfConfigs();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Passed.toLowerCase()))).isTrue();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Failed.toLowerCase()))).isFalse();
        assertThat(configs.match(RunIfConfig.fromJobResult(JobResult.Cancelled.toLowerCase()))).isFalse();
    }

    @Test
    public void shouldAddErrorsToErrorCollectionOfTheCollectionAsWellAsEachRunIfConfig() {
        RunIfConfigs configs = new RunIfConfigs();
        RunIfConfig config = new RunIfConfig("passed");
        config.addError("status", "some error");
        configs.add(config);
        configs.addError("key", "some error");
        assertThat(configs.errors().on("key")).isEqualTo("some error");
        assertThat(configs.get(0).errors().on("status")).isEqualTo("some error");
    }

}
