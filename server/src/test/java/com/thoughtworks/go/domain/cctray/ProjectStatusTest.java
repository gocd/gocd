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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.util.Dates;
import org.jdom2.Element;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.domain.cctray.ProjectStatus.Key.keyFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectStatusTest {
    @Test
    public void shouldGetCcTrayStatusXml() {
        ProjectStatus projectStatus = dummyStatus();
        String contextPath = "http://localhost/go";
        Element element = projectStatus.ccTrayXmlElement(contextPath);

        assertThat(element.getName()).isEqualTo("Project");
        assertThat(element.getAttributeValue("name")).isEqualTo("name");
        assertThat(element.getAttributeValue("activity")).isEqualTo("activity");
        assertThat(element.getAttributeValue("lastBuildStatus")).isEqualTo("Success");
        assertThat(element.getAttributeValue("lastBuildLabel")).isEqualTo("1");
        assertThat(element.getAttributeValue("lastBuildTime")).isEqualTo(Dates.formatIso8601ForCCTray(projectStatus.getLastBuildTime()));
        assertThat(element.getAttributeValue("webUrl")).isEqualTo(contextPath + "/web-url");
    }

    @Test
    public void shouldListViewers() {
        Users viewers = mock(Users.class);

        ProjectStatus status = dummyStatus();
        status.updateViewers(viewers);

        assertThat(status.viewers()).isEqualTo(viewers);
    }

    @Test
    public void shouldProvideItsXmlRepresentation_WhenThereAreNoBreakers() {
        ProjectStatus status = new ProjectStatus(keyFrom("name"), 0, "activity1", "build-status-1", "build-label-1",
                Dates.parseIso8601StrictOffset("2010-05-23T10:00:00+02:00"), "web-url");

        assertThat(status.xmlRepresentation()).isEqualTo("<Project name=\"name\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" " +
                        "lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"__SITE_URL_PREFIX__/web-url\" />");
    }

    @Test
    public void shouldProvideItsXmlRepresentation_WhenThereAreBreakers() {
        ProjectStatus status = new ProjectStatus(keyFrom("name"), 0, "activity1", "build-status-1", "build-label-1",
                Dates.parseIso8601StrictOffset("2010-05-23T10:00:00+02:00"), "web-url", new LinkedHashSet<>(List.of("breaker1", "breaker2")));

        assertThat(status.xmlRepresentation()).isEqualTo("<Project name=\"name\" activity=\"activity1\" lastBuildStatus=\"build-status-1\" lastBuildLabel=\"build-label-1\" " +
                        "lastBuildTime=\"2010-05-23T08:00:00Z\" webUrl=\"__SITE_URL_PREFIX__/web-url\">" +
                        "<messages><message text=\"breaker1, breaker2\" kind=\"Breakers\" /></messages></Project>");
    }

    @Test
    public void shouldAlwaysHaveEmptyStringAsXMLRepresentationOfANullProjectStatus() {
        assertThat(new ProjectStatus.NullProjectStatus(keyFrom("some-name")).xmlRepresentation()).isEmpty();
        assertThat(new ProjectStatus.NullProjectStatus(keyFrom("some-other-name")).xmlRepresentation()).isEmpty();
    }

    @Test
    public void shouldNotBeViewableByAnyoneTillViewersAreUpdated() {
        ProjectStatus status = dummyStatus();

        assertThat(status.canBeViewedBy("abc")).isFalse();
        assertThat(status.canBeViewedBy("def")).isFalse();

        status.updateViewers(new AllowedUsers(Set.of("abc", "ghi"), Collections.emptySet()));

        assertThat(status.canBeViewedBy("abc")).isTrue();
        assertThat(status.canBeViewedBy("def")).isFalse();
        assertThat(status.canBeViewedBy("ghi")).isTrue();
    }

    private static @NonNull ProjectStatus dummyStatus() {
        return new ProjectStatus(keyFrom("name"), 0, "activity", "Success", "1", new Date(), "/web-url");
    }
}
