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

import com.thoughtworks.go.domain.materials.Modification;

public class ModificationSummary {
    Modification modification;
    public static final ModificationSummary NEVER = new ModificationSummary(Modification.NEVER);

    public ModificationSummary(Modification modification) {
        this.modification = modification;
    }

    public Date getModifiedTime() {
        return modification.getModifiedTime();
    }

    public String getComment() {
        return modification.getComment();
    }

    public String getRevision() {
        final String revisionNumber = modification.getRevision();
        if (revisionNumber.length() < 12) {
            return revisionNumber;
        }
        return revisionNumber.substring(0, 12) + "...";
    }

    public String getUserName() {
        return modification.getUserName();
    }

    public String getUserDisplayName() {
        return modification.getUserDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModificationSummary that = (ModificationSummary) o;

        if (getComment() != null ? !getComment().equals(that.getComment()) : that.getComment() != null) {
            return false;
        }
        if (getRevision() != null ? !getRevision().equals(that.getRevision()) : that.getRevision() != null) {
            return false;
        }
        if (getUserName() != null ? !getUserName().equals(that.getUserName()) : that.getUserName() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (modification != null ? modification.hashCode() : 0);
        result = 31 * result + (getComment() != null ? getComment().hashCode() : 0);
        result = 31 * result + (getRevision() != null ? getRevision().hashCode() : 0);
        result = 31 * result + (getUserName() != null ? getUserName().hashCode() : 0);
        return result;
    }

    public ModificationSummary withLatestRevision(ModificationSummary latestSummary) {
        if (this.getModifiedTime().after(latestSummary.getModifiedTime())) {
            return this;
        }
        return latestSummary;
    }
}
