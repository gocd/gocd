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
import com.thoughtworks.go.junit5.JsonSource;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xmlunit.assertj.XmlAssert;

import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels.createPipelineInstanceModels;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelinesXmlRepresenterTest {
    @ParameterizedTest
    @JsonSource(jsonFiles = "/feeds/pipelines.xml")
    void shouldConvertPipelineInstanceModelsToDocument(String expectedXML) {
        XmlWriterContext context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());
        PipelineInstanceModel up42Model = pipelineInstanceModel("up42");
        PipelineInstanceModel up43Model = pipelineInstanceModel("up43");
        PipelineInstanceModels models = createPipelineInstanceModels(up42Model, up43Model);
        PipelinesXmlRepresenter representer = new PipelinesXmlRepresenter(models);

        Document document = representer.toXml(context);

        XmlAssert.assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }

    @Test
    void shouldGenerateXmlWithRootElementAndSelfLinkWhenPipelineInstanceModelsIsEmpty() {
        XmlWriterContext context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());
        PipelineInstanceModels models = createPipelineInstanceModels();
        PipelinesXmlRepresenter representer = new PipelinesXmlRepresenter(models);

        Document document = representer.toXml(context);

        String expectedXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<pipelines>\n" +
            "  <link rel=\"self\" href=\"https://go-server/go/api/feed/pipelines.xml\"/>\n" +
            "</pipelines>";

        XmlAssert.assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }

    private static PipelineInstanceModel pipelineInstanceModel(String name) {
        PipelineInstanceModel pipelineInstanceModel = mock(PipelineInstanceModel.class);
        when(pipelineInstanceModel.getName()).thenReturn(name);
        return pipelineInstanceModel;
    }
}
