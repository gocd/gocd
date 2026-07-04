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

import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.util.Dates;
import com.thoughtworks.go.util.UrlUtil;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.join;

public class ProjectStatus {
    public static final String SITE_URL_PREFIX = "__SITE_URL_PREFIX__";

    private final @NotNull Key key;
    private final int stageOrder;
    private final String activity;
    private final String lastBuildStatus;
    private final String lastBuildLabel;
    private final Set<String> breakers;
    private final @NotNull Date lastBuildTime;
    private final String webPathAfterContext;

    private volatile Users viewers;
    private volatile String cachedXmlRepresentation;

    public ProjectStatus(@NotNull Key key, int stageOrder, String activity, String lastBuildStatus, String lastBuildLabel,
                         Date lastBuildTime, String webPathAfterContext) {
        this(key, stageOrder, activity, lastBuildStatus, lastBuildLabel, lastBuildTime, webPathAfterContext, new HashSet<>());
    }

    public ProjectStatus(@NotNull Key key, int stageOrder, String activity, String lastBuildStatus, String lastBuildLabel,
                         Date lastBuildTime, String webPathAfterContext, Set<String> breakers) {
        this.key = key;
        this.stageOrder = stageOrder;
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

        return key.equals(that.key) &&
            Objects.equals(activity, that.activity) &&
            Objects.equals(lastBuildStatus, that.lastBuildStatus) &&
            Objects.equals(lastBuildLabel, that.lastBuildLabel) &&
            lastBuildTime.equals(that.lastBuildTime) &&
            Objects.equals(webPathAfterContext, that.webPathAfterContext) &&
            Objects.equals(breakers, that.breakers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, activity, lastBuildStatus, lastBuildLabel, lastBuildTime, breakers, webPathAfterContext);
    }

    @Override
    public String toString() {
        return String.format("ProjectStatus[%s, %s, %s, %s, %s, %s, %s]", key.projectName(), activity, lastBuildStatus, lastBuildLabel,
                Dates.formatIso8601SystemCompactOffsetNoMillis(lastBuildTime) + "(" + lastBuildTime.getTime() + ")", webPathAfterContext, breakers);
    }

    public String getLastBuildLabel() {
        return lastBuildLabel;
    }

    public @NotNull Date getLastBuildTime() {
        return lastBuildTime;
    }

    public String getLastBuildStatus() {
        return lastBuildStatus;
    }

    public Key key() {
        return key;
    }

    public String name() {
        return key.projectName();
    }

    public int stageOrder() {
        return stageOrder;
    }

    public Element ccTrayXmlElement(String fullContextPath) {
        Element element = new Element("Project");
        element.setAttribute("name", key.projectName());
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

        public NullProjectStatus(Key identifier) {
            this(identifier, Integer.MAX_VALUE);
        }

        public NullProjectStatus(Key identifier, int stageOrderId) {
            super(identifier, stageOrderId, "", "Success", "1", DEFAULT_LAST_BUILD_DATE, "");
        }

        @Override
        public String xmlRepresentation() {
            return "";
        }
    }

    public record Key(String pipeline, @Nullable String stage, @Nullable String job) {
        Key(String pipeline) {
            this(pipeline, null, null);
        }

        Key(String pipeline, String stage) {
            this(pipeline, stage, null);
        }

        Key(StageIdentifier identifier) {
            this(identifier.getPipelineName(), identifier.getStageName());
        }

        Key(JobIdentifier identifier) {
            this(identifier.getPipelineName(), identifier.getStageName(), identifier.getBuildName());
        }

        Key(PipelineConfig pipelineConfig, StageConfig stageConfig) {
            this(pipelineConfig.name().toString(), stageConfig.name().toString());
        }

        Key(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig) {
            this(pipelineConfig.name().toString(), stageConfig.name().toString(), jobConfig.name().toString());
        }

        String projectName() {
            return Stream.of(pipeline, stage, job).filter(Objects::nonNull).collect(Collectors.joining(" :: "));
        }

        public static Key keyFrom(String pipeline) {
            return new Key(pipeline);
        }

        public static Key keyFrom(String pipeline, String stage) {
            return new Key(pipeline, stage);
        }

        public static Key keyFrom(String pipeline, String stage, String job) {
            return new Key(pipeline, stage, job);
        }
    }
}
