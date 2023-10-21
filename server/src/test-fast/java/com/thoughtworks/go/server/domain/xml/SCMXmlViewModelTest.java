/*
 * Copyright 2023 Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.DateUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SCMXmlViewModelTest {
    @Test
    public void shouldPopulateModificationDetailsForPluggableSCMMaterial() {
        String userName = "user";
        String comment = "comment-1";
        Date checkinDate = new Date(System.currentTimeMillis());
        String revision = "revision-1";
        Modification modification = new Modification(userName, comment, null, checkinDate, revision);
        modification.setModifiedFiles(List.of(new ModifiedFile("f1", null, ModifiedAction.added), new ModifiedFile("f2", null, ModifiedAction.deleted)));
        Modifications modifications = new Modifications(modification);

        XmlWriterContext writerContext = mock(XmlWriterContext.class);
        when(writerContext.getBaseUrl()).thenReturn("http://someurl:8153/go");
        Element modificationsTag = DocumentHelper.createDocument().addElement("modifications");
        new PipelineXmlViewModel.ScmXmlViewModel(MaterialsMother.pluggableSCMMaterial()).populateXmlForModifications(modifications, writerContext, modificationsTag);

        Element changeSet = modificationsTag.element("changeset");

        assertThat(changeSet, is(not(nullValue())));
        assertThat(changeSet.attributeValue("changesetUri"), is("http://someurl:8153/go/api/materials/1/changeset/revision-1.xml"));
        assertThat(changeSet.element("user").getText(), is(userName));
        assertThat(changeSet.element("revision").getText(), is(revision));
        assertThat(changeSet.element("checkinTime").getText(), is(DateUtils.formatISO8601(checkinDate)));
        assertThat(changeSet.element("message").getText(), is(comment));
        Element file1 = changeSet.elements("file").get(0);
        assertThat(file1.attributeValue("name"), is("f1"));
        assertThat(file1.attributeValue("action"), is("added"));
        Element file2 = changeSet.elements("file").get(1);
        assertThat(file2.attributeValue("name"), is("f2"));
        assertThat(file2.attributeValue("action"), is("deleted"));
    }
}
