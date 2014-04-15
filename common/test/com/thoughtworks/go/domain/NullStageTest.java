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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@Ignore
public class NullStageTest {
    private final StageConfig twoPlanStageConfig =
            StageConfigMother.twoBuildPlansWithResourcesAndMaterials("test-stage");

    @Test
    public void shouldCreateWithSameNumberOfNullBuildInstancesAsBuildPlansInStage() {
        NullStage nullPipeline = NullStage.createNullStage(twoPlanStageConfig);
        JobInstances nullInstances = nullPipeline.getJobInstances();
        assertThat(nullInstances.size(), is(2));
        assertThat(nullInstances.get(0).getName(), is("WinBuild"));
        assertThat(nullInstances.get(1).getName(), is("NixBuild"));
    }

    @Test
    public void shouldAlwaysReturnOtherInstanceForMostRecent() {
        Stage nullStage = NullStage.createNullStage(twoPlanStageConfig);
        Stage realStage = StageMother.completedStageInstanceWithTwoPlans("real");
        assertThat(realStage.mostRecent(nullStage), is(realStage));
        assertThat(nullStage.mostRecent(realStage), is(realStage));
        assertThat(realStage.mostRecent(realStage), is(realStage));
    }
}
