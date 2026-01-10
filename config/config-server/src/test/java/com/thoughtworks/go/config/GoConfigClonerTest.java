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

import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineTemplateConfigMother;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GoConfigClonerTest {
    @Test
    public void fixesApplied_canCloneList12() {
        final List<String> original = List.of("single element list");
        final List<String> dupe = new GoConfigCloner().deepClone(original);
        assertEquals(1, dupe.size());
        assertEquals("single element list", dupe.getFirst());
        assertEquals(dupe, original);
    }

    @Test
    public void shouldNotCloneAllPipelineConfigs() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        assertThat(config.getAllPipelineConfigs()).hasSize(2);
        //change state
        config.findGroup("defaultGroup").add(PipelineConfigMother.pipelineConfig("p3"));

        assertThat(config.getAllPipelineConfigs()).describedAs("cache should be stale").hasSize(2);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(ReflectionUtil.<Object>getField(config, "allPipelineConfigs")).isNotNull();
        assertThat(ReflectionUtil.<Object>getField(cloned, "allPipelineConfigs")).isNull();
        assertThat(cloned.getAllPipelineConfigs()).hasSize(3);
    }

    @Test
    public void shouldNotCloneAllTemplatesWithAssociatedPipelines() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template-1");
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        config.addTemplate(template);
        PipelineConfig p1 = PipelineConfigMother.pipelineConfigWithTemplate("p1", template.name().toString());
        config.addPipelineWithoutValidation("g1", p1);
        //to prime cache
        assertThat(config.templatesWithAssociatedPipelines())
            .hasSize(1).values().hasSize(1);
        //change state
        config.deletePipeline(p1);
        config.getTemplates().removeTemplateNamed(template.name());

        assertThat(config.templatesWithAssociatedPipelines())
            .describedAs("cache should be stale")
            .hasSize(1).values().hasSize(1);;

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(ReflectionUtil.<Object>getField(config, "allTemplatesWithAssociatedPipelines")).isNotNull();
        assertThat(ReflectionUtil.<Object>getField(cloned, "allTemplatesWithAssociatedPipelines")).isNull();
        assertThat(cloned.templatesWithAssociatedPipelines()).isEmpty();
    }

    @Test
    public void shouldNotClonePipelineNameToConfigMap() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        assertThat(config.pipelineConfigByName(new CaseInsensitiveString("p1"))).isNotNull();
        config.pipelineConfigByName(new CaseInsensitiveString("p1"));
        //change state
        config.findGroup("defaultGroup").add(PipelineConfigMother.pipelineConfig("p3"));

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(ReflectionUtil.<Object>getField(config, "pipelineNameToConfigMap")).isNotNull();
        assertThat(ReflectionUtil.<Object>getField(cloned, "pipelineNameToConfigMap")).isNull();
        assertThat(cloned.pipelineConfigsAsMap()).hasSize(3);
    }

    @Test
    public void shouldNotCloneCachedArtifactConfigs() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        config.getAllPipelineConfigs();
        //change state
        config.encryptSecureProperties(config);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(ReflectionUtil.<Object>getField(config.getAllPipelineConfigs().getFirst(), "externalArtifactConfigs")).isEqualTo(new ArrayList<>());
        assertThat(ReflectionUtil.<Object>getField(cloned.getAllPipelineConfigs().getFirst(), "externalArtifactConfigs")).isNull();
    }

    @Test
    public void shouldNotCloneCachedFetchPluggableArtifactTasks() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        config.getAllPipelineConfigs();
        //change state
        config.encryptSecureProperties(config);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(ReflectionUtil.<Object>getField(config.getAllPipelineConfigs().getFirst(), "fetchExternalArtifactTasks")).isEqualTo(new ArrayList<>());
        assertThat(ReflectionUtil.<Object>getField(cloned.getAllPipelineConfigs().getFirst(), "fetchExternalArtifactTasks")).isNull();
    }

    @Test
    public void shouldDeepCloneObject() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(cloned.getGroups().size()).isEqualTo(1);
        assertThat(cloned.getGroups().getFirst().getPipelines().size()).isEqualTo(2);
    }
}
