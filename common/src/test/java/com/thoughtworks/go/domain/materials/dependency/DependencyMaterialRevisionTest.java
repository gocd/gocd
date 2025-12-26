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
package com.thoughtworks.go.domain.materials.dependency;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyMaterialRevisionTest {

    @Test
    public void shouldRenderPipelineLabelToRenderer() {
        DependencyMaterialRevision revision = DependencyMaterialRevision.create("pipeline", 50, "1.0.123", "stage", 1);
        assertThat(revision.getRevisionUrl()).isEqualTo("pipelines/pipeline/50/stage/1");
    }

    @Test
    public void shouldReturnRevisionForSavingIntoDatabase() {
        DependencyMaterialRevision revision = DependencyMaterialRevision.create("pipeline", 2, "1.0.123", "stage", 1);
        assertThat(revision.getRevision()).isEqualTo("pipeline/2/stage/1");
        assertThat(revision.getPipelineLabel()).isEqualTo("1.0.123");
    }

    @Test
    public void shouldConvertToTheCounterBasedRevision() {
        DependencyMaterialRevision materialRevision = DependencyMaterialRevision.create("pipeline", 10, "1.2.3", "stage", 4);

        MaterialRevision withRevision = materialRevision.convert(new DependencyMaterial(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage")), new Date());
        DependencyMaterialRevision revision = (DependencyMaterialRevision) withRevision.getRevision();
        assertThat(revision.getRevision()).isEqualTo("pipeline/10/stage/4");
        assertThat(revision.getPipelineLabel()).isEqualTo("1.2.3");
    }

    @Test
    public void shouldAddPipelineLabelAsRevisionForMaterial() {
        DependencyMaterialRevision materialRevision = DependencyMaterialRevision.create("pipeline", 10, "foo-1.2.3", "stage", 4);
        Map<String, String> revMap = new HashMap<>();
        materialRevision.putRevision(revMap);
        assertThat(revMap.get("pipeline")).isEqualTo("foo-1.2.3");
    }
}
