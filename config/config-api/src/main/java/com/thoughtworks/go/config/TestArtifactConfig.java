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

@AttributeAwareConfigTag(value = "artifact", attribute = "type", attributeValue = "test")
public class TestArtifactConfig extends BuiltinArtifactConfig {
    public static final String TEST_PLAN_DISPLAY_NAME = "Test Artifact";
    public static final String TEST_OUTPUT_FOLDER = "testoutput";

    public TestArtifactConfig() {
    }

    public TestArtifactConfig(String src, String dest) {
        setSource(src);
        setDestination(dest);
    }

    @Override
    public ArtifactType getArtifactType() {
        return ArtifactType.test;
    }

    @Override
    public String getArtifactTypeValue() {
        return TEST_PLAN_DISPLAY_NAME;
    }

    @Override
    public String getDestination() {
        return StringUtils.isBlank(destination) ? TEST_OUTPUT_FOLDER : FilenameUtils.separatorsToUnix(destination);
    }
}
