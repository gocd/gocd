/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

/**
 * @understands a material revision which matches a search criteria
 */
public class MatchedRevision {
    private final String searchString;
    private final String shortRevision;
    private final String longRevision;
    private final String user;
    private final Date checkinTime;
    private final String comment;

    public MatchedRevision(String searchString, String shortRevision, String longRevision, String user, Date checkinTime, String comment) {
        this.searchString = searchString;
        this.shortRevision = shortRevision;
        this.longRevision = longRevision;
        this.user = user;
        this.checkinTime = checkinTime;
        this.comment = comment;
    }

    public MatchedRevision(String searchString, String revision, Date modifiedTime, String pipelineLabel) {
        this(searchString, revision, revision, null, modifiedTime, pipelineLabel);
    }

    public String getShortRevision() {
        return shortRevision;
    }

    public String getLongRevision() {
        return longRevision;
    }

    public String getUser() {
        return user;
    }

    public Date getCheckinTime() {
        return checkinTime;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchedRevision that = (MatchedRevision) o;

        if (searchString != null ? !searchString.equals(that.searchString) : that.searchString != null) return false;
        if (shortRevision != null ? !shortRevision.equals(that.shortRevision) : that.shortRevision != null)
            return false;
        if (longRevision != null ? !longRevision.equals(that.longRevision) : that.longRevision != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (checkinTime != null ? !checkinTime.equals(that.checkinTime) : that.checkinTime != null) return false;
        return comment != null ? comment.equals(that.comment) : that.comment == null;
    }

    @Override
    public int hashCode() {
        int result = searchString != null ? searchString.hashCode() : 0;
        result = 31 * result + (shortRevision != null ? shortRevision.hashCode() : 0);
        result = 31 * result + (longRevision != null ? longRevision.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (checkinTime != null ? checkinTime.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
