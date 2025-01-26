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
        assertEquals("single element list", dupe.get(0));
        assertEquals(dupe, original);
    }

    @Test
    public void shouldNotCloneAllPipelineConfigs() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        config.getAllPipelineConfigs();
        //change state
        config.findGroup("defaultGroup").remove(0);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat((Object) ReflectionUtil.getField(config, "allPipelineConfigs")).isNotNull();
        assertThat((Object) ReflectionUtil.getField(cloned, "allPipelineConfigs")).isNull();
        assertThat(cloned.getAllPipelineConfigs().size()).isEqualTo(1);
    }

    @Test
    public void shouldNotCloneAllTemplatesWithAssociatedPipelines() {
        PipelineTemplateConfig template = PipelineTemplateConfigMother.createTemplate("template-1");
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        config.addTemplate(template);
        config.addPipelineWithoutValidation("g1", PipelineConfigMother.pipelineConfigWithTemplate("p1", template.name().toString()));
        //to prime cache
        config.templatesWithAssociatedPipelines();
        //change state
        config.findGroup("g1").remove(0);
        config.getTemplates().removeTemplateNamed(template.name());
        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat((Object) ReflectionUtil.getField(config, "allTemplatesWithAssociatedPipelines")).isNotNull();
        assertThat((Object) ReflectionUtil.getField(cloned, "allTemplatesWithAssociatedPipelines")).isNull();
        assertThat(cloned.templatesWithAssociatedPipelines().size()).isEqualTo(0);
    }

    @Test
    public void shouldNotClonePipelineNameToConfigMap() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        config.pipelineConfigByName(new CaseInsensitiveString("p1"));
        //change state
        config.findGroup("defaultGroup").remove(0);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat((Object) ReflectionUtil.getField(config, "pipelineNameToConfigMap")).isNotNull();
        assertThat((Object) ReflectionUtil.getField(cloned, "pipelineNameToConfigMap")).isNull();
        assertThat(cloned.pipelineConfigsAsMap().size()).isEqualTo(1);
    }

    @Test
    public void shouldNotCloneCachedArtifactConfigs() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        config.getAllPipelineConfigs();
        //change state
        config.encryptSecureProperties(config);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat((Object) ReflectionUtil.getField(config.getAllPipelineConfigs().get(0), "externalArtifactConfigs")).isEqualTo(new ArrayList<>());
        assertThat((Object) ReflectionUtil.getField(cloned.getAllPipelineConfigs().get(0), "externalArtifactConfigs")).isNull();
    }

    @Test
    public void shouldNotCloneCachedFetchPluggableArtifactTasks() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        //to prime cache
        config.getAllPipelineConfigs();
        //change state
        config.encryptSecureProperties(config);

        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat((Object) ReflectionUtil.getField(config.getAllPipelineConfigs().get(0), "fetchExternalArtifactTasks")).isEqualTo(new ArrayList<>());
        assertThat((Object) ReflectionUtil.getField(cloned.getAllPipelineConfigs().get(0), "fetchExternalArtifactTasks")).isNull();
    }

    @Test
    public void shouldDeepCloneObject() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("p1", "p2");
        BasicCruiseConfig cloned = new GoConfigCloner().deepClone(config);
        assertThat(cloned.getGroups().size()).isEqualTo(1);
        assertThat(cloned.getGroups().get(0).getPipelines().size()).isEqualTo(2);
    }
}
