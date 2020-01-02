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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ArtifactType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@AttributeAwareConfigTag(value = "artifact", attribute = "type", attributeValue = "build")
public class BuildArtifactConfig extends BuiltinArtifactConfig {
    public static final String ARTIFACT_PLAN_DISPLAY_NAME = "Build Artifact";

    public BuildArtifactConfig() {
    }

    public BuildArtifactConfig(String source, String destination) {
        setSource(source);
        setDestination(destination);
    }

    @Override
    public String getDestination() {
        return StringUtils.isBlank(destination) ? DEFAULT_ROOT.getPath() : FilenameUtils.separatorsToUnix(destination);
    }

    @Override
    public ArtifactType getArtifactType() {
        return ArtifactType.build;
    }

    @Override
    public String getArtifactTypeValue() {
        return ARTIFACT_PLAN_DISPLAY_NAME;
    }
}
