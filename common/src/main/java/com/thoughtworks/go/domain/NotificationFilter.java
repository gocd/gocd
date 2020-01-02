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

import com.thoughtworks.go.util.GoConstants;

import java.util.HashMap;
import java.util.Map;

public class NotificationFilter extends PersistentObject {
    private String pipelineName;
    private String stageName;
    private StageEvent event;
    private boolean myCheckin;

    private NotificationFilter() {
    }

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

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public StageEvent getEvent() {
        return event;
    }

    public void setEvent(StageEvent event) {
        this.event = event;
    }

    public boolean isMyCheckin() {
        return myCheckin;
    }

    public void setMyCheckin(boolean myCheckin) {
        this.myCheckin = myCheckin;
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
        return String.format("pipeline: %s, stage: %s, describeChange: %s, check-in: %s", pipelineName, stageName, event,
                myCheckin ? "Mine" : "All");
    }

    @Override
    public String toString() {
        return "NotificationFilter[" + description() + "]";
    }

    /**
     * Used for JSON serialization in Rails
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NotificationFilter filter = (NotificationFilter) o;

        if (myCheckin != filter.myCheckin) {
            return false;
        }
        if (event != filter.event) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(filter.pipelineName) : filter.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(filter.stageName) : filter.stageName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + (event != null ? event.hashCode() : 0);
        result = 31 * result + (myCheckin ? 1 : 0);
        return result;
    }

    public boolean include(NotificationFilter filter) {
        return pipelineName.equals(filter.pipelineName)
                && stageName.equals(filter.stageName)
                && event.include(filter.event);
    }
}
