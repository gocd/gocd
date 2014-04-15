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

package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.Arrays;

import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import static com.thoughtworks.go.helper.PipelineHistoryMother.pipeline;
import static com.thoughtworks.go.helper.PipelineHistoryMother.pipelineWithLatestRevision;
import static com.thoughtworks.go.helper.PipelineHistoryMother.pipelineWithLatestRevisionAndMaterialRevision;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;


public class EnvironmentTest {
    @Test
    public void shouldBeAbleToTellIfThereAreAnyPiplinesThatHaveNewMaterialRevisions() {
        MaterialRevisions latest = new MaterialRevisions();
        latest.addRevision(MaterialsMother.hgMaterial(), ModificationsMother.aCheckIn("21"));
        assertThat(new Environment("blah", Arrays.asList(pipeline(), pipelineWithLatestRevision(latest))).hasNewRevisions(),is(true));
        assertThat(new Environment("blah", Arrays.asList(pipeline(), pipelineWithLatestRevisionAndMaterialRevision(latest,latest))).hasNewRevisions(),is(false));
    }

}
