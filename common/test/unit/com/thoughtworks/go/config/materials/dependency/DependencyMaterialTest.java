/*
 * Copyright 2015 ThoughtWorks, Inc.
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
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision.create;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependencyMaterialTest {
    private DependencyMaterial dependencyMaterial;

    @Before
    public void setup() {
        dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
    }

    @Test
    public void shouldReturnCruiseAsUser() {
        assertThat(dependencyMaterial.getUserName(), is("cruise"));
    }

    @Test
    public void shouldReturnJson() {
        Map<String, String> json = new LinkedHashMap<>();
        dependencyMaterial.toJson(json, create("pipeline", 10, "1.0.123", "stage", 1));

        assertThat(json.get("location"), is("pipeline/stage"));
        assertThat(json.get("scmType"), is("Dependency"));
        assertThat(json.get("folder"), is(""));
        assertThat(json.get("action"), is("Completed"));
    }

    @Test
    public void shouldDifferIfStageCounterHasChanged() {
        DependencyMaterialRevision rev1 = DependencyMaterialRevision.create("pipeline", 10, "1.0.123", "stage", 1);
        DependencyMaterialRevision rev2 = DependencyMaterialRevision.create("pipeline", 10, "1.0.123", "stage", 2);
        DependencyMaterialRevision rev3 = DependencyMaterialRevision.create("pipeline", 11, "1.0.123", "stage", 1);
        assertThat(rev1, is(not(rev2)));
        assertThat(rev2, is(not(rev3)));
        assertThat(rev3, is(not(rev1)));
    }

    @Test
    public void shouldParseMaterialRevisionWithPipelineLabel() {
        ArrayList<Modification> mods = new ArrayList<Modification>();
        Modification mod = new Modification(new Date(), "pipelineName/123/stageName/2", "pipeline-label-123", null);
        mods.add(mod);
        DependencyMaterialRevision revision = (DependencyMaterialRevision) new Modifications(mods).latestRevision(dependencyMaterial);
        assertThat(revision.getRevision(), is("pipelineName/123/stageName/2"));
        assertThat(revision.getPipelineLabel(), is("pipeline-label-123"));
        assertThat(revision.getPipelineCounter(), is(123));
        assertThat(revision.getPipelineName(), is("pipelineName"));
        assertThat(revision.getStageName(), is("stageName"));
        assertThat(revision.getStageCounter(), is(2));

    }

    @Test public void shouldBeUniqueBasedOnpipelineAndStageName() throws Exception {
        DependencyMaterial material1 = new DependencyMaterial(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1"));
        Map<String, Object> map = new HashMap<String, Object>();
        material1.appendCriteria(map);
        assertThat(map, hasEntry("pipelineName", (Object) "pipeline1"));
        assertThat(map, hasEntry("stageName", (Object) "stage1"));
        assertThat(map.size(), is(2));
    }

    @Test public void shouldUsePipelineNameAsMaterialNameIfItIsNotSet() throws Exception {
        assertThat(new DependencyMaterial(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1")).getName(), is(new CaseInsensitiveString("pipeline1")));
    }

    @Test public void shouldUseMaterialNameAsMaterialNameIfItIsSet() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1"));
        material.setName(new CaseInsensitiveString("my-material-name"));
        assertThat(material.getName(), is(new CaseInsensitiveString("my-material-name")));
    }

    @Test public void shouldGenerateSqlCriteriaMapInSpecificOrder() throws Exception {
        Map<String, Object> map = dependencyMaterial.getSqlCriteria();
        assertThat(map.size(), is(3));
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey(), is("type"));
        assertThat(iter.next().getKey(), is("pipelineName"));
        assertThat(iter.next().getKey(), is("stageName"));
    }

    @Test public void equalsImplementation() throws Exception {
        DependencyMaterial one = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        DependencyMaterial two = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        two.setName(new CaseInsensitiveString("other-name-that-should-be-ignored-in-equals-comparison"));
        assertEquals(two, one);

        DependencyMaterial three = new DependencyMaterial(new CaseInsensitiveString("otherPipelineName"), new CaseInsensitiveString("stage"));
        assertNotEquals(one, three);
    }

    @Test public void hashCodeImplementation() throws Exception {
        DependencyMaterial one = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        DependencyMaterial two = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stage"));
        two.setName(new CaseInsensitiveString("other-name-that-should-be-ignored-in-hashcode-generation"));
        assertEquals(two.hashCode(), one.hashCode());

        DependencyMaterial three = new DependencyMaterial(new CaseInsensitiveString("otherPipelineName"), new CaseInsensitiveString("stage"));
        assertNotEquals(one.hashCode(), three.hashCode());
    }

    @Test public void shouldReturnUpstreamPipelineNameAsDisplayNameIfMaterialNameIsNotDefined() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("first"));
        assertThat(material.getDisplayName(), is("upstream"));
    }

    @Test public void shouldReturnMaterialNameIfDefined() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("first"));
        material.setName(new CaseInsensitiveString("my_name"));
        assertThat(material.getDisplayName(), is("my_name"));
    }

    @Test public void shouldNotTruncateshortRevision() throws Exception {
        Material material = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("first"));
        assertThat(material.getShortRevision("pipeline-name/1/stage-name/5"), is("pipeline-name/1/stage-name/5"));
    }

    @Test
    public void shouldUseACombinationOfPipelineAndStageNameAsURI() {
        Material material = new DependencyMaterial(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        assertThat(material.getUriForDisplay(), is("pipeline-foo / stage-bar"));
    }

    @Test
    public void shouldDetectDependencyMaterialUsedInFetchArtifact() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        ArrayList<FetchTask> fetchTasks = new ArrayList<FetchTask>();
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("something"), new CaseInsensitiveString("new"), "src", "dest"));
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"), new CaseInsensitiveString("job"), "src", "dest"));
        when(pipelineConfig.getFetchTasks()).thenReturn(fetchTasks);

        assertThat(material.isUsedInFetchArtifact(pipelineConfig), is(true));
    }

    @Test
    public void shouldDetectDependencyMaterialUsedInFetchArtifactFromAncestor() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("parent-pipeline"), new CaseInsensitiveString("stage-bar"));
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        ArrayList<FetchTask> fetchTasks = new ArrayList<FetchTask>();
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("grandparent-pipeline/parent-pipeline"), new CaseInsensitiveString("grandparent-stage"), new CaseInsensitiveString("grandparent-job"), "src", "dest"));
        when(pipelineConfig.getFetchTasks()).thenReturn(fetchTasks);

        assertThat(material.isUsedInFetchArtifact(pipelineConfig), is(true));
    }

    @Test
    public void shouldDetectDependencyMaterialNotUsedInFetchArtifact() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        ArrayList<FetchTask> fetchTasks = new ArrayList<FetchTask>();
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("something"), new CaseInsensitiveString("new"), "src", "dest"));
        fetchTasks.add(new FetchTask(new CaseInsensitiveString("another"), new CaseInsensitiveString("boo"),new CaseInsensitiveString("foo"), "src", "dest"));
        when(pipelineConfig.getFetchTasks()).thenReturn(fetchTasks);

        assertThat(material.isUsedInFetchArtifact(pipelineConfig), is(false));
    }

    @Test
    public void shouldGetAttributesAllFields() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        Map<String, Object> attributesWithSecureFields = material.getAttributes(true);
        assertAttributes(attributesWithSecureFields);

        Map<String, Object> attributesWithoutSecureFields = material.getAttributes(false);
        assertAttributes(attributesWithoutSecureFields);
    }


    @Test
    public void shouldHandleNullOriginDuringValidationWhenUpstreamPipelineDoesNotExist() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream_stage"), new CaseInsensitiveString("upstream_pipeline"), new CaseInsensitiveString("stage"));
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipeline.setOrigin(null);
        dependencyMaterialConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new BasicCruiseConfig(), pipeline));
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME), is("Pipeline with name 'upstream_pipeline' does not exist, it is defined as a dependency for pipeline 'p' (cruise-config.xml)"));
    }

    @Test
    public void shouldHandleNullOriginDuringValidationWhenUpstreamStageDoesNotExist() {
        CruiseConfig cruiseConfig = GoConfigMother.pipelineHavingJob("upstream_pipeline", "upstream_stage", "j1", null, null);
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream_pipeline"), new CaseInsensitiveString("does_not_exist"));
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString("downstream"), new MaterialConfigs());
        pipeline.setOrigin(null);
        dependencyMaterialConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", cruiseConfig, pipeline));
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME), is("Stage with name 'does_not_exist' does not exist on pipeline 'upstream_pipeline', it is being referred to from pipeline 'downstream' (cruise-config.xml)"));
    }
    

    private void assertAttributes(Map<String, Object> attributes) {
        assertThat((String) attributes.get("type"), is("pipeline"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("pipeline-configuration");
        assertThat((String) configuration.get("pipeline-name"), is("pipeline-name"));
        assertThat((String) configuration.get("stage-name"), is("stage-name"));
    }

    @Test
    public void shouldReturnFalseForDependencyMaterial_supportsDestinationFolder() throws Exception {
        DependencyMaterial material = new DependencyMaterial();
        assertThat(material.supportsDestinationFolder(), is(false));
    }
}
