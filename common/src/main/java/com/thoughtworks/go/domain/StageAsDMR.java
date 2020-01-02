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

import java.util.Date;

public class StageAsDMR {

    private StageIdentifier identifier;
    private Date completedDate;
    private Long pipelineId;

    public StageAsDMR() {
    }

    public StageAsDMR(StageIdentifier identifier, Date completedDate) {
        this.identifier = identifier;
        this.completedDate = completedDate;
    }

    public void setIdentifier(StageIdentifier identifier) {
        this.identifier = identifier;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public StageIdentifier getIdentifier() {
        return identifier;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageAsDMR asDMR = (StageAsDMR) o;

        if (completedDate != null ? !completedDate.equals(asDMR.completedDate) : asDMR.completedDate != null) {
            return false;
        }
        if (identifier != null ? !identifier.equals(asDMR.identifier) : asDMR.identifier != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier != null ? identifier.hashCode() : 0;
        result = 31 * result + (completedDate != null ? completedDate.hashCode() : 0);
        return result;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(Long piplineId) {
        this.pipelineId = piplineId;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("StageAsDMR");
        sb.append("{identifier=").append(identifier);
        sb.append(", completedDate=").append(completedDate);
        sb.append(", pipelineId=").append(pipelineId);
        sb.append('}');
        return sb.toString();
    }
}
