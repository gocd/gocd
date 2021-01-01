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
package com.thoughtworks.go.domain;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public enum ArtifactPlanType {
    unit,
    file,
    external;

    public boolean isTest() {
        return this.equals(unit);
    }

    public static ArtifactPlanType fromArtifactType(ArtifactType artifactType) {
        switch (artifactType) {
            case test:
                return ArtifactPlanType.unit;
            case build:
                return ArtifactPlanType.file;
            case external:
                return ArtifactPlanType.external;
            default:
                throw bomb("Illegal name in for the artifact type.[" + artifactType + "].");
        }
    }
}
