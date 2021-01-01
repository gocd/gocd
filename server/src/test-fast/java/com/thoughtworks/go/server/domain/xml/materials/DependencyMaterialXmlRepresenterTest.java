/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.domain.xml.materials;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.junit5.FileSource;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.dom.DOMElement;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xmlunit.assertj.XmlAssert.assertThat;

public class DependencyMaterialXmlRepresenterTest {
    @ParameterizedTest
    @FileSource(files = "/feeds/materials/dependency-material.xml")
    void shouldRepresentDependencyMaterial(String expectedXML) {
        Date date = DateUtils.parseISO8601("2019-12-31T15:31:49+05:30");
        DependencyMaterial material = MaterialsMother.dependencyMaterial();
        material.setId(60);
        MaterialRevision revision = new MaterialRevision(material, new Modification(date, "acceptance/63/twist-plugins/2", null, null));
        DOMElement root = new DOMElement("materials");
        ElementBuilder builder = new ElementBuilder(root);
        StageFinder stageFinder = getStageFinder(100L);
        XmlWriterContext context = new XmlWriterContext("https://test.host/go", null, null, stageFinder, new SystemEnvironment());

        new DependencyMaterialXmlRepresenter("up42", 1, revision).populate(builder, context);

        assertThat(root.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }

    private StageFinder getStageFinder(long id) {
        StageFinder stageFinder = mock(StageFinder.class);
        Stage stage = mock(Stage.class);
        when(stage.getId()).thenReturn(id);
        when(stageFinder.findStageWithIdentifier(any(StageIdentifier.class))).thenReturn(stage);
        return stageFinder;
    }
}
