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

package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CRPluggableArtifact extends CRArtifact {
    private String id;
    private String store_id;
    private Collection<CRConfigurationProperty> configuration;

    public CRPluggableArtifact(String id, String store_id, CRConfigurationProperty... configurationProperties) {
        super(CRArtifactType.external);
        this.id = id;
        this.store_id = store_id;
        this.configuration = Arrays.asList(configurationProperties);
    }

    public CRPluggableArtifact(String id, String store_id, List<CRConfigurationProperty> configurationProperties) {
        super(CRArtifactType.external);
        this.id = id;
        this.store_id = store_id;
        this.configuration = configurationProperties;
    }

    public String getId() {
        return id;
    }

    public String getStoreId() {
        return store_id;
    }

    public Collection<CRConfigurationProperty> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CRPluggableArtifact that = (CRPluggableArtifact) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
            return false;
        }
        if (store_id != null ? !store_id.equals(that.store_id) : that.store_id != null) {
            return false;
        }
        return getConfiguration() != null ? getConfiguration().equals(that.getConfiguration()) : that.getConfiguration() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (store_id != null ? store_id.hashCode() : 0);
        result = 31 * result + (getConfiguration() != null ? getConfiguration().hashCode() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        super.getErrors(errors, parentLocation);
        errors.checkMissing(location, "id", id);
        errors.checkMissing(location, "store_id", store_id);
        if (this.configuration != null) {
            for (CRConfigurationProperty property : configuration) {
                property.getErrors(errors, location);
            }
        }
    }
}
