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
package com.thoughtworks.go.domain.materials.packagematerial;

import java.util.Date;
import java.util.Map;

import com.thoughtworks.go.domain.materials.Revision;

public class PackageMaterialRevision implements Revision {

    private String revision;
    private Date timestamp;
    private Map<String, String> data;

    public PackageMaterialRevision(String revision, Date timestamp) {
        this(revision, timestamp, null);
    }

    public PackageMaterialRevision(String revision, Date timestamp, Map<String, String> data) {
        this.revision = revision;
        this.timestamp = timestamp;
        this.data = data;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public String getRevisionUrl() {
        return getRevision();
    }

    @Override
    public boolean isRealRevision() {
        return true;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PackageMaterialRevision revision1 = (PackageMaterialRevision) o;

        if (revision != null ? !revision.equals(revision1.revision) : revision1.revision != null) {
            return false;
        }
        if (timestamp != null ? !timestamp.equals(revision1.timestamp) : revision1.timestamp != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = revision != null ? revision.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }
}
