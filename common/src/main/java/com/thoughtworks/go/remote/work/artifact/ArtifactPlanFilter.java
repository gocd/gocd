/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.remote.work.artifact;

import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.domain.ArtifactPlanType;
import com.thoughtworks.go.domain.MergedTestArtifactPlan;

import java.util.ArrayList;
import java.util.List;

public class ArtifactPlanFilter {

    public List<ArtifactPlan> getBuiltInMergedArtifactPlans(List<ArtifactPlan> artifactPlans) {
        MergedTestArtifactPlan testArtifactPlan = null;
        final List<ArtifactPlan> mergedPlans = new ArrayList<>();
        for (ArtifactPlan artifactPlan : artifactPlans) {
            if (artifactPlan.getArtifactPlanType().isTest()) {
                if (testArtifactPlan == null) {
                    testArtifactPlan = new MergedTestArtifactPlan(artifactPlan);
                    mergedPlans.add(testArtifactPlan);
                } else {
                    testArtifactPlan.add(artifactPlan);
                }
            } else if (artifactPlan.getArtifactPlanType() == ArtifactPlanType.file) {
                mergedPlans.add(artifactPlan);
            }
        }
        return mergedPlans;
    }

    public List<ArtifactPlan> getPluggableArtifactPlans(List<ArtifactPlan> artifactPlans) {
        final ArrayList<ArtifactPlan> pluggableArtifactPlans = new ArrayList<>();
        for (ArtifactPlan artifactPlan : artifactPlans) {
            if (artifactPlan.getArtifactPlanType() == ArtifactPlanType.external) {
                pluggableArtifactPlans.add(artifactPlan);
            }
        }
        return pluggableArtifactPlans;
    }
}
