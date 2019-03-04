/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic.v5;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class CapabilitiesDTO {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    @Expose
    @SerializedName("supports_status_report")
    private boolean supportsStatusReport;

    @Expose
    @SerializedName("supports_agent_status_report")
    private boolean supportsAgentStatusReport;

    public boolean supportsStatusReport() {
        return supportsStatusReport;
    }

    public boolean supportsAgentStatusReport() {
        return supportsAgentStatusReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CapabilitiesDTO)) return false;

        CapabilitiesDTO that = (CapabilitiesDTO) o;

        if (supportsStatusReport != that.supportsStatusReport) return false;
        return supportsAgentStatusReport == that.supportsAgentStatusReport;
    }

    @Override
    public int hashCode() {
        int result = (supportsStatusReport ? 1 : 0);
        result = 31 * result + (supportsAgentStatusReport ? 1 : 0);
        return result;
    }
}
