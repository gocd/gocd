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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class StageIdentifierTest {

    @Test
    public void shouldContainCounterIfStageHasRerun() {
        StageIdentifier identifier = new StageIdentifier("cruise", null, "label", "dev", "2");
        assertThat(identifier.ccTrayLastBuildLabel(), is("label :: 2"));
    }

    @Test
    public void shouldNotContainCounterForFirstRun() {
        StageIdentifier identifier = new StageIdentifier("cruise", null, "label", "dev", "1");
        assertThat(identifier.ccTrayLastBuildLabel(), is("label"));
    }

    @Test
    public void shouldConstructFromStageLocator() {
        StageIdentifier identifier = new StageIdentifier("pipeline-name/10/stage-name/7");
        assertThat(identifier.getPipelineName(), is("pipeline-name"));
        assertThat(identifier.getStageName(), is("stage-name"));
        assertThat(identifier.getPipelineCounter(), is(10));
        assertThat(identifier.getStageCounter(), is("7"));
    }

    @Test
    public void shouldThrowExceptionIfStageCounterIsNotNumber() {
        StageIdentifier identifier = new StageIdentifier("cruise", null, "label", "dev", "");
        try {
            identifier.ccTrayLastBuildLabel();
            Assert.fail("should throw exception if stage counter is not number");
        } catch (Exception e) {
            assertThat(e, instanceOf(NumberFormatException.class));
        }
    }

    @Test
    public void pipelineStagesWithSameCountersAndDifferentlabelShouldBeEqual() {
        StageIdentifier stage1 = new StageIdentifier("blahPipeline", 1, "blahLabel", "blahStage", "1");
        StageIdentifier stage2 = new StageIdentifier("blahPipeline", 1, "fooLabel", "blahStage", "1");
        StageIdentifier stage3 = new StageIdentifier("blahPipeline", 1, "blahStage", "1");
        StageIdentifier stage4 = new StageIdentifier("blahPipeline", 1, "blahStage", "2");

        assertThat(stage1,is(stage2));
        assertThat(stage1,is(stage3));
        assertThat(stage2,is(stage3));
        assertThat(stage2,is(not(stage4)));
    }


    @Test
    public void shouldReturnURN() throws Exception {
        StageIdentifier id = new StageIdentifier("cruise", 1, "dev", "1");
        assertThat(id.asURN(), is("urn:x-go.studios.thoughtworks.com:stage-id:cruise:1:dev:1"));
    }
}
