/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic.v2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CapabilitiesDTO {
    @Expose
    @SerializedName("supports_status_report")
    private boolean supportsStatusReport;

    public boolean supportsStatusReport() {
        return supportsStatusReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilitiesDTO)) return false;

        CapabilitiesDTO that = (CapabilitiesDTO) o;

        return supportsStatusReport == that.supportsStatusReport;
    }

    @Override
    public int hashCode() {
        return (supportsStatusReport ? 1 : 0);
    }
}
