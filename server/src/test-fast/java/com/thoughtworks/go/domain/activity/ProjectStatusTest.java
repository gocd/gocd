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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.util.DateUtils;
import org.jdom2.Element;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

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

    @Test
    public void shouldListViewers() throws Exception {
        Users viewers = mock(Users.class);

        ProjectStatus status = new ProjectStatus("name", "activity", "web-url");
        status.updateViewers(viewers);

        assertThat(status.viewers(), is(viewers));
    }

    @Test
    public void shouldProvideItsXmlRepresentation_WhenThereAreNoBreakers() throws Exception {
        ProjectStatus status = new ProjectStatus("name", "activity1", "build-status-1", "build-label-1",
                DateUtils.parseRFC822("Sun, 23 May 2010 10:00:00 +0200"), "web-url");

        assertThat(status.xmlRepresentation(),
                is("<Project name=\"name\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" " +
                        "lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"__SITE_URL_PREFIX__/web-url\" />"));
    }

    @Test
    public void shouldProvideItsXmlRepresentation_WhenThereAreBreakers() throws Exception {
        ProjectStatus status = new ProjectStatus("name", "activity1", "build-status-1", "build-label-1",
                DateUtils.parseRFC822("Sun, 23 May 2010 10:00:00 +0200"), "web-url", s("breaker1", "breaker2"));

        assertThat(status.xmlRepresentation(),
                is("<Project name=\"name\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" " +
                        "lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"__SITE_URL_PREFIX__/web-url\">" +
                        "<messages><message text=\"breaker1, breaker2\" kind=\"Breakers\" /></messages></Project>"));
    }

    @Test
    public void shouldAlwaysHaveEmptyStringAsXMLRepresentationOfANullProjectStatus() throws Exception {
        assertThat(new ProjectStatus.NullProjectStatus("some-name").xmlRepresentation(), is(""));
        assertThat(new ProjectStatus.NullProjectStatus("some-other-name").xmlRepresentation(), is(""));
    }

    @Test
    public void shouldNotBeViewableByAnyoneTillViewersAreUpdated() throws Exception {
        ProjectStatus status = new ProjectStatus("name", "activity", "web-url");

        assertThat(status.canBeViewedBy("abc"), is(false));
        assertThat(status.canBeViewedBy("def"), is(false));

        status.updateViewers(new AllowedUsers(s("abc", "ghi"), Collections.emptySet()));

        assertThat(status.canBeViewedBy("abc"), is(true));
        assertThat(status.canBeViewedBy("def"), is(false));
        assertThat(status.canBeViewedBy("ghi"), is(true));
    }
}
