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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.GoConstants;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFilterTest {
    @Mock
    private ValidationContext validationContext;

    @Test
    void shouldMatchFixedStage() {
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Fixed, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Fixed)).isTrue();
    }

    @Test
    void shouldMatchBrokenStage() {
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks)).isTrue();
    }

    @Test
    void allEventShouldMatchAnyEvents() {
        NotificationFilter filter = new NotificationFilter("cruise", "dev", StageEvent.All, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks)).isTrue();
    }

    @Test
    void shouldNotMatchStageWithDifferentPipeline() {
        NotificationFilter filter = new NotificationFilter("xyz", "dev", StageEvent.All, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.All)).isFalse();
    }

    @Test
    void shouldNotMatchStageWithDifferentName() {
        NotificationFilter filter = new NotificationFilter("cruise", "xyz", StageEvent.All, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.All)).isFalse();
    }

    @Test
    void filterWithAllEventShouldIncludeOthers() {
        assertThat(new NotificationFilter("cruise", "dev", StageEvent.All, false).include(
            new NotificationFilter("cruise", "dev", StageEvent.Fixed, false))).isTrue();

    }

    @Test
    void filterWithSameEventShouldIncludeOthers() {
        assertThat(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false).include(
            new NotificationFilter("cruise", "dev", StageEvent.Fixed, true))).isTrue();

    }

    @Test
    void anyPipelineShouldAlwaysMatch() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks)).isTrue();
    }

    @Test
    void anyStageShouldAlwaysMatchWithinSamePipeline() {
        NotificationFilter filter = new NotificationFilter("cruise", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise", "dev"), StageEvent.Breaks)).isTrue();
    }

    @Test
    void anyStageShouldNotMatchWithinADifferentPipeline() {
        NotificationFilter filter = new NotificationFilter("cruise", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise2", "dev"), StageEvent.Breaks)).isFalse();
    }

    @Test
    void specificStageShouldMatchWithinAnyPipeline() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, "dev", StageEvent.Breaks, false);
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise1", "dev"), StageEvent.Breaks)).isTrue();
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise2", "dev"), StageEvent.Breaks)).isTrue();
        assertThat(filter.matchStage(new StageConfigIdentifier("cruise2", "not-dev"), StageEvent.Breaks)).isFalse();
    }

    @Test
    void anyPipelineAndAnyStageShouldAlwaysApply() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev")).isTrue();
    }

    @Test
    void anyStageShouldAlwaysApply() {
        NotificationFilter filter = new NotificationFilter("cruise2", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev")).isTrue();
    }

    @Test
    void shouldNotApplyIfPipelineDiffers() {
        NotificationFilter filter = new NotificationFilter("cruise1", GoConstants.ANY_STAGE, StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev")).isFalse();
    }

    @Test
    void shouldNotApplyIfStageDiffers() {
        NotificationFilter filter = new NotificationFilter("cruise2", "devo", StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise2", "dev")).isFalse();
    }

    @Test
    void specificStageShouldApplyToAnyPipeline() {
        NotificationFilter filter = new NotificationFilter(GoConstants.ANY_PIPELINE, "dev", StageEvent.Breaks, false);
        assertThat(filter.appliesTo("cruise1", "dev")).isTrue();
        assertThat(filter.appliesTo("cruise2", "dev")).isTrue();
        assertThat(filter.appliesTo("cruise2", "not-dev")).isFalse();
    }

    @Nested
    class Validate {
        @Test
        void shouldBeValidIfPipelineNameIsSetToAnyPipeline() {
            NotificationFilter filter = new NotificationFilter("[Any Pipeline]", null, null, true);

            filter.validate(validationContext);

            assertThat(filter.errors()).isEmpty();
        }

        @Test
        void shouldErrorOutWhenPipelineWithNameDoesNotExist() {
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("up42");
            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
            NotificationFilter filter = new NotificationFilter("Unknown", null, null, true);

            filter.validate(validationContext);

            assertThat(filter.errors()).hasSize(1)
                .containsEntry("pipelineName", List.of("Pipeline with name 'Unknown' was not found!"));
        }

        @Test
        void shouldBeValidIfPipelineWithNameExistAndStageIsSetToAnyStage() {
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("up42");
            NotificationFilter filter = new NotificationFilter("up42", "[Any Stage]", null, true);
            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);

            filter.validate(validationContext);

            assertThat(filter.errors()).isEmpty();
        }

        @Test
        void shouldErrorOutWhenPipelineWithNameExistAndStageDoesNotExist() {
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig("up42", "defaultStage"));
            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
            NotificationFilter filter = new NotificationFilter("up42", "unit-tests", null, true);

            filter.validate(validationContext);

            assertThat(filter.errors()).hasSize(1)
                .containsEntry("stageName", List.of("Stage 'unit-tests' not found in pipeline 'up42'!"));
        }

        @Test
        void shouldBeValidWhenPipelineAndStage() {
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig("up42", "unit-tests"));
            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
            NotificationFilter filter = new NotificationFilter("up42", "unit-tests", null, true);

            filter.validate(validationContext);

            assertThat(filter.errors()).isEmpty();
        }
    }
}
