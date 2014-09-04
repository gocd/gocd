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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.StageMother;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DependencyMaterialUpdaterTest {

    @Test
    public void shouldGenerateCacheKeyWithLowercaseForDependencyMaterial() {
        String key = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(new DependencyMaterial(new CaseInsensitiveString("PIPELINE_123"), new CaseInsensitiveString("STAGE_123")));
        assertThat(key, is(String.format(DependencyMaterialUpdater.DEPENDENCY_MATERIAL_CACHE_KEY_FORMAT, "pipeline_123", "stage_123")));
    }

    @Test
    public void shouldGenerateCacheKeyWithLowercaseForStage() {
        Stage stage = new Stage();
        stage.setIdentifier(new StageIdentifier("PIPEline_123", 3, "3", "STAge_123", "1"));

        String key = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(stage);
        assertThat(key, is(String.format(DependencyMaterialUpdater.DEPENDENCY_MATERIAL_CACHE_KEY_FORMAT, "pipeline_123", "stage_123")));
    }

   @Test
    public void cacheKeyForDependencyMaterial_shouldReturnKeyEqualTo_cacheKeyForDependencyMaterial() {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        String cacheKeyFromMaterial = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(dependencyMaterial);
        String cacheKeyFromStage = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(StageMother.completedFailedStageInstance("pipeline-name", "stage-name", "does-not-matter"));

        assertSame(cacheKeyFromStage, cacheKeyFromMaterial);
    }

}