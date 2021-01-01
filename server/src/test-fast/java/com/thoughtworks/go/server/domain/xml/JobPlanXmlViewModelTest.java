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

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class JobPlanXmlViewModelTest {

    @Test
    public void shouldConvertJobPlanToXmlDocument() throws IOException, DocumentException {
        EnvironmentVariable secureEnvVariable = new EnvironmentVariable("secureVariable", "value2", true);
        DefaultJobPlan jobPlan1 = JobInstanceMother.jobPlan("job-1", 1);
        jobPlan1.setJobId(10);
        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("some_var", "blah");
        variables.add(secureEnvVariable);
        jobPlan1.setVariables(variables);
        DefaultJobPlan jobPlan2 = JobInstanceMother.jobPlan("job-2", 1);
        jobPlan2.setJobId(11);

        JobPlanXmlViewModel jobPlanXmlViewModel = new JobPlanXmlViewModel(new ArrayList<>(Arrays.asList(new WaitingJobPlan(jobPlan1, "envName"), new WaitingJobPlan(jobPlan2, null))));

        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<scheduledJobs>"
                + "<job name=\"job-1\" id=\"10\">"
                + "<link rel=\"self\" href=\"http://baseurl/go/tab/build/detail/pipeline/1/stage/1/job-1\"/>"
                + "<buildLocator>pipeline/1/stage/1/job-1</buildLocator>"
                + "<environment>envName</environment>"
                + "<resources><resource><![CDATA[foo]]></resource><resource><![CDATA[bar]]></resource></resources>"
                + "<environmentVariables><variable name=\"some_var\">blah</variable><variable name=\"secureVariable\">****</variable></environmentVariables>"
                + "</job>"
                + "<job name=\"job-2\" id=\"11\">"
                + "<link rel=\"self\" href=\"http://baseurl/go/tab/build/detail/pipeline/1/stage/1/job-2\"/>"
                + "<buildLocator>pipeline/1/stage/1/job-2</buildLocator>"
                + "<resources><resource><![CDATA[foo]]></resource><resource><![CDATA[bar]]></resource></resources>"
                + "</job>"
                + "</scheduledJobs>";

        Document document = jobPlanXmlViewModel.toXml(new XmlWriterContext("http://baseurl/go", null, null, null, new SystemEnvironment()));
        assertEquals(expectedXml, document.asXML());
    }
}
