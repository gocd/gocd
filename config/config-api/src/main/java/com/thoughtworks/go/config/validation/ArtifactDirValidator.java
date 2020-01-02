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
package com.thoughtworks.go.config.validation;

import java.io.File;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ArtifactDirValidator implements GoConfigValidator {
    @Override
    public void validate(CruiseConfig cruiseConfig) throws Exception {
        ServerConfig serverConfig = cruiseConfig.server();
        String artifactDir = serverConfig.artifactsDir();

        if (isEmpty(artifactDir)) {
            throw new Exception("Please provide a not empty value for artifactsdir");
        }

        if (StringUtils.equals(".", artifactDir) || new File("").getAbsolutePath().equals(
                new File(artifactDir).getAbsolutePath())) {
            throw new Exception("artifactsdir should not point to the root of sand box [" +
                    new File(artifactDir).getAbsolutePath()
                    + "]");
        }
    }
}
