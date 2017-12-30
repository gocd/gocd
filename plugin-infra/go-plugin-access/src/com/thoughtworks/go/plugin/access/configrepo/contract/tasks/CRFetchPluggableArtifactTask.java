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

package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class CRFetchPluggableArtifactTask extends CRAbstractFetchTask {

    public static final String TYPE_NAME = "fetch_pluggable_artifact";

    private String store_id;
    private Collection<CRConfigurationProperty> configuration;

    public CRFetchPluggableArtifactTask() {
        super(TYPE_NAME);
    }

    public CRFetchPluggableArtifactTask(String stage, String job, String storeId, CRConfigurationProperty... crConfigurationProperties) {
        super(stage, job, TYPE_NAME);
        this.store_id = storeId;
        configuration = Arrays.asList(crConfigurationProperties);
    }

    public CRFetchPluggableArtifactTask(CRRunIf runIf, CRTask onCancel,
                                        String pipelineName, String stage, String job,
                                        String storeId, CRConfigurationProperty... crConfigurationProperties) {
        super(runIf, onCancel);
        this.pipeline = pipelineName;
        this.stage = stage;
        this.job = job;
        this.store_id = storeId;
        configuration = Arrays.asList(crConfigurationProperties);
    }

    public String getStoreId() {
        return store_id;
    }

    public Collection<CRConfigurationProperty> getConfiguration() {
        return configuration;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "store_id", store_id);
        errors.checkMissing(location, "stage", stage);
        errors.checkMissing(location, "job", job);

        if(this.configuration != null)
        {
            for(CRConfigurationProperty p : configuration)
            {
                p.getErrors(errors,location);
            }
        }
        validateKeyUniqueness(errors,location);
    }

    void validateKeyUniqueness(ErrorCollection errors,String location) {
        if(this.configuration != null) {
            ArrayList<String> keys = new ArrayList<>();
            for (CRConfigurationProperty property : this.configuration) {
                String key = property.getKey();
                if (keys.contains(key))
                    errors.addError(location, String.format("Duplicate Configuration property %s", property));
                else
                    keys.add(key);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CRFetchPluggableArtifactTask that = (CRFetchPluggableArtifactTask) o;

        if (store_id != null ? !store_id.equals(that.store_id) : that.store_id != null) {
            return false;
        }
        return configuration != null ? configuration.equals(that.configuration) : that.configuration == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (store_id != null ? store_id.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }
}
