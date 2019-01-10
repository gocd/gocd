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

package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.util.DateUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StageCctrayPresentationModelTest {

    private StageCctrayPresentationModel cctrayXmlPresenter;

    private static final Date DATE = new Date();
    private static final String DATE_STR = DateUtils.formatIso8601ForCCTray(DATE);
    private static final int ID = 123;
    private static final String LABEL = "build.123";
    private static final int STAGE_ID = 8;
    private static final String CONTEXT_PATH = "http://localhost:8153/go";
    private static final int BUILD_ID = 3;

    @Before public void setUp() throws Exception {
        Stage stage = StageMother.passedStageInstance("cruise", "ft", "firefox", DATE);
        stage.setId(STAGE_ID);
        stage.setCompletedByTransitionId(100L);
        stage.getJobInstances().getByName("firefox").setId(BUILD_ID);

        Pipeline pipeline = PipelineMother.pipeline("cruise", stage);
        pipeline.setId(ID); //used to mock pipeline label
        pipeline.setLabel(LABEL); //used to mock pipeline label
        cctrayXmlPresenter = new StageCctrayPresentationModel(pipeline, stage);
    }

    @Test public void shouldGetGoodCctrayXml() throws Exception {
        Element root = new Element("Projects");
        cctrayXmlPresenter.toCctrayXml(root, CONTEXT_PATH);
        assertThat(root.getChildren().size(), is(2));
        shouldContainStage(root);
        shouldContainBuilds(root);
    }

    private void shouldContainStage(Element root) {
        Element stageProject = findChildByName(root, "cruise :: ft");
        assertThat(stageProject, hasAttribute("activity", "Sleeping"));
        assertThat(stageProject, hasAttribute("lastBuildStatus", "Success"));
        assertThat(stageProject, hasAttribute("lastBuildLabel", String.valueOf(LABEL)));
        assertThat(stageProject, hasAttribute("lastBuildTime", DATE_STR));
        assertThat(stageProject, hasAttribute("webUrl", stageDetailUrl()));
    }

    private void shouldContainBuilds(Element root) {
        Element buildProject = findChildByName(root, "cruise :: ft :: firefox");
        assertThat(buildProject, hasAttribute("activity", "Sleeping"));
        assertThat(buildProject, hasAttribute("lastBuildStatus", "Success"));
        assertThat(buildProject, hasAttribute("lastBuildLabel", String.valueOf(LABEL)));
        assertThat(buildProject, hasAttribute("lastBuildTime", DATE_STR));
        assertThat(buildProject, hasAttribute("webUrl", buildDetailUrl()));
    }

    @Test public void stageFailingShouldBeTreatedAsCompleted() throws Exception {
        final Stage stage = mock(Stage.class);
        when(stage.stageState()).thenReturn(StageState.Failing);
        StageCctrayPresentationModel presenter = new StageCctrayPresentationModel(null, stage);
        assertThat(presenter.stageActivity(), is("Sleeping"));
    }

    private static String stageDetailUrl() {
        return CONTEXT_PATH + "/pipelines/" + STAGE_ID;
    }

    private static String buildDetailUrl() {
        return CONTEXT_PATH + "/tab/build/detail/" + BUILD_ID;
    }

    public static HasAttributeMatcher hasAttribute(String attrName, String attrValue) {
        return new HasAttributeMatcher(attrName, attrValue);
    }

    private static final class HasAttributeMatcher extends TypeSafeMatcher<Element> {
        private final String attrName;
        private final String attrValue;
        private String actualAttributeValue;

        public HasAttributeMatcher(String attrName, String attrValue) {
            this.attrName = attrName;
            this.attrValue = attrValue;
        }

        public void describeTo(Description description) {
            description.appendText(
                    "should has attribute [name=" + attrName + ", value=" + attrValue + "], actual attribute is: "
                            + actualAttributeValue);
        }

        public boolean matchesSafely(Element item) {
            actualAttributeValue = item.getAttributeValue(attrName);
            return attrValue.equals(actualAttributeValue);
        }
    }

    private static Element findChildByName(Element parent, String childName) {
        for (Object o : parent.getChildren()) {
            Element child = (Element) o;
            if (childName.equals(child.getAttributeValue("name"))) {
                return child;
            }
        }
        return null;
    }
}

