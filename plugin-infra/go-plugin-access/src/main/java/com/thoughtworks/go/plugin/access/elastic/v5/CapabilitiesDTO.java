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
package com.thoughtworks.go.plugin.access.elastic.v5;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

class CapabilitiesDTO {
    @Expose
    @SerializedName("supports_plugin_status_report")
    private boolean supportsStatusReport;

    @Expose
    @SerializedName("supports_cluster_status_report")
    private boolean supportsClusterStatusReport;

    @Expose
    @SerializedName("supports_agent_status_report")
    private boolean supportsAgentStatusReport;

    public boolean supportsPluginStatusReport() {
        return supportsStatusReport;
    }

    public boolean supportsClusterStatusReport() {
        return supportsClusterStatusReport;
    }

    public boolean supportsAgentStatusReport() {
        return supportsAgentStatusReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CapabilitiesDTO that = (CapabilitiesDTO) o;
        return supportsStatusReport == that.supportsStatusReport &&
                supportsClusterStatusReport == that.supportsClusterStatusReport &&
                supportsAgentStatusReport == that.supportsAgentStatusReport;
    }

    @Override
    public int hashCode() {
        return Objects.hash(supportsStatusReport, supportsClusterStatusReport, supportsAgentStatusReport);
    }
}
