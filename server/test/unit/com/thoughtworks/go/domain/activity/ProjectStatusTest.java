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

package com.thoughtworks.go.domain.activity;

import java.util.Date;

import com.thoughtworks.go.util.DateUtils;
import static org.hamcrest.core.Is.is;
import org.jdom.Element;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class ProjectStatusTest {
    @Test
    public void shouldGetCcTrayStatusxml() throws Exception {
        String projectName = "projectName";
        String activity = "Building";
        String lastBuildStatus = "Success";
        String lastBuildLabel = "LastBuildLabel";
        Date lastBuildTime = new Date();
        String webUrl = "weburl";
        String contextPath = "http://localhost/go";

        ProjectStatus projectStatus = new ProjectStatus(projectName, activity, lastBuildStatus, lastBuildLabel,
                lastBuildTime, webUrl);

        Element element = projectStatus.ccTrayXmlElement(contextPath);

        assertThat(element.getName(), is("Project"));
        assertThat(element.getAttributeValue("name"), is(projectName));
        assertThat(element.getAttributeValue("activity"), is(activity));
        assertThat(element.getAttributeValue("lastBuildStatus"), is(lastBuildStatus));
        assertThat(element.getAttributeValue("lastBuildLabel"), is(lastBuildLabel));
        assertThat(element.getAttributeValue("lastBuildTime"), is(DateUtils.formatIso8601ForCCTray(lastBuildTime)));
        assertThat(element.getAttributeValue("webUrl"), is(contextPath + "/" + webUrl));

    }

}



