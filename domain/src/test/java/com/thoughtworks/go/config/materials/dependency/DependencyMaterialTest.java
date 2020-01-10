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
package com.thoughtworks.go.config.materials.dependency;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DependencyMaterialTest {
    private DependencyMaterial dependencyMaterial;

    @BeforeEach
    void setup() {
        dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
    }

    @Test
    void shouldReturnCruiseAsUser() {
        assertThat(dependencyMaterial.getUserName()).isEqualTo("cruise");
    }

    @Test
    void shouldReturnJson() {
        Map<String, String> json = new LinkedHashMap<>();
        dependencyMaterial.toJson(json, create("pipeline", 10, "1.0.123", "stage", 1));

        assertThat(json.get("location")).isEqualTo("pipeline/stage");
        assertThat(json.get("scmType")).isEqualTo("Dependency");
        assertThat(json.get("folder")).isEqualTo("");
        assertThat(json.get("action")).isEqualTo("Completed");
    }

    @Test
    void shouldDifferIfStageCounterHasChanged() {
        DependencyMaterialRevision rev1 = create("pipeline", 10, "1.0.123", "stage", 1);
        DependencyMaterialRevision rev2 = create("pipeline", 10, "1.0.123", "stage", 2);
        DependencyMaterialRevision rev3 = create("pipeline", 11, "1.0.123", "stage", 1);
        assertThat(rev1).isNotEqualTo(rev2);
        assertThat(rev2).isNotEqualTo(rev3);
        assertThat(rev3).isNotEqualTo(rev1);
    }

    @Test
    void shouldParseMaterialRevisionWithPipelineLabel() {
        ArrayList<Modification> mods = new ArrayList<>();
        Modification mod = new Modification(new Date(), "pipelineName/123/stageName/2", "pipeline-label-123", null);
        mods.add(mod);
        DependencyMaterialRevision revision = (DependencyMaterialRevision) new Modifications(mods).latestRevision(dependencyMaterial);
        assertThat(revision.getRevision()).isEqualTo("pipelineName/123/stageName/2");
        assertThat(revision.getPipelineLabel()).isEqualTo("pipeline-label-123");
        assertThat(revision.getPipelineCounter()).isEqualTo(123);
        assertThat(revision.getPipelineName()).isEqualTo("pipelineName");
        assertThat(revision.getStageName()).isEqualTo("stageName");
        assertThat(revision.getStageCounter()).isEqualTo(2);

    }

    @Test
    void shouldBeUniqueBasedOnpipelineAndStageName() throws Exception {
        DependencyMaterial material1 = new DependencyMaterial(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1"));
        Map<String, Object> map = new HashMap<>();
        material1.appendCriteria(map);
        assertThat(map).containsEntry("pipelineName", "pipeline1");
        assertThat(map).containsEntry("stageName", "stage1");
        assertThat(map.size()).isEqualTo(2);
    }

    @Test
    void shouldUsePipelineNameAsMaterialNameIfItIsNotSet() throws Exception {
        assertThat(new DependencyMaterial(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1")).getName()).isEqualTo(new CaseInsensitiveString("pipeline1"));
    }

    @Test
    void shouldUseMaterialNameAsMaterialNameIfItIsSet() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1"));
        material.setName(new CaseInsensitiveString("my-material-name"));
        assertThat(material.getName()).isEqualTo(new CaseInsensitiveString("my-material-name"));
    }

    @Test
    void shouldGenerateSqlCriteriaMapInSpecificOrder() throws Exception {
        Map<String, Object> map = dependencyMaterial.getSqlCriteria();
        assertThat(map.size()).isEqualTo(3);
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey()).isEqualTo("type");
        assertThat(iter.next().getKey()).isEqualTo("pipelineName");
        assertThat(iter.next().getKey()).isEqualTo("stageName");
    }

    @Test
    void equalsImplementation() throws Exception {
        DependencyMaterial one = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        DependencyMaterial two = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        two.setName(new CaseInsensitiveString("other-name-that-should-be-ignored-in-equals-comparison"));
        assertThat(one).isEqualTo(two);

        DependencyMaterial three = new DependencyMaterial(new CaseInsensitiveString("otherPipelineName"), new CaseInsensitiveString("stage"));
        assertThat(three).isNotEqualTo(one);
    }

    @Test
    void hashCodeImplementation() throws Exception {
        DependencyMaterial one = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        DependencyMaterial two = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        two.setName(new CaseInsensitiveString("other-name-that-should-be-ignored-in-hashcode-generation"));
        assertThat(one.hashCode()).isEqualTo(two.hashCode());

        DependencyMaterial three = new DependencyMaterial(new CaseInsensitiveString("otherPipelineName"), new CaseInsensitiveString("stage"));
        assertThat(three.hashCode()).isNotEqualTo(one.hashCode());
    }

    @Test
    void shouldReturnUpstreamPipelineNameAsDisplayNameIfMaterialNameIsNotDefined() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("first"));
        assertThat(material.getDisplayName()).isEqualTo("upstream");
    }

    @Test
    void shouldReturnMaterialNameIfDefined() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("first"));
        material.setName(new CaseInsensitiveString("my_name"));
        assertThat(material.getDisplayName()).isEqualTo("my_name");
    }

    @Test
    void shouldNotTruncateshortRevision() throws Exception {
        Material material = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("first"));
        assertThat(material.getShortRevision("pipeline-name/1/stage-name/5")).isEqualTo("pipeline-name/1/stage-name/5");
    }

    @Test
    void shouldUseACombinationOfPipelineAndStageNameAsURI() {
        Material material = new DependencyMaterial(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        assertThat(material.getUriForDisplay()).isEqualTo("pipeline-foo / stage-bar");
    }

    @Test
    void shouldDetectDependencyMaterialUsedInFetchArtifact() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        ArrayList<FetchTask> fetchTasks = new ArrayList<>();
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("something"), new CaseInsensitiveString("new"), "src", "dest"));
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"), new CaseInsensitiveString("job"), "src", "dest"));
        when(pipelineConfig.getFetchTasks()).thenReturn(fetchTasks);

        assertThat(material.isUsedInFetchArtifact(pipelineConfig)).isTrue();
    }

    @Test
    void shouldDetectDependencyMaterialUsedInFetchArtifactFromAncestor() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("parent-pipeline"), new CaseInsensitiveString("stage-bar"));
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        ArrayList<FetchTask> fetchTasks = new ArrayList<>();
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("grandparent-pipeline/parent-pipeline"), new CaseInsensitiveString("grandparent-stage"), new CaseInsensitiveString("grandparent-job"), "src", "dest"));
        when(pipelineConfig.getFetchTasks()).thenReturn(fetchTasks);

        assertThat(material.isUsedInFetchArtifact(pipelineConfig)).isTrue();
    }

    @Test
    void shouldDetectDependencyMaterialNotUsedInFetchArtifact() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        ArrayList<FetchTask> fetchTasks = new ArrayList<>();
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("something"), new CaseInsensitiveString("new"), "src", "dest"));
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("another"), new CaseInsensitiveString("boo"), new CaseInsensitiveString("foo"), "src", "dest"));
        when(pipelineConfig.getFetchTasks()).thenReturn(fetchTasks);

        assertThat(material.isUsedInFetchArtifact(pipelineConfig)).isFalse();
    }

    @Test
    void shouldGetAttributesAllFields() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        Map<String, Object> attributesWithSecureFields = material.getAttributes(true);
        assertAttributes(attributesWithSecureFields);

        Map<String, Object> attributesWithoutSecureFields = material.getAttributes(false);
        assertAttributes(attributesWithoutSecureFields);
    }


    @Test
    void shouldHandleNullOriginDuringValidationWhenUpstreamPipelineDoesNotExist() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream_stage"), new CaseInsensitiveString("upstream_pipeline"), new CaseInsensitiveString("stage"));
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipeline.setOrigin(null);
        dependencyMaterialConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), pipeline));
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).isEqualTo("Pipeline with name 'upstream_pipeline' does not exist, it is defined as a dependency for pipeline 'p' (cruise-config.xml)");
    }

    @Test
    void shouldHandleNullOriginDuringValidationWhenUpstreamStageDoesNotExist() {
        CruiseConfig cruiseConfig = GoConfigMother.pipelineHavingJob("upstream_pipeline", "upstream_stage", "j1", null, null);
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream_pipeline"), new CaseInsensitiveString("does_not_exist"));
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString("downstream"), new MaterialConfigs());
        pipeline.setOrigin(null);
        dependencyMaterialConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, pipeline));
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).isEqualTo("Stage with name 'does_not_exist' does not exist on pipeline 'upstream_pipeline', it is being referred to from pipeline 'downstream' (cruise-config.xml)");
    }


    private void assertAttributes(Map<String, Object> attributes) {
        assertThat(attributes.get("type")).isEqualTo("pipeline");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("pipeline-configuration");
        assertThat(configuration.get("pipeline-name")).isEqualTo("pipeline-name");
        assertThat(configuration.get("stage-name")).isEqualTo("stage-name");
    }

    @Test
    void shouldReturnFalseForDependencyMaterial_supportsDestinationFolder() throws Exception {
        DependencyMaterial material = new DependencyMaterial();
        assertThat(material.supportsDestinationFolder()).isFalse();
    }
}
