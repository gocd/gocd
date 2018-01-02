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

package com.thoughtworks.go.config;

import java.util.List;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GoConfigParallelGraphWalkerTest {

    @Test
    public void shouldWalkCruiseConfigObjectsParallelly() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("bad pipeline name");
        CruiseConfig rawCruiseConfig = new Cloner().deepClone(cruiseConfig);
        MagicalGoConfigXmlLoader.validate(cruiseConfig);
        cruiseConfig.copyErrorsTo(rawCruiseConfig);
        assertThat(rawCruiseConfig.pipelineConfigByName(new CaseInsensitiveString("bad pipeline name")).errors(),
                is(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("bad pipeline name")).errors()));
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

        CruiseConfig rawCruiseConfig = new Cloner().deepClone(cruiseConfig);
        MagicalGoConfigXmlLoader.validate(cruiseConfig);
        cruiseConfig.copyErrorsTo(rawCruiseConfig);
        assertThat(rawCruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template")).errors().isEmpty(), is(true));
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-with-template")).getStage(new CaseInsensitiveString("invalid stage name")).errors().isEmpty(), is(false));

        assertThat(rawCruiseConfig.getTemplateByName(new CaseInsensitiveString("template-1")).errors().isEmpty(), is(true));
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

        CruiseConfig rawCruiseConfig = new Cloner().deepClone(cruiseConfig);
        MagicalGoConfigXmlLoader.validate(cruiseConfig);
        cruiseConfig.copyErrorsTo(rawCruiseConfig);

        ConfigErrors templateErrors = rawCruiseConfig.getTemplateByName(new CaseInsensitiveString("invalid template name")).errors();
        assertThat(templateErrors.getAll().size(), is(1));
        assertThat(templateErrors.getAll().get(0),
                is("Invalid template name 'invalid template name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldNotTryAndCopyErrorsFromAnObjectWithANullField() throws Exception {
        AValidatableObjectWithAField badObjectWithNullField = new AValidatableObjectWithAField(null);
        badObjectWithNullField.addError("blah", "Bad");

        AValidatableObjectWithAField goodObjectWithNonNullField = new AValidatableObjectWithAField(new SomeOtherObject("1"));

        BasicCruiseConfig.copyErrors(badObjectWithNullField, goodObjectWithNonNullField);

        assertThat(goodObjectWithNonNullField.errors().getAll().size(), is(1));
        assertThat(goodObjectWithNonNullField.errors().firstError(), is("Bad"));
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

        BasicCruiseConfig.copyErrors(badObjectWith2ObjectsInList, goodObjectWith2ObjectsInList);

        assertThat(goodObjectWith2ObjectsInList.getSomeOtherObjectList().size(), is(1));
        assertThat(goodObjectWith2ObjectsInList.getSomeOtherObjectList().get(0).errors().getAll().size(), is(1));
        assertThat(goodObjectWith2ObjectsInList.getSomeOtherObjectList().get(0).errors().firstError(), is("y"));
    }

    @Test
    public void shouldCopyErrorsForFieldsOnPipelineConfig(){
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline", MaterialConfigsMother.defaultMaterialConfigs(), new JobConfigs(JobConfigMother.createJobConfigWithJobNameAndEmptyResources()));
        pipelineConfig.setVariables(new EnvironmentVariablesConfig(asList(new EnvironmentVariableConfig("name", "value"))));

        PipelineConfig pipelineWithErrors = new Cloner().deepClone(pipelineConfig);
        pipelineWithErrors.getVariables().get(0).addError("name", "error on environment variable");
        pipelineWithErrors.first().addError("name", "error on stage");
        pipelineWithErrors.first().getJobs().first().addError("name", "error on job");
        BasicCruiseConfig.copyErrors(pipelineWithErrors, pipelineConfig);
        assertThat(pipelineConfig.getVariables().get(0).errors().on("name"), is("error on environment variable"));
        assertThat(pipelineConfig.first().errors().on("name"), is("error on stage"));
        assertThat(pipelineConfig.first().getJobs().first().errors().on("name"), is("error on job"));
    }

    private class AValidatableObjectWithAList implements Validatable {
        private ConfigErrors configErrors = new ConfigErrors();
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

    private class SomeOtherObjectList extends BaseCollection<SomeOtherObject> {
    }

    private class AValidatableObjectWithAField implements Validatable {
        private ConfigErrors configErrors = new ConfigErrors();
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

    private class SomeOtherObject implements Validatable {
        private ConfigErrors configErrors = new ConfigErrors();
        private String id;

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
