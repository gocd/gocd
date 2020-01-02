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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRFetchPluggableArtifactTask extends CRAbstractFetchTask {
    public static final String ARTIFACT_ORIGIN = "external";

    @SerializedName("artifact_id")
    @Expose
    private String artifactId;
    @SerializedName("configuration")
    @Expose
    private Collection<CRConfigurationProperty> configuration;

    public CRFetchPluggableArtifactTask() {
        super(null, null, ArtifactOrigin.external, null, null);
    }

    public CRFetchPluggableArtifactTask(CRRunIf runIf, CRTask onCancel, String pipelineName, String stage, String job, String artifactId, List<CRConfigurationProperty> crConfigurationProperties) {
        super(stage, job, ArtifactOrigin.external, runIf, onCancel);
        this.artifactId = artifactId;
        this.pipeline = pipelineName;
        this.configuration = crConfigurationProperties;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "artifact_id", artifactId);
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

}
