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
package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.MaterialsMother;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class MaterialXmlViewModelTest {

    @Test
    public void shouldReturnPackageMaterialXmlViewModelIfUsingPackageMaterial() {
        PipelineXmlViewModel.MaterialXmlViewModel model = PipelineXmlViewModel.MaterialXmlViewModel.viewModelFor(MaterialsMother.packageMaterial());
        assertThat(model instanceof PipelineXmlViewModel.PackageXmlViewModel, is(true));
    }

    @Test
    public void shouldThrowExceptionWhenMaterialTypeIsUnknown() {
        try {
            PipelineXmlViewModel.MaterialXmlViewModel.viewModelFor(mock(Material.class));
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Unknown material type"));
        }
    }
}

