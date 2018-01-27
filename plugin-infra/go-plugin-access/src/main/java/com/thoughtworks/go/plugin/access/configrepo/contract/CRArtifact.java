/*
 * Copyright 2017 ThoughtWorks, Inc.
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

public class CRArtifact extends CRBase {
    private String source;
    private String destination;
    private CRArtifactType type;
    private String id;
    private String store_id;
    private Collection<CRConfigurationProperty> configuration;

    public CRArtifact(){}
    public CRArtifact(String src, String dest,CRArtifactType type) {
        this.source = src;
        this.destination = dest;
        this.type = type;
    }

    public CRArtifact(String id, String store_id, CRConfigurationProperty... configurationProperties) {
        this.type = CRArtifactType.plugin;
        this.id = id;
        this.store_id = store_id;
        this.configuration = Arrays.asList(configurationProperties);
    }
    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
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

        CRArtifact that = (CRArtifact) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) return false;
        if (type != that.type) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (store_id != null ? !store_id.equals(that.store_id) : that.store_id != null) return false;
        return configuration != null ? configuration.equals(that.configuration) : that.configuration == null;
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (store_id != null ? store_id.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    public CRArtifactType getType() {
        return type;
    }

    public void setType(CRArtifactType type) {
        this.type = type;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"type",type);
        if (type == CRArtifactType.plugin) {
            errors.checkMissing(location, "id", id);
            errors.checkMissing(location, "store_id", store_id);
            if (this.configuration != null) {
                for (CRConfigurationProperty property : configuration) {
                    property.getErrors(errors, location);
                }
            }
        } else {
            errors.checkMissing(location, "source", source);
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Artifacts",myLocation);
    }
}
