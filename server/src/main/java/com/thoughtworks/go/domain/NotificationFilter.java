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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.util.GoConstants;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "notificationfilters")
public class NotificationFilter extends HibernatePersistedObject implements Validatable {
    @Column(name = "pipeline")
    private String pipelineName;
    @Column(name = "stage")
    private String stageName;
    @Enumerated(EnumType.STRING)
    private StageEvent event;
    private boolean myCheckin;

    @EqualsAndHashCode.Exclude
    private transient ConfigErrors errors = new ConfigErrors();

    public NotificationFilter(String pipelineName, String stageName, StageEvent event, boolean myCheckin) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.event = event;
        this.myCheckin = myCheckin;
    }

    public NotificationFilter(NotificationFilter filter) {
        this(filter.pipelineName, filter.stageName, filter.event, filter.myCheckin);
        this.id = filter.id;
    }

    public boolean isAppliedOnAllCheckins() {
        return !myCheckin;
    }

    public boolean matchStage(StageConfigIdentifier stageIdentifier, StageEvent event) {
        return this.event.include(event) && appliesTo(stageIdentifier.getPipelineName(), stageIdentifier.getStageName());
    }

    public boolean appliesTo(String pipelineName, String stageName) {
        boolean pipelineMatches = this.pipelineName.equals(pipelineName) ||
            this.pipelineName.equals(GoConstants.ANY_PIPELINE);
        boolean stageMatches = this.stageName.equals(stageName) ||
            this.stageName.equals(GoConstants.ANY_STAGE);

        return pipelineMatches && stageMatches;
    }

    public String description() {
        return format("pipeline: %s, stage: %s, describeChange: %s, check-in: %s", pipelineName, stageName, event,
            myCheckin ? "Mine" : "All");
    }

    public boolean include(NotificationFilter filter) {
        return pipelineName.equals(filter.pipelineName)
            && stageName.equals(filter.stageName)
            && event.include(filter.event);
    }

    /**
     * Used for JSON serialization in Rails. See NotificationFiltersRepresenter
     *
     * @return a Map representation of this {@link NotificationFilter} instance that is serializable by JRuby
     */
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("pipelineName", pipelineName);
        map.put("stageName", stageName);
        map.put("myCheckin", myCheckin);
        map.put("event", event.toString());

        return map;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (equalsIgnoreCase(this.pipelineName, "[Any Pipeline]")) {
            return;
        }

        PipelineConfig pipelineConfig = validationContext.getCruiseConfig()
            .getPipelineConfigByName(new CaseInsensitiveString(this.pipelineName));
        if (pipelineConfig == null) {
            addError("pipelineName", format("Pipeline with name '%s' was not found!", this.pipelineName));
            return;
        }

        if (equalsIgnoreCase(this.stageName, "[Any Stage]")) {
            return;
        }

        if (pipelineConfig.getStage(this.stageName) == null) {
            addError("stageName", format("Stage '%s' not found in pipeline '%s'!", this.stageName, this.pipelineName));
        }
    }

    @Override
    public ConfigErrors errors() {
        return this.errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        this.errors.add(fieldName, message);
    }
}
