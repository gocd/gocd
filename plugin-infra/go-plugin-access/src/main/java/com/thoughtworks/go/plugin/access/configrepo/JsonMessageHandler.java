/*
 * Copyright 2017 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;

import java.util.Collection;

public interface JsonMessageHandler {
    String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations);

    CRParseResult responseMessageForParseDirectory(String responseBody);

    String requestMessageForPipelineExport(CRPipeline pipeline);

    String responseMessageForPipelineExport(String responseBody);

    Capabilities getCapabilitiesFromResponse(String responseBody);
}