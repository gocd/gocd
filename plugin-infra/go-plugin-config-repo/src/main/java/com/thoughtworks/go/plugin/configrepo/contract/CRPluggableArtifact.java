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

package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRPluggableArtifact extends CRArtifact {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("store_id")
    @Expose
    private String storeId;
    @SerializedName("configuration")
    @Expose
    private Collection<CRConfigurationProperty> configuration;

    public CRPluggableArtifact(String id, String storeId, List<CRConfigurationProperty> configurationProperties) {
        super(CRArtifactType.external);
        this.id = id;
        this.storeId = storeId;
        this.configuration = configurationProperties;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        super.getErrors(errors, parentLocation);
        errors.checkMissing(location, "id", id);
        errors.checkMissing(location, "store_id", storeId);
        if (this.configuration != null) {
            for (CRConfigurationProperty property : configuration) {
                property.getErrors(errors, location);
            }
        }
    }
}
