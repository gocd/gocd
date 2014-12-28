/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.util.List;

public interface SCMExtensionContract {
    SCMPropertyConfiguration getSCMConfiguration(String pluginId);

    ValidationResult isSCMConfigurationValid(String pluginId, final SCMPropertyConfiguration scmConfiguration);

    Result checkConnectionToSCM(String pluginId, final SCMPropertyConfiguration scmConfiguration);

    SCMRevision getLatestRevision(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String flyweightFolder);

    List<SCMRevision> latestModificationSince(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String flyweightFolder, final SCMRevision previouslyKnownRevision);

    Result checkout(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String destinationFolder, final SCMRevision revision);
}
