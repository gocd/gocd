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

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.PipelineHistoryMother;
import com.thoughtworks.go.junit5.JsonSource;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.xmlunit.assertj.XmlAssert;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialsMother.gitMaterial;
import static com.thoughtworks.go.util.DateUtils.parseISO8601;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xmlunit.assertj.XmlAssert.assertThat;

public class PipelineXmlRepresenterTest {

    private XmlWriterContext context;
    private PipelineInstanceModel model;

    @BeforeEach
    void setUp() {
        context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());
        Date scheduledDate = parseISO8601("2019-12-31T15:31:49+05:30");
        model = PipelineHistoryMother.pipelineInstanceModel("up42", 100, scheduledDate);
        GitMaterial gitMaterial = gitMaterial("https://material/example.git");
        gitMaterial.setId(100);
        MaterialRevision materialRevision = new MaterialRevision(gitMaterial, modifications());
        model.setLatestRevisions(new MaterialRevisions(materialRevision));
    }

    @ParameterizedTest
    @JsonSource(jsonFiles = "/feeds/pipeline.xml")
    void shouldGeneratePipelineXml(String expectedXML) {
        Document document = new PipelineXmlRepresenter(model).toXml(context);

        assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }

    @Test
    void shouldAddPipelineAfterLink() {
        PipelineTimelineEntry entry = mock(PipelineTimelineEntry.class);
        when(entry.getPipelineName()).thenReturn("up42");
        when(entry.getId()).thenReturn(101L);
        model.setPipelineAfter(entry);

        Document document = new PipelineXmlRepresenter(model).toXml(context);

        XmlAssert.assertThat(document.asXML())
            .nodesByXPath("//pipeline/link[@rel=\"insertedBefore\"]")
            .exist()
            .haveAttribute("href", "https://go-server/go/api/feed/pipelines/up42/101.xml");
    }

    @Test
    void shouldAddPipelineBeforeLink() {
        PipelineTimelineEntry entry = mock(PipelineTimelineEntry.class);
        when(entry.getPipelineName()).thenReturn("up42");
        when(entry.getId()).thenReturn(99L);
        model.setPipelineBefore(entry);

        Document document = new PipelineXmlRepresenter(model).toXml(context);

        XmlAssert.assertThat(document.asXML())
            .nodesByXPath("//pipeline/link[@rel=\"insertedAfter\"]")
            .exist()
            .haveAttribute("href", "https://go-server/go/api/feed/pipelines/up42/99.xml");
    }

    private List<Modification> modifications() {
        Date date = DateUtils.parseISO8601("2019-12-31T15:31:49+05:30");
        return Arrays.asList(
            modification(date, "Bob", "Adding build.xml", "3", "build.xml", ModifiedAction.added),
            modification(date, "Sam", "Fixing the not checked in files", "2", "tools/bin/go.jruby", ModifiedAction.added),
            modification(date, "Sam", "Adding .gitignore", "1", ".gitignore", ModifiedAction.modified)
        );
    }

    private Modification modification(Date date, String user, String comment, String revision, String filename, ModifiedAction action) {
        Modification modification = new Modification(user, comment, user.toLowerCase() + "@gocd.org", date, revision, null);
        modification.createModifiedFile(filename, null, action);
        return modification;
    }
}
