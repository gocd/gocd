/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

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
    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) { return false; }
        if (getClass() != that.getClass()) { return false; }

        return equals((MatchedRevision) that);
    }

    private boolean equals(MatchedRevision that) {
        if (!checkinTime.equals(that.checkinTime)) { return false; }
        if (!comment.equals(that.comment)) { return false; }
        if (!shortRevision.equals(that.shortRevision)) { return false; }
        if (!longRevision.equals(that.longRevision)) { return false; }
        if (!searchString.equals(that.searchString)) { return false; }
        if (!user.equals(that.user)) { return false; }
        return true;
    }

    @Override
    public int hashCode() {
        int result = searchString.hashCode();
        result = 31 * result + shortRevision.hashCode();
        result = 31 * result + longRevision.hashCode();
        result = 31 * result + user.hashCode();
        result = 31 * result + checkinTime.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
