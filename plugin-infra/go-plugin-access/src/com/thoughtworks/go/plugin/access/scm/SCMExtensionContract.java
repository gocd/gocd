/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.util.Map;

public interface SCMExtensionContract extends GoPluginExtension {

    SCMPropertyConfiguration getSCMConfiguration(String pluginId);

    SCMView getSCMView(String pluginId);

    ValidationResult isSCMConfigurationValid(String pluginId, final SCMPropertyConfiguration scmConfiguration);

    Result checkConnectionToSCM(String pluginId, final SCMPropertyConfiguration scmConfiguration);

    MaterialPollResult getLatestRevision(String pluginId, final SCMPropertyConfiguration scmConfiguration, Map<String, String> materialData, final String flyweightFolder);

    MaterialPollResult latestModificationSince(String pluginId, final SCMPropertyConfiguration scmConfiguration, Map<String, String> materialData, final String flyweightFolder, final SCMRevision previouslyKnownRevision);

    Result checkout(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String destinationFolder, final SCMRevision revision);
}
