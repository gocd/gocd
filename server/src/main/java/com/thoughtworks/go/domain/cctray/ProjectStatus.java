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

import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.util.Dates;
import com.thoughtworks.go.util.UrlUtil;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.join;

public class ProjectStatus {
    public static final String SITE_URL_PREFIX = "__SITE_URL_PREFIX__";

    private final String name;
    private final String activity;
    private final String lastBuildStatus;
    private final String lastBuildLabel;
    private final Set<String> breakers;
    private final Date lastBuildTime;
    private final String webPathAfterContext;

    private volatile Users viewers;
    private volatile String cachedXmlRepresentation;

    public ProjectStatus(String name, String activity, String lastBuildStatus, String lastBuildLabel,
                         Date lastBuildTime, String webPathAfterContext) {
        this(name, activity, lastBuildStatus, lastBuildLabel, lastBuildTime, webPathAfterContext, new HashSet<>());
    }

    public ProjectStatus(String name, String activity, String lastBuildStatus, String lastBuildLabel,
                         Date lastBuildTime, String webPathAfterContext, Set<String> breakers) {
        this.name = name;
        this.activity = activity;
        this.lastBuildStatus = lastBuildStatus;
        this.lastBuildLabel = lastBuildLabel;
        this.breakers = breakers;
        this.lastBuildTime = lastBuildTime == null ? new Date() : lastBuildTime;
        this.webPathAfterContext = webPathAfterContext;
        this.viewers = Users.NOONE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectStatus that = (ProjectStatus) o;

        return Objects.equals(activity, that.activity) &&
            Objects.equals(lastBuildLabel, that.lastBuildLabel) &&
            Objects.equals(lastBuildStatus, that.lastBuildStatus) &&
            Objects.equals(lastBuildTime, that.lastBuildTime) &&
            Objects.equals(name, that.name) &&
            Objects.equals(webPathAfterContext, that.webPathAfterContext) &&
            Objects.equals(breakers, that.breakers);
    }

    @Override
    public int hashCode() {
        int result;
        result = name != null ? name.hashCode() : 0;
        result = 31 * result + (activity != null ? activity.hashCode() : 0);
        result = 31 * result + (lastBuildStatus != null ? lastBuildStatus.hashCode() : 0);
        result = 31 * result + (lastBuildLabel != null ? lastBuildLabel.hashCode() : 0);
        result = 31 * result + (lastBuildTime != null ? lastBuildTime.hashCode() : 0);
        result = 31 * result + (webPathAfterContext != null ? webPathAfterContext.hashCode() : 0);
        result = 31 * result + (breakers != null ? breakers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("ProjectStatus[%s, %s, %s, %s, %s, %s, %s]", name, activity, lastBuildStatus, lastBuildLabel,
                Dates.formatIso8601CompactOffset(lastBuildTime) + "(" + lastBuildTime.getTime() + ")", webPathAfterContext, breakers);
    }

    public String getLastBuildLabel() {
        return lastBuildLabel;
    }

    public Date getLastBuildTime() {
        return lastBuildTime;
    }

    public String getLastBuildStatus() {
        return lastBuildStatus;
    }

    public String name() {
        return name;
    }

    public Element ccTrayXmlElement(String fullContextPath) {
        Element element = new Element("Project");
        element.setAttribute("name", name);
        element.setAttribute("activity", activity);
        element.setAttribute("lastBuildStatus", lastBuildStatus);
        element.setAttribute("lastBuildLabel", lastBuildLabel);
        element.setAttribute("lastBuildTime", Dates.formatIso8601ForCCTray(lastBuildTime));
        element.setAttribute("webUrl", UrlUtil.joinPathPartsPreEncoded(fullContextPath, webPathAfterContext));

        if (!breakers.isEmpty()) {
            addBreakers(element);
        }

        return element;
    }

    public Users viewers() {
        return viewers;
    }

    public ProjectStatus updateViewers(Users viewers) {
        this.viewers = viewers;
        return this;
    }

    public boolean canBeViewedBy(String userName) {
        return viewers.contains(userName);
    }

    public String xmlRepresentation() {
        if (cachedXmlRepresentation == null) {
            cachedXmlRepresentation = new XMLOutputter().outputString(ccTrayXmlElement(SITE_URL_PREFIX));
        }
        return cachedXmlRepresentation;
    }

    public Set<String> getBreakers() {
        return breakers;
    }

    private void addBreakers(Element element) {
        Element messages = new Element("messages");
        Element message = new Element("message");
        String breakerNames = join(", ", breakers);
        message.setAttribute("text", breakerNames);
        message.setAttribute("kind", "Breakers");
        messages.addContent(message);
        element.addContent(messages);
    }

    public static class NullProjectStatus extends ProjectStatus {
        private static final Date DEFAULT_LAST_BUILD_DATE = new Date();

        public NullProjectStatus(String name) {
            super(name, "", "Success", "1", DEFAULT_LAST_BUILD_DATE, "");
        }

        @Override
        public String xmlRepresentation() {
            return "";
        }
    }
}
