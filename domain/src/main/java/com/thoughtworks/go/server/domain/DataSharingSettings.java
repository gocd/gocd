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
package com.thoughtworks.go.server.domain;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class DataSharingSettings extends com.thoughtworks.go.domain.PersistentObject {
    private boolean allowSharing = true;
    private String updatedBy;
    private Timestamp updatedOn;

    public DataSharingSettings() {
    }

    public DataSharingSettings(boolean allowSharing, String updatedBy, Date updatedOn) {
        this.allowSharing = allowSharing;
        this.updatedBy = updatedBy;
        this.updatedOn = new Timestamp(updatedOn.getTime());
    }

    public void setAllowSharing(boolean allowSharing) {
        this.allowSharing = allowSharing;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean allowSharing() {
        return allowSharing;
    }

    public String updatedBy() {
        return updatedBy;
    }

    public Timestamp updatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Timestamp updatedOn) {
        this.updatedOn = updatedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSharingSettings that = (DataSharingSettings) o;
        return allowSharing == that.allowSharing &&
                Objects.equals(updatedBy, that.updatedBy) && Objects.equals(updatedOn, that.updatedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), allowSharing, updatedBy, updatedOn);
    }

    public void copyFrom(DataSharingSettings from) {
        this.allowSharing = from.allowSharing;
        this.updatedBy = from.updatedBy;
        this.updatedOn = from.updatedOn;
    }
}
