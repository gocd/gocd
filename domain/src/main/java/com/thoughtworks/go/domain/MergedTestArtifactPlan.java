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
package com.thoughtworks.go.domain;

import java.util.Iterator;

import static com.thoughtworks.go.config.TestArtifactConfig.TEST_OUTPUT_FOLDER;

public class MergedTestArtifactPlan extends ArtifactPlan {
    public MergedTestArtifactPlan(ArtifactPlan artifactPlan) {
        super(ArtifactPlanType.unit, null, TEST_OUTPUT_FOLDER);
        add(artifactPlan);
    }

    public void add(ArtifactPlan plan) {
        testArtifactPlansForMerging.add(plan);
    }

    @Override
    public void printArtifactInfo(StringBuilder builder) {
        Iterator<ArtifactPlan> planIterator = testArtifactPlansForMerging.iterator();
        builder.append('[');
        while (planIterator.hasNext()) {
            ArtifactPlan plan = planIterator.next();
            builder.append(plan.getSrc());
            if (planIterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(']');
    }
}
