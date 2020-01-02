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

import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ProjectStatus {
    public static final Date DEFAULT_LAST_BUILD_TIME = new Date();
    public static final String DEFAULT_LAST_BUILD_STATUS = "Success";
    public static final String SITE_URL_PREFIX = "__SITE_URL_PREFIX__";

    private String name;
    private String activity;
    private String lastBuildStatus = "Success";
    private String lastBuildLabel;
    private final Set<String> breakers;
    private Date lastBuildTime;
    private String webUrl;
    public static final String DEFAULT_LAST_BUILD_LABEL = "1";
    private volatile Users viewers;
    private String cachedXmlRepresentation;

    public ProjectStatus(String name, String activity, String lastBuildStatus, String lastBuildLabel,
                         Date lastBuildTime, String webUrl) {
        this(name, activity, lastBuildStatus, lastBuildLabel, lastBuildTime, webUrl, new HashSet<>());
    }

    public ProjectStatus(String name, String activity, String webUrl) {
        this(name, activity, DEFAULT_LAST_BUILD_STATUS, DEFAULT_LAST_BUILD_LABEL, DEFAULT_LAST_BUILD_TIME, webUrl);
    }

    public ProjectStatus(String name, String activity, String lastBuildStatus, String lastBuildLabel,
                         Date lastBuildTime, String webUrl, Set<String> breakers) {
        this.name = name;
        this.activity = activity;
        this.lastBuildStatus = lastBuildStatus;
        this.lastBuildLabel = lastBuildLabel;
        this.breakers = breakers;
        this.lastBuildTime = lastBuildTime == null ? DEFAULT_LAST_BUILD_TIME : lastBuildTime;
        this.webUrl = webUrl;
        this.viewers = NoOne.INSTANCE;
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

        if (activity != null ? !activity.equals(that.activity) : that.activity != null) {
            return false;
        }
        if (lastBuildLabel != null ? !lastBuildLabel.equals(that.lastBuildLabel) : that.lastBuildLabel != null) {
            return false;
        }
        if (lastBuildStatus != null ? !lastBuildStatus.equals(that.lastBuildStatus) : that.lastBuildStatus != null) {
            return false;
        }
        if (lastBuildTime != null ? !lastBuildTime.equals(that.lastBuildTime) : that.lastBuildTime != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (webUrl != null ? !webUrl.equals(that.webUrl) : that.webUrl != null) {
            return false;
        }
        if (breakers != null ? !breakers.equals(that.breakers) : that.breakers != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (activity != null ? activity.hashCode() : 0);
        result = 31 * result + (lastBuildStatus != null ? lastBuildStatus.hashCode() : 0);
        result = 31 * result + (lastBuildLabel != null ? lastBuildLabel.hashCode() : 0);
        result = 31 * result + (lastBuildTime != null ? lastBuildTime.hashCode() : 0);
        result = 31 * result + (webUrl != null ? webUrl.hashCode() : 0);
        result = 31 * result + (breakers != null ? breakers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("ProjectStatus[%s, %s, %s, %s, %s, %s, %s]", name, activity, lastBuildStatus, lastBuildLabel,
                DateUtils.formatISO8601(lastBuildTime) + "(" + lastBuildTime.getTime() + ")", webUrl, breakers);
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
        element.setAttribute("lastBuildTime", DateUtils.formatIso8601ForCCTray(lastBuildTime));
        element.setAttribute("webUrl", fullContextPath + "/" + webUrl);

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
        String breakerNames = StringUtils.join(breakers, ", ");
        message.setAttribute("text", breakerNames);
        message.setAttribute("kind", "Breakers");
        messages.addContent(message);
        element.addContent(messages);
    }

    public static class NullProjectStatus extends ProjectStatus {
        public NullProjectStatus(String name) {
            super(name, "", DEFAULT_LAST_BUILD_STATUS, DEFAULT_LAST_BUILD_LABEL, DEFAULT_LAST_BUILD_TIME, "");
        }

        @Override
        public String xmlRepresentation() {
            return "";
        }
    }
}
