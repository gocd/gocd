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
package com.thoughtworks.go.domain.valuestreammap;

import java.util.Date;

import com.thoughtworks.go.domain.materials.Modification;

public class SCMRevision implements Revision, Comparable<SCMRevision> {

    private final Modification modification;

    public SCMRevision(Modification modification) {
        this.modification = modification;
    }

    @Override
    public String getRevisionString() {
        return modification.getRevision();
    }

    public String getUser() {
        return modification.getUserDisplayName();
    }

    public String getComment() {
        return modification.getComment();
    }

    public Date getModifiedTime() {
        return modification.getModifiedTime();
    }

    @Override
    public String toString() {
        return "SCMRevision{" +
                "modification=" + modification.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SCMRevision that = (SCMRevision) o;

        if (modification != null ? !modification.equals(that.modification) : that.modification != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return modification != null ? modification.hashCode() : 0;
    }

    @Override
    public int compareTo(SCMRevision other) {
        return other.modification.getModifiedTime().compareTo(modification.getModifiedTime());
    }
}
