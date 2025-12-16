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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GoConfigParallelGraphWalkerTest {

    @Test
    public void shouldWalkCruiseConfigObjectsParallelly() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("bad pipeline name");
        CruiseConfig rawCruiseConfig = GoConfigMother.deepClone(cruiseConfig);
        MagicalGoConfigXmlLoader.validate(cruiseConfig);
        Validatable.copyErrors(cruiseConfig, rawCruiseConfig);
        assertThat(rawCruiseConfig.pipelineConfigByName(new CaseInsensitiveString("bad pipeline name")).errors())
            .isEqualTo(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("bad pipeline name")).errors());
    }

    @Test
    public void shouldHandlePipelinesWithTemplates() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipeline-1");
        cruiseConfig.getTemplates().add(
            new PipelineTemplateConfig(new CaseInsensitiveString("template-1"), new StageConfig(new CaseInsensitiveString("invalid stage name"), new JobConfigs(new JobConfig("job-1"))
            )));
        PipelineConfig pipelineWithTemplate = new PipelineConfig(new CaseInsensitiveString("pipeline-with-template"), MaterialConfigsMother.defaultMaterialConfigs());
        pipelineWithTemplate.setTemplateName(new CaseInsensitiveString("template-1"));
        cruiseConfig.getGroups().get(0).add(pipelineWithTemplate);

        CruiseConfig rawCruiseConfig = GoConfigMother.deepClone(cruiseConfig);
        MagicalGoConfigXmlLoader.validate(cruiseConfig);
        Validatable.copyErrors(cruiseConfig, rawCruiseConfig);
        assertThat(rawCruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template")).errors().isEmpty()).isTrue();
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template")).getStage(new CaseInsensitiveString("invalid stage name")).errors().isEmpty()).isFalse();

        assertThat(rawCruiseConfig.getTemplateByName(new CaseInsensitiveString("template-1")).errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldAddErrorsToRawCruiseConfigWhenTemplateHasErrors() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipeline-1");
        cruiseConfig.getTemplates().add(
            new PipelineTemplateConfig(new CaseInsensitiveString("invalid template name"), new StageConfig(new CaseInsensitiveString("stage-1"), new JobConfigs(new JobConfig("job-1"))
            )));
        PipelineConfig pipelineWithTemplate = new PipelineConfig(new CaseInsensitiveString("pipeline-with-template"), MaterialConfigsMother.defaultMaterialConfigs());
        pipelineWithTemplate.setTemplateName(new CaseInsensitiveString("invalid template name"));
        cruiseConfig.getGroups().get(0).add(pipelineWithTemplate);

        CruiseConfig rawCruiseConfig = GoConfigMother.deepClone(cruiseConfig);
        MagicalGoConfigXmlLoader.validate(cruiseConfig);
        Validatable.copyErrors(cruiseConfig, rawCruiseConfig);

        ConfigErrors templateErrors = rawCruiseConfig.getTemplateByName(new CaseInsensitiveString("invalid template name")).errors();
        assertThat(templateErrors.getAll().size()).isEqualTo(1);
        assertThat(templateErrors.getAll().get(0)).isEqualTo("Invalid template name 'invalid template name'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldNotTryAndCopyErrorsFromAnObjectWithANullField() {
        AValidatableObjectWithAField badObjectWithNullField = new AValidatableObjectWithAField(null);
        badObjectWithNullField.addError("blah", "Bad");

        AValidatableObjectWithAField goodObjectWithNonNullField = new AValidatableObjectWithAField(new SomeOtherObject("1"));

        Validatable.copyErrors(badObjectWithNullField, goodObjectWithNonNullField);

        assertThat(goodObjectWithNonNullField.errors().getAll().size()).isEqualTo(1);
        assertThat(goodObjectWithNonNullField.errors().firstError()).isEqualTo("Bad");
    }

    @Test
    public void shouldCopyErrorsToCorrectObjectBasedOnEqualityRatherThanIndex() {
        AValidatableObjectWithAList badObjectWith2ObjectsInList = new AValidatableObjectWithAList();
        SomeOtherObject soo1 = new SomeOtherObject("1");
        soo1.addError("a", "b");
        badObjectWith2ObjectsInList.add(soo1);
        SomeOtherObject soo2 = new SomeOtherObject("2");
        soo2.addError("x", "y");
        badObjectWith2ObjectsInList.add(soo2);

        AValidatableObjectWithAList goodObjectWith2ObjectsInList = new AValidatableObjectWithAList();
        goodObjectWith2ObjectsInList.add(new SomeOtherObject("2"));

        Validatable.copyErrors(badObjectWith2ObjectsInList, goodObjectWith2ObjectsInList);

        assertThat(goodObjectWith2ObjectsInList.getSomeOtherObjectList().size()).isEqualTo(1);
        assertThat(goodObjectWith2ObjectsInList.getSomeOtherObjectList().get(0).errors().getAll().size()).isEqualTo(1);
        assertThat(goodObjectWith2ObjectsInList.getSomeOtherObjectList().get(0).errors().firstError()).isEqualTo("y");
    }

    @Test
    public void shouldCopyErrorsForFieldsOnPipelineConfig() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", MaterialConfigsMother.defaultMaterialConfigs(), new JobConfigs(JobConfigMother.createJobConfigWithJobNameAndEmptyResources()));
        pipelineConfig.setVariables(new EnvironmentVariablesConfig(List.of(new EnvironmentVariableConfig("name", "value"))));

        PipelineConfig pipelineWithErrors = GoConfigMother.deepClone(pipelineConfig);
        pipelineWithErrors.getVariables().get(0).addError("name", "error on environment variable");
        pipelineWithErrors.first().addError("name", "error on stage");
        pipelineWithErrors.first().getJobs().first().addError("name", "error on job");
        Validatable.copyErrors(pipelineWithErrors, pipelineConfig);
        assertThat(pipelineConfig.getVariables().get(0).errors().firstErrorOn("name")).isEqualTo("error on environment variable");
        assertThat(pipelineConfig.first().errors().firstErrorOn("name")).isEqualTo("error on stage");
        assertThat(pipelineConfig.first().getJobs().first().errors().firstErrorOn("name")).isEqualTo("error on job");
    }

    private static class AValidatableObjectWithAList implements Validatable {
        private final ConfigErrors configErrors = new ConfigErrors();
        @SuppressWarnings("FieldMayBeFinal")
        private SomeOtherObjectList someOtherObjectList = new SomeOtherObjectList();

        @Override
        public void validate(ValidationContext validationContext) {
        }

        @Override
        public ConfigErrors errors() {
            return configErrors;
        }

        @Override
        public void addError(String fieldName, String message) {
            configErrors.add(fieldName, message);
        }

        public void add(SomeOtherObject soo) {
            someOtherObjectList.add(soo);
        }

        public List<SomeOtherObject> getSomeOtherObjectList() {
            return someOtherObjectList;
        }
    }

    private static class SomeOtherObjectList extends BaseCollection<SomeOtherObject> {
    }

    private static class AValidatableObjectWithAField implements Validatable {
        private final ConfigErrors configErrors = new ConfigErrors();
        @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
        private SomeOtherObject someOtherObject;

        private AValidatableObjectWithAField(SomeOtherObject someOtherObject) {
            this.someOtherObject = someOtherObject;
        }

        @Override
        public void validate(ValidationContext validationContext) {
        }

        @Override
        public ConfigErrors errors() {
            return configErrors;
        }

        @Override
        public void addError(String fieldName, String message) {
            configErrors.add(fieldName, message);
        }
    }

    private static class SomeOtherObject implements Validatable {
        private final ConfigErrors configErrors = new ConfigErrors();
        private final String id;

        private SomeOtherObject(String id) {
            this.id = id;
        }

        @Override
        public void validate(ValidationContext validationContext) {
        }

        @Override
        public ConfigErrors errors() {
            return configErrors;
        }

        @Override
        public void addError(String fieldName, String message) {
            configErrors.add(fieldName, message);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SomeOtherObject that = (SomeOtherObject) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }
}
