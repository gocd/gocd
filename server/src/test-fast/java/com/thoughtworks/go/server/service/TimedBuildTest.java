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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class TimedBuildTest {

    @Test
    public void shouldReturnEmptyBuildCauseIfThereIsNoModification_whenTriggeringOnlyForMaterialChange() throws Exception {
        MaterialRevisions someRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.gitMaterial("git://url"), ModificationsMother.aCheckIn("1", "file1.txt")));

        BuildType timedBuild = new TimedBuild();
        PipelineConfig timerConfig = PipelineConfigMother.pipelineConfigWithTimer("Timer", "* * * * * ?", true);
        BuildCause buildCause = timedBuild.onEmptyModifications(timerConfig, someRevisions);

        assertThat(buildCause, is(nullValue()));
    }

    @Test
    public void shouldReturnAForcedBuildCauseIfThereIsNoModification_whenTriggeringIrrespectiveOfMaterialChange() throws Exception {
        MaterialRevisions someRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.gitMaterial("git://url"), ModificationsMother.aCheckIn("1", "file1.txt")));

        BuildType timedBuild = new TimedBuild();
        PipelineConfig timerConfig = PipelineConfigMother.pipelineConfigWithTimer("Timer", "* * * * * ?", false);
        BuildCause buildCause = timedBuild.onEmptyModifications(timerConfig, someRevisions);

        assertThat(buildCause.getMaterialRevisions(), is(someRevisions));
        assertThat(buildCause.getApprover(), is(Username.CRUISE_TIMER.getDisplayName()));
    }
}
