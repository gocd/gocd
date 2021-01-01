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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.materials.NullRevision;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PipelineInstanceModelRealRevisionTest {

    @Test public void shouldPublishItselfAsARealRevision() throws Exception {
        assertThat(new StringRevision("foo").isRealRevision(), is(true));
        assertThat(DependencyMaterialRevision.create("blahPipeline",5,"blahLabel","blahStage",2).isRealRevision(), is(true));
        assertThat(new NullRevision().isRealRevision(), is(false));
        assertThat(PipelineInstanceModel.UNKNOWN_REVISION.isRealRevision(), is(false));
    }
}
