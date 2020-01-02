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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageConfigIdentifierTest {

    @Test
    public void shouldUseCaseInsensitiveStringForPipelineName() {
        StageConfigIdentifier stageConfigIdentifier1 = new StageConfigIdentifier("pipelineName", "stageName");
        StageConfigIdentifier stageConfigIdentifier2 = new StageConfigIdentifier("pipelinename", "stageName");
        assertThat(stageConfigIdentifier1, is(stageConfigIdentifier2));
    }

    @Test
    public void shouldUseCaseInsensitiveStringForStageName() {
        StageConfigIdentifier stageConfigIdentifier1 = new StageConfigIdentifier("pipelineName", "stageName");
        StageConfigIdentifier stageConfigIdentifier2 = new StageConfigIdentifier("pipelineName", "Stagename");
        assertThat(stageConfigIdentifier1, is(stageConfigIdentifier2));
    }
}
