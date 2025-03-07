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

package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.junit5.FileSource;
import org.dom4j.Document;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xmlunit.assertj.XmlAssert.assertThat;

public class JobXmlRepresenterTest {
    @ParameterizedTest
    @FileSource(files = "/feeds/job.xml")
    void shouldGenerateDocumentForJob(String expectedXML) throws IllegalArtifactLocationException {
        ArtifactUrlReader urlReader = mock(ArtifactUrlReader.class);
        JobInstance jobInstance = JobInstanceMother.failed("unit-test");
        JobIdentifier identifier = new JobIdentifier("up42", 1, "UP42", "unit-tests", "1", "unit");
        jobInstance.setIdentifier(identifier);
        JobPlanLoader jobPlanLoader = mock(JobPlanLoader.class);
        JobPlan jobPlan = mock(JobPlan.class);

        when(urlReader.findArtifactUrl(identifier)).thenReturn("artifact-url");
        when(urlReader.findArtifactRoot(identifier)).thenReturn("artifact-root-url");
        when(jobPlanLoader.loadOriginalJobPlan(jobInstance.getIdentifier())).thenReturn(jobPlan);
        when(jobPlan.getArtifactPlansOfType(ArtifactPlanType.unit)).thenReturn(List.of(
            new ArtifactPlan(ArtifactPlanType.unit, "source", "destination")
        ));

        XmlWriterContext context = new XmlWriterContext("https://go-server/go", urlReader, jobPlanLoader);

        Document document = new JobXmlRepresenter(jobInstance).toXml(context);

        assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }
}
