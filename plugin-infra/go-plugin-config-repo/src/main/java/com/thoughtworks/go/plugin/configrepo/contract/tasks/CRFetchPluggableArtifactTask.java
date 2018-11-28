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

package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CRFetchPluggableArtifactTask extends CRAbstractFetchTask {
    public static final String ARTIFACT_ORIGIN = "external";

    private String artifact_id;
    private Collection<CRConfigurationProperty> configuration;

    public CRFetchPluggableArtifactTask() {
        super(TYPE_NAME, ArtifactOrigin.external);
    }

    public CRFetchPluggableArtifactTask(String stage,
                                        String job,
                                        String artifactId,
                                        CRConfigurationProperty... crConfigurationProperties) {
        super(stage, job, TYPE_NAME, ArtifactOrigin.external);
        this.artifact_id = artifactId;
        configuration = Arrays.asList(crConfigurationProperties);
    }

    public CRFetchPluggableArtifactTask(String stage,
                                        String job,
                                        String artifactId,
                                        List<CRConfigurationProperty> crConfigurationProperties) {
        super(stage, job, TYPE_NAME, ArtifactOrigin.external);
        this.artifact_id = artifactId;
        configuration = crConfigurationProperties;
    }

    public CRFetchPluggableArtifactTask(CRRunIf runIf, CRTask onCancel,
                                        String pipelineName, String stage, String job,
                                        String artifactId, CRConfigurationProperty... crConfigurationProperties) {
        super(runIf, onCancel);
        this.pipeline = pipelineName;
        this.stage = stage;
        this.job = job;
        this.artifact_id = artifactId;
        configuration = Arrays.asList(crConfigurationProperties);
    }

    public String getArtifactId() {
        return artifact_id;
    }

    public Collection<CRConfigurationProperty> getConfiguration() {
        return configuration;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "artifact_id", artifact_id);
        errors.checkMissing(location, "stage", stage);
        errors.checkMissing(location, "job", job);

        if (this.configuration != null) {
            for (CRConfigurationProperty p : configuration) {
                p.getErrors(errors, location);
            }
        }
        validateKeyUniqueness(errors, location);
    }

    void validateKeyUniqueness(ErrorCollection errors, String location) {
        if (this.configuration != null) {
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

        if (artifact_id != null ? !artifact_id.equals(that.artifact_id) : that.artifact_id != null) {
            return false;
        }
        return configuration != null ? configuration.equals(that.configuration) : that.configuration == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (artifact_id != null ? artifact_id.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }
}
