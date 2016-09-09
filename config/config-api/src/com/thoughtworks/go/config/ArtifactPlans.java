/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.Artifact;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigTag("artifacts")
@ConfigCollection(Artifact.class)
public class ArtifactPlans extends BaseCollection<ArtifactPlan> implements Validatable, ParamsAttributeAware {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ArtifactPlans() {
    }

    public ArtifactPlans(List<ArtifactPlan> plans) {
        super(plans);
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();

        for (ArtifactPlan artifactPlan : this) {
            isValid = artifactPlan.validateTree(validationContext) && isValid;
        }
        return isValid;
    }
    public void validate(ValidationContext validationContext) {
        validateUniqueness();
    }

    private void validateUniqueness() {
        List<ArtifactPlan> plans = new ArrayList<>();
        for (ArtifactPlan artifactPlan : this) {
            artifactPlan.validateUniqueness(plans);
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void setConfigAttributes(Object attributes) {
        clear();
        if (attributes == null) {
            return;
        }
        List<Map> attrList = (List<Map>) attributes;
        for (Map attrMap : attrList) {
            String source = (String) attrMap.get(ArtifactPlan.SRC);
            String destination = (String) attrMap.get(ArtifactPlan.DEST);
            if (source.trim().isEmpty() && destination.trim().isEmpty()) {
                continue;
            }
            String type = (String) attrMap.get("artifactTypeValue");

            if (TestArtifactPlan.TEST_PLAN_DISPLAY_NAME.equals(type)) {
                this.add(new TestArtifactPlan(source, destination));
            } else {
                this.add(new ArtifactPlan(source, destination));
            }
        }
    }

}
