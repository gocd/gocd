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
import com.thoughtworks.go.junit5.FileSource;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.List;

import static org.xmlunit.assertj.XmlAssert.assertThat;

public class JobPlanXmlRepresenterTest {
    @ParameterizedTest
    @FileSource(files = "/feeds/job-plan.xml")
    void shouldGenerateDocumentForJobPlan(String expectedXML) {
        EnvironmentVariable secureEnvVariable = new EnvironmentVariable("secureVariable", "value2", true);
        DefaultJobPlan jobPlan1 = JobInstanceMother.jobPlan("job-1", 1);
        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("some_var", "blah");
        variables.add(secureEnvVariable);
        jobPlan1.setVariables(variables);

        XmlWriterContext context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());

        Document document = new JobPlanXmlRepresenter(List.of(new WaitingJobPlan(jobPlan1, "envName")))
            .toXml(context);

        assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }
}
