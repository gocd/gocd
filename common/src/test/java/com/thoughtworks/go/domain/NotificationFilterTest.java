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

import com.thoughtworks.go.util.GoConstants;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class NotificationFilterTest {

    @Test
    public void shouldMatchFixedStage() {
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Fixed, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Fixed), is(true));
    }

    @Test
    public void shouldMatchBrokenStage() {
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks), is(true));
    }

    @Test
    public void allEventShouldMatchAnyEvents() {
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.All, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks), is(true));
    }

    @Test
    public void shouldNotMatchStageWithDifferentPipeline() {
        NotificationFilter filter = new NotificationFilter("xyz", "dev", StageEvent.All, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.All), is(false));
    }

    @Test
    public void shouldNotMatchStageWithDifferentName() {
        NotificationFilter filter = new NotificationFilter("cruise", "xyz", StageEvent.All, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.All), is(false));
    }

    @Test
    public void filterWithAllEventShouldIncludeOthers() {
        assertThat(new NotificationFilter("cruise", "dev", StageEvent.All, false).include(
                new NotificationFilter("cruise", "dev", StageEvent.Fixed, false)), is(true));

    }

    @Test
    public void filterWithSameEventShouldIncludeOthers() {
        assertThat(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false).include(
                new NotificationFilter("cruise", "dev", StageEvent.Fixed, true)), is(true));

    }

    @Test
    public void anyPipelineShouldAlwaysMatch() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks), is(true));
    }

    @Test
    public void anyStageShouldAlwaysMatchWithinSamePipeline() {
        NotificationFilter filter = new NotificationFilter("cruise", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks), is(true));
    }

    @Test
    public void anyStageShouldNotMatchWithinADifferentPipeline() {
        NotificationFilter filter = new NotificationFilter("cruise", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise2", "dev"), StageEvent.Breaks), is(false));
    }

    @Test
    public void specificStageShouldMatchWithinAnyPipeline() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, "dev", StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise1", "dev"), StageEvent.Breaks), is(true));
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise2", "dev"), StageEvent.Breaks), is(true));
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise2", "not-dev"), StageEvent.Breaks), is(false));
    }

    @Test
    public void anyPipelineAndAnyStageShouldAlwaysApply() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev"), is(true));
    }

    @Test
    public void anyStageShouldAlwaysApply() {
        NotificationFilter filter = new NotificationFilter("cruise2", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev"), is(true));
    }

    @Test
    public void shouldNotApplyIfPipelineDiffers() {
        NotificationFilter filter = new NotificationFilter("cruise1", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev"), is(false));
    }

    @Test
    public void shouldNotApplyIfStageDiffers() {
        NotificationFilter filter = new NotificationFilter("cruise2", "devo", StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev"), is(false));
    }

    @Test
    public void specificStageShouldApplyToAnyPipeline() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, "dev", StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise1", "dev"), is(true));
        assertThat(filter.appliesTo("cruise2", "dev"), is(true));
        assertThat(filter.appliesTo("cruise2", "not-dev"), is(false));
    }
}
