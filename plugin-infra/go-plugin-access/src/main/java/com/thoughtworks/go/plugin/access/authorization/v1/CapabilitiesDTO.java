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
package com.thoughtworks.go.plugin.access.authorization.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class CapabilitiesDTO {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("supported_auth_type")
    private final SupportedAuthTypeDTO supportedAuthType;

    @Expose
    @SerializedName("can_search")
    private final boolean canSearch;

    @Expose
    @SerializedName("can_authorize")
    private final boolean canAuthorize;

    public CapabilitiesDTO(SupportedAuthTypeDTO supportedAuthType, boolean canSearch, boolean canAuthorize) {
        this.supportedAuthType = supportedAuthType;
        this.canSearch = canSearch;
        this.canAuthorize = canAuthorize;
    }

    public SupportedAuthTypeDTO getSupportedAuthType() {
        return supportedAuthType;
    }

    public boolean canSearch() {
        return canSearch;
    }

    public String toJSON() {
        return GSON.toJson(this);
    }

    public static CapabilitiesDTO fromJSON(String json) {
        return GSON.fromJson(json, CapabilitiesDTO.class);
    }

    public boolean canAuthorize() {
        return canAuthorize;
    }

    public com.thoughtworks.go.plugin.domain.authorization.Capabilities toDomainModel() {
        com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType supportedAuthType = com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType.valueOf(this.supportedAuthType.name());
        return new com.thoughtworks.go.plugin.domain.authorization.Capabilities(supportedAuthType, canSearch, canAuthorize, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CapabilitiesDTO that = (CapabilitiesDTO) o;

        if (canSearch != that.canSearch) return false;
        if (canAuthorize != that.canAuthorize) return false;
        return supportedAuthType == that.supportedAuthType;
    }

    @Override
    public int hashCode() {
        int result = supportedAuthType != null ? supportedAuthType.hashCode() : 0;
        result = 31 * result + (canSearch ? 1 : 0);
        result = 31 * result + (canAuthorize ? 1 : 0);
        return result;
    }
}
