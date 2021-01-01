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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.util.Map;

public interface JsonMessageHandler {
    SCMPropertyConfiguration responseMessageForSCMConfiguration(String responseBody);

    SCMView responseMessageForSCMView(String responseBody);

    String requestMessageForIsSCMConfigurationValid(SCMPropertyConfiguration scmConfiguration);

    ValidationResult responseMessageForIsSCMConfigurationValid(String responseBody);

    String requestMessageForCheckConnectionToSCM(SCMPropertyConfiguration scmConfiguration);

    Result responseMessageForCheckConnectionToSCM(String responseBody);

    String requestMessageForLatestRevision(SCMPropertyConfiguration scmConfiguration, Map<String, String> materialData, String flyweightFolder);

    MaterialPollResult responseMessageForLatestRevision(String responseBody);

    String requestMessageForLatestRevisionsSince(SCMPropertyConfiguration scmConfiguration, Map<String, String> materialData, String flyweightFolder, SCMRevision previousRevision);

    MaterialPollResult responseMessageForLatestRevisionsSince(String responseBody);

    String requestMessageForCheckout(SCMPropertyConfiguration scmConfiguration, String destinationFolder, SCMRevision revision);

    Result responseMessageForCheckout(String responseBody);
}
