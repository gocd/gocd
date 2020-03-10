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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.junit5.JsonSource;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.junit.jupiter.params.ParameterizedTest;

import static com.thoughtworks.go.util.DateUtils.parseISO8601;
import static org.xmlunit.assertj.XmlAssert.assertThat;

public class StageXmlRepresenterTest {
    @ParameterizedTest
    @JsonSource(jsonFiles = "/feeds/stage.xml")
    void shouldGenerateDocumentForStage(String expectedXML) {
        String pipelineName = "BulletinBoard";
        String stageName = "UnitTest";
        String jobName = "run-junit";
        XmlWriterContext context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());
        Stage stage = StageMother.cancelledStage(stageName, jobName);
        stage.getJobInstances().get(0).setIdentifier(new JobIdentifier(pipelineName, 1, null, stageName, "1", jobName));
        stage.getJobInstances().get(0).getTransitions().first()
            .setStateChangeTime(parseISO8601("2020-01-03T11:14:19+05:30"));
        stage.setIdentifier(new StageIdentifier(pipelineName, 10, stage.getName(), "4"));

        Document document = new StageXmlRepresenter(stage).toXml(context);

        assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }
}
