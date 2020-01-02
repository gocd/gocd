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

import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.helper.MaterialsMother;
import org.dom4j.tree.DefaultElement;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class PipelineXmlViewModelTest {

    @Test
    public void shouldReturnProperXMLWhenTherAreNullAttributesOnAMaterial() {
        PipelineXmlViewModel.ScmXmlViewModel model = new PipelineXmlViewModel.ScmXmlViewModel(MaterialsMother.svnMaterial("url", "folder", null, null, false, null));
        DefaultElement materials = new DefaultElement("materials");

        model.populateXml(materials, new Modifications(), mock(XmlWriterContext.class));

        assertThat(materials.selectSingleNode("./material/@username"), is(nullValue()));
    }
}
