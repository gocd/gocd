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

package com.thoughtworks.go.server.service.dd;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FanInNodeFactoryTest {

    @Mock
    private CruiseConfig cruiseConfig;

    @Before
    public void setup() {
        cruiseConfig = mock(CruiseConfig.class);
    }

    @Test
    public void shouldCreateRootNodeForScmMaterial() {
        MaterialConfig material = MaterialConfigsMother.svnMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material, cruiseConfig);
        assertThat(node instanceof RootFanInNode, is(true));
        assertThat(node.materialConfig, is(material));
    }

    @Test
    public void shouldCreateRootNodeForPackageMaterial() {
        MaterialConfig material = MaterialConfigsMother.packageMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material, cruiseConfig);
        assertThat(node instanceof RootFanInNode, is(true));
        assertThat(node.materialConfig, is(material));
    }

    @Test
    public void shouldCreateRootNodeForPluggableSCMMaterial() {
        MaterialConfig material = MaterialConfigsMother.pluggableSCMMaterialConfig();
        FanInNode node = FanInNodeFactory.create(material, cruiseConfig);
        assertThat(node instanceof RootFanInNode, is(true));
        assertThat(node.materialConfig, is(material));
    }

    @Test
    public void shouldCreateDependencyNodeForScmMaterial() {
        MaterialConfig material = MaterialConfigsMother.dependencyMaterialConfig();
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        when(pipelineConfig.materialConfigs()).thenReturn(new MaterialConfigs());
        when(cruiseConfig.pipelineConfigByName(((DependencyMaterialConfig)material).getPipelineName())).thenReturn(pipelineConfig);
        FanInNode node = FanInNodeFactory.create(material, cruiseConfig);
        assertThat(node instanceof DependencyFanInNode, is(true));
        assertThat(node.materialConfig, is(material));
    }

    @Test
    public void shouldNotCreateNodeForUnregisteredMaterials() {
        try {
            FanInNodeFactory.create(new SomeRandomMaterialConfig(), cruiseConfig);
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Not a valid material type"));
        }
    }
}
