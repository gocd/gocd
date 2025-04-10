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
package com.thoughtworks.go.server.service.dd;

import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class FanInNodeFactoryTest {
    @Test
    public void shouldCreateRootNodeForScmMaterial() {
        MaterialConfig material = MaterialConfigsMother.svnMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material);
        assertThat(node instanceof RootFanInNode).isTrue();
        assertThat(node.materialConfig).isEqualTo(material);
    }

    @Test
    public void shouldCreateRootNodeForPackageMaterial() {
        MaterialConfig material = MaterialConfigsMother.packageMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material);
        assertThat(node instanceof RootFanInNode).isTrue();
        assertThat(node.materialConfig).isEqualTo(material);
    }

    @Test
    public void shouldCreateRootNodeForPluggableSCMMaterial() {
        MaterialConfig material = MaterialConfigsMother.pluggableSCMMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material);
        assertThat(node instanceof RootFanInNode).isTrue();
        assertThat(node.materialConfig).isEqualTo(material);
    }

    @Test
    public void shouldCreateDependencyNodeForScmMaterial() {
        MaterialConfig material = MaterialConfigsMother.dependencyMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material);
        assertThat(node instanceof DependencyFanInNode).isTrue();
        assertThat(node.materialConfig).isEqualTo(material);
    }

    @Test
    public void shouldNotCreateNodeForUnregisteredMaterials() {
        try {
            FanInNodeFactory.create(new SomeRandomMaterialConfig());
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Not a valid material type");
        }
    }
}
