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

package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.JobInstanceMother;
import org.dom4j.DocumentException;
import org.dom4j.dom.DOMDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobXmlViewModelTest {

    private XmlWriterContext xmlWriterContext;
    private JobXmlViewModel jobXmlViewModel;
    private JobPlan jobPlan;
    private JobInstance defaultJob;

    @Before
    public void setUp() {
        xmlWriterContext = mock(XmlWriterContext.class);
        jobPlan = mock(JobPlan.class);
        defaultJob = JobInstanceMother.completed("defaultJob");
        when(xmlWriterContext.planFor(defaultJob.getIdentifier())).thenReturn(jobPlan);
        when(xmlWriterContext.propertiesForJob(defaultJob.getId())).thenReturn(new Properties());
        jobXmlViewModel = new JobXmlViewModel(defaultJob);
    }

    @Test
    public void shouldMaskSecureVariables() throws IOException, DocumentException {
        EnvironmentVariable envVariable = new EnvironmentVariable("stdVariable", "value1", false);
        EnvironmentVariable secureEnvVariable = new EnvironmentVariable("secureVariable", "value2", true);

        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.add(envVariable);
        environmentVariables.add(secureEnvVariable);
        when(jobPlan.getVariables()).thenReturn(environmentVariables);
        when(jobPlan.getResources()).thenReturn(new Resources());

        DOMDocument document = (DOMDocument) jobXmlViewModel.toXml(xmlWriterContext);

        Assert.assertThat(document.asXML(), containsString(
                "<environmentvariables><variable name=\"stdVariable\"><![CDATA[value1]]></variable><variable name=\"secureVariable\"><![CDATA[****]]></variable></environmentvariables>"));

    }
}
