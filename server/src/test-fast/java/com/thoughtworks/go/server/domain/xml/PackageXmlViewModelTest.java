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
package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.DateUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageXmlViewModelTest {
    @Test
    public void shouldPopulateModificationDetailsForPackageMaterial() {
        String userName = "user";
        String comment = "comments";
        Date checkinDate = new Date(System.currentTimeMillis());
        String revision = "package-1.0.0.rpm";
        Element modificationsTag = DocumentHelper.createDocument().addElement("modifications");
        Modifications modifications = new Modifications(new Modification(userName, comment, null, checkinDate, revision));

        XmlWriterContext writerContext = mock(XmlWriterContext.class);
        when(writerContext.getBaseUrl()).thenReturn("http://someurl:8153/go");
        new PipelineXmlViewModel.PackageXmlViewModel(MaterialsMother.packageMaterial()).populateXmlForModifications(modifications, writerContext, modificationsTag);
        Element changeSet = modificationsTag.element("changeset");
        assertThat(changeSet, is(not(nullValue())));
        assertThat(changeSet.attributeValue("changesetUri"), is("http://someurl:8153/go/api/materials/1/changeset/package-1.0.0.rpm.xml"));
        assertThat(changeSet.element("user").getText(), is(userName));
        assertThat(changeSet.element("revision").getText(), is(revision));
        assertThat(changeSet.element("checkinTime").getText(), is(DateUtils.formatISO8601(checkinDate)));
        assertThat(changeSet.element("message").getText(), is(comment));
    }
}
